(function () {
  'use strict';

  var API = '/api/management/shipping-carriers';
  var ALLOWED = ['QUAN_LY', 'QUAN_TRI_VIEN'];
  var carriers = [];

  function esc(v) { return OldBook.escapeHtml(v == null ? '' : v); }
  function money(v) { return OldBook.formatMoney(v); }
  function val(id) { return document.getElementById(id).value.trim(); }
  function num(id) { var v = val(id); return v === '' ? null : Number(v); }
  function fullUrl(path) { return window.location.origin + path; }

  function alertBox(message) {
    var a = document.getElementById('carrierAlert');
    a.classList.remove('d-none');
    a.textContent = message;
    document.getElementById('carrierContent').classList.add('d-none');
  }

  function ensureAccess() {
    if (!OldBook.auth.isLoggedIn()) {
      OldBook.auth.redirectToLogin('/management/shipping-carriers');
      return false;
    }
    var user = OldBook.auth.getUser();
    if (ALLOWED.indexOf(user && user.vaiTro) < 0) {
      alertBox('Chỉ quản lý hoặc quản trị viên mới quản lý được đối tác vận chuyển.');
      return false;
    }
    return true;
  }

  function load() {
    return OldBook.request('GET', API).then(function (data) {
      carriers = data || [];
      render();
    }).catch(function (e) {
      if (!OldBook.auth.handleAuthError(e)) OldBook.toast(e.message, 'danger');
    });
  }

  function render() {
    var body = document.getElementById('carrierBody');
    if (!carriers.length) {
      body.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Chưa có đối tác.</td></tr>';
      return;
    }
    body.innerHTML = carriers.map(function (c) {
      var active = c.trangThai === 'HOAT_DONG';
      return '<tr>' +
        '<td><strong>' + esc(c.maCode) + '</strong></td>' +
        '<td>' + esc(c.tenDonVi) + '</td>' +
        '<td>' + money(c.phiVanChuyen) + '</td>' +
        '<td>' + esc(c.thoiGianGiaoDuKien) + '</td>' +
        '<td><span class="badge ' + (active ? 'text-bg-success' : 'text-bg-secondary') + '">' + (active ? 'Hoạt động' : 'Ngừng') + '</span></td>' +
        '<td class="link-cell small">' + (c.duongDanTrang
          ? '<a href="' + esc(c.duongDanTrang) + '" target="_blank" rel="noopener"><code>' + esc(c.duongDanTrang) + '</code></a>' +
            ' <button class="btn btn-link btn-sm p-0 ms-1" title="Sao chép link" onclick="Carriers.copy(\'' + esc(c.maCode) + '\')"><i class="bi bi-clipboard"></i></button>'
          : '-') + '</td>' +
        '<td class="text-end text-nowrap">' +
          '<button class="btn btn-outline-secondary btn-sm me-1" onclick="Carriers.edit(\'' + esc(c.maCode) + '\')"><i class="bi bi-pencil"></i></button>' +
          '<button class="btn btn-sm ' + (active ? 'btn-outline-danger' : 'btn-outline-success') + '" onclick="Carriers.toggle(\'' + esc(c.maCode) + '\')">' + (active ? 'Ngừng' : 'Bật lại') + '</button>' +
        '</td>' +
      '</tr>';
    }).join('');
  }

  function find(code) { return carriers.find(function (c) { return c.maCode === code; }); }

  function create() {
    var body = {
      maCode: val('newCode').toUpperCase(),
      tenDonVi: val('newName'),
      phiVanChuyen: num('newFee'),
      thoiGianGiaoDuKien: num('newDays'),
      soDienThoai: val('newPhone') || null,
      email: val('newEmail') || null
    };
    OldBook.request('POST', API, body).then(function (c) {
      ['newCode', 'newName', 'newFee', 'newDays', 'newPhone', 'newEmail'].forEach(function (id) { document.getElementById(id).value = ''; });
      OldBook.toast('Đã thêm ' + c.tenDonVi + '. Link: ' + fullUrl(c.duongDanTrang), 'success', { duration: 6000 });
      load();
    }).catch(function (e) { OldBook.toast(e.message, 'danger'); });
  }

  function edit(code) {
    var c = find(code);
    if (!c) return;
    document.getElementById('editCode').value = c.maCode;
    document.getElementById('editCodeLabel').textContent = '(' + c.maCode + ')';
    document.getElementById('editName').value = c.tenDonVi || '';
    document.getElementById('editFee').value = c.phiVanChuyen == null ? '' : c.phiVanChuyen;
    document.getElementById('editDays').value = c.thoiGianGiaoDuKien == null ? '' : c.thoiGianGiaoDuKien;
    document.getElementById('editPhone').value = c.soDienThoai || '';
    document.getElementById('editEmail').value = c.email || '';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('editModal')).show();
  }

  function save() {
    var code = val('editCode');
    var body = {
      tenDonVi: val('editName'),
      phiVanChuyen: num('editFee'),
      thoiGianGiaoDuKien: num('editDays'),
      soDienThoai: val('editPhone') || null,
      email: val('editEmail') || null
    };
    OldBook.request('PUT', API + '/' + encodeURIComponent(code), body).then(function () {
      bootstrap.Modal.getInstance(document.getElementById('editModal')).hide();
      OldBook.toast('Đã cập nhật đối tác.', 'success');
      load();
    }).catch(function (e) { OldBook.toast(e.message, 'danger'); });
  }

  function toggle(code) {
    var c = find(code);
    if (!c) return;
    var next = c.trangThai === 'HOAT_DONG' ? 'NGUNG_HOAT_DONG' : 'HOAT_DONG';
    if (next === 'NGUNG_HOAT_DONG' && !window.confirm('Ngừng ' + c.tenDonVi + '? Khách sẽ không chọn được hãng này và link của hãng sẽ bị khóa.')) return;
    OldBook.request('PATCH', API + '/' + encodeURIComponent(code) + '/status', { trangThai: next }).then(function () {
      load();
    }).catch(function (e) { OldBook.toast(e.message, 'danger'); });
  }

  function copy(code) {
    var c = find(code);
    if (!c || !c.duongDanTrang) return;
    var url = fullUrl(c.duongDanTrang);
    var done = function () { OldBook.toast('Đã sao chép: ' + url, 'success'); };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(url).then(done, function () { window.prompt('Sao chép link:', url); });
    } else {
      window.prompt('Sao chép link:', url);
    }
  }

  window.Carriers = { load: load, create: create, edit: edit, save: save, toggle: toggle, copy: copy };

  document.addEventListener('DOMContentLoaded', function () {
    if (!ensureAccess()) return;
    load();
  });
})();
