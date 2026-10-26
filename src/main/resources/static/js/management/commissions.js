// Trang Hoa hồng: tỷ lệ theo danh mục, sửa tỷ lệ (có ghi chú), xem lịch sử.
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('cmContent')) { return; }

  var Auth = OB.auth;
  var BASE = '/api/management/commissions';
  var rows = [];
  var editing = null;
  var busy = false;
  var requestId = 0;

  function $(id) { return document.getElementById(id); }
  function esc(value) { return OB.escapeHtml(value === null || value === undefined ? '' : value); }
  function modal(id) { return window.bootstrap.Modal.getOrCreateInstance($(id)); }

  function dateTime(value) {
    var m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(String(value || ''));
    return m ? m[3] + '/' + m[2] + '/' + m[1] + ' ' + m[4] + ':' + m[5] : '—';
  }

  function render() {
    $('cmRows').innerHTML = rows.map(function (r) {
      return '<tr>'
        + '<td><strong>' + esc(r.tenDanhMuc) + '</strong><br><span class="small text-muted">DM' + r.maDM + '</span></td>'
        + '<td class="text-end"><strong>' + esc(r.tyLe) + '%</strong>'
        + (r.macDinh ? '<br><span class="small text-muted">mặc định</span>' : '') + '</td>'
        + '<td class="text-end">' + r.soCuaHang + '</td>'
        + '<td>' + esc(r.apDungTu ? dateTime(r.apDungTu) : '—') + '</td>'
        + '<td>' + esc(r.nguoiSua || '—') + (r.ghiChu ? '<br><span class="small text-muted">' + esc(r.ghiChu) + '</span>' : '') + '</td>'
        + '<td class="text-end text-nowrap">'
        + '<button type="button" class="btn btn-sm btn-outline-brown me-1" data-history="' + r.maDM + '">Lịch sử</button>'
        + '<button type="button" class="btn btn-sm btn-accent" data-edit="' + r.maDM + '">Sửa</button></td>'
        + '</tr>';
    }).join('');
    $('cmEmpty').classList.toggle('d-none', rows.length > 0);
    if (OB.managerShell) { OB.managerShell.touch(); }
  }

  function load() {
    var id = ++requestId;
    $('cmError').classList.add('d-none');
    OB.request('GET', BASE).then(function (data) {
      if (id !== requestId) { return; }
      rows = data || [];
      render();
    }, function (err) {
      if (id !== requestId || (Auth.handleAuthError && Auth.handleAuthError(err))) { return; }
      $('cmErrorText').textContent = err.message || 'Không tải được danh sách hoa hồng.';
      $('cmError').classList.remove('d-none');
    });
  }

  function find(maDM) {
    return rows.find(function (r) { return String(r.maDM) === String(maDM); }) || null;
  }

  function openEdit(maDM) {
    editing = find(maDM);
    if (!editing) { return; }
    $('cmEditTitle').textContent = 'Sửa hoa hồng · ' + editing.tenDanhMuc;
    $('cmRate').value = editing.tyLe;
    $('cmNote').value = '';
    $('cmEditError').classList.add('d-none');
    modal('cmEditModal').show();
  }

  function save(event) {
    event.preventDefault();
    if (busy || !editing) { return; }
    var rate = parseFloat($('cmRate').value);
    var err = $('cmEditError');
    if (isNaN(rate) || rate < 0 || rate > 100) {
      err.textContent = 'Nhập tỷ lệ từ 0 đến 100.';
      err.classList.remove('d-none');
      return;
    }
    var target = editing;
    OB.confirm('Đổi hoa hồng danh mục "' + target.tenDanhMuc + '" từ ' + target.tyLe + '% sang ' + rate + '%? '
      + 'Tỷ lệ mới áp cho các đơn giao thành công từ bây giờ.', { confirmText: 'Lưu' }).then(function (ok) {
      if (!ok) { return; }
      busy = true;
      $('cmSave').disabled = true;
      OB.request('PUT', BASE + '/' + encodeURIComponent(target.maDM), { tyLe: rate, ghiChu: $('cmNote').value })
        .then(function () {
          modal('cmEditModal').hide();
          OB.toast('Đã cập nhật hoa hồng "' + target.tenDanhMuc + '"', 'success');
          load();
        }, function (e) {
          err.textContent = e.message;
          err.classList.remove('d-none');
        }).then(function () { busy = false; $('cmSave').disabled = false; });
    });
  }

  function openHistory(maDM) {
    var row = find(maDM);
    if (!row) { return; }
    $('cmHistoryTitle').textContent = 'Lịch sử · ' + row.tenDanhMuc;
    $('cmHistoryRows').innerHTML = '<tr><td colspan="5" class="text-muted">Đang tải...</td></tr>';
    $('cmHistoryEmpty').classList.add('d-none');
    modal('cmHistoryModal').show();
    OB.request('GET', BASE + '/' + encodeURIComponent(maDM) + '/history').then(function (list) {
      list = list || [];
      $('cmHistoryRows').innerHTML = list.map(function (h) {
        return '<tr><td><strong>' + esc(h.tyLe) + '%</strong></td><td>' + esc(dateTime(h.ngayBatDau)) + '</td>'
          + '<td>' + esc(h.ngayKetThuc ? dateTime(h.ngayKetThuc) : 'Đang áp dụng') + '</td>'
          + '<td>' + esc(h.nguoiSua || '—') + '</td><td class="small">' + esc(h.ghiChu || '') + '</td></tr>';
      }).join('');
      $('cmHistoryEmpty').classList.toggle('d-none', list.length > 0);
    }, function (e) {
      $('cmHistoryRows').innerHTML = '<tr><td colspan="5" class="text-danger">' + esc(e.message) + '</td></tr>';
    });
  }

  function init() {
    if (!Auth.isLoggedIn()) { return; }
    var user = Auth.getUser();
    if (!user || user.vaiTro !== 'QUAN_LY') { $('cmDenied').classList.remove('d-none'); return; }
    $('cmContent').classList.remove('d-none');

    $('cmRetry').addEventListener('click', load);
    $('cmEditForm').addEventListener('submit', save);
    $('cmRows').addEventListener('click', function (event) {
      var edit = event.target.closest('[data-edit]');
      var history = event.target.closest('[data-history]');
      if (edit) { openEdit(edit.getAttribute('data-edit')); }
      if (history) { openHistory(history.getAttribute('data-history')); }
    });

    load();
    if (OB.managerRealtime) { OB.managerRealtime.on('commissions', load); }
  }

  init();
})();
