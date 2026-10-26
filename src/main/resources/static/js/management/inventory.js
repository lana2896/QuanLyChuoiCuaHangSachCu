// Trang Kho toàn chuỗi: tồn kho theo tựa sách ở các chi nhánh và các phiếu điều chuyển.
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('ivContent')) { return; }

  var Auth = OB.auth;
  var stockReq = 0;
  var transferReq = 0;

  function $(id) { return document.getElementById(id); }
  function esc(v) { return OB.escapeHtml(v === null || v === undefined ? '' : v); }

  function dateTime(value) {
    var m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(String(value || ''));
    return m ? m[3] + '/' + m[2] + '/' + m[1] + ' ' + m[4] + ':' + m[5] : '—';
  }

  function showError(id, err, fallback) {
    if (Auth.handleAuthError && Auth.handleAuthError(err)) { return; }
    $(id).textContent = (err && err.message) || fallback;
    $(id).classList.remove('d-none');
  }

  function loadStock() {
    var id = ++stockReq;
    $('ivStockError').classList.add('d-none');
    var q = $('ivKeyword').value.trim();
    OB.request('GET', '/api/management/inventory' + (q ? '?tuKhoa=' + encodeURIComponent(q) : '')).then(function (rows) {
      if (id !== stockReq) { return; }
      rows = rows || [];
      $('ivStockRows').innerHTML = rows.map(function (t) {
        var parts = t.chiNhanh.map(function (c) {
          return '<li><strong>' + esc(c.tenCuaHang) + '</strong>: ' + esc(c.soLuongTon) + ' cuốn' +
            (c.doMoiPercent !== null ? ' <span class="text-muted">(' + Math.round(Number(c.doMoiPercent)) + '% mới, ' +
              esc(OB.formatMoney(c.giaBanCu)) + ')</span>' : '') +
            (c.trangThaiBan === 'NGUNG_BAN' ? ' <span class="badge text-bg-secondary">ngừng bán</span>' : '') + '</li>';
        }).join('');
        return '<tr><td><strong>' + esc(t.tenSach) + '</strong><br><span class="small text-muted">' + esc(t.tacGia || 'Chưa rõ tác giả') +
          '</span></td><td class="text-end"><strong>' + esc(t.tongTon) + '</strong><br><span class="small text-muted">' +
          esc(t.soChiNhanh) + ' chi nhánh</span></td><td><ul class="list-unstyled small mb-0">' + parts + '</ul></td></tr>';
      }).join('');
      $('ivStockEmpty').classList.toggle('d-none', rows.length > 0);
      if (OB.managerShell) { OB.managerShell.touch(); }
    }, function (err) {
      if (id === stockReq) { showError('ivStockError', err, 'Không tải được tồn kho.'); }
    });
  }

  function loadTransfers() {
    var id = ++transferReq;
    $('ivTransferError').classList.add('d-none');
    var status = $('ivStatus').value;
    OB.request('GET', '/api/management/transfers' + (status ? '?trangThai=' + encodeURIComponent(status) : '')).then(function (rows) {
      if (id !== transferReq) { return; }
      rows = rows || [];
      $('ivTransferRows').innerHTML = rows.map(function (t) {
        return '<tr><td>#' + esc(t.maPhieu) + '</td><td>' + esc(t.tenSach) + '</td><td>' + esc(t.tenCuaHangNguon) +
          '</td><td>' + esc(t.tenCuaHangDich) + '</td><td class="text-end">' + esc(t.soLuong) + '</td><td>' + esc(t.tenTrangThai) +
          (t.lyDoTuChoi ? '<br><span class="small text-muted">' + esc(t.lyDoTuChoi) + '</span>' : '') +
          '</td><td class="text-nowrap">' + esc(dateTime(t.ngayTao)) + '</td></tr>';
      }).join('');
      $('ivTransferEmpty').classList.toggle('d-none', rows.length > 0);
    }, function (err) {
      if (id === transferReq) { showError('ivTransferError', err, 'Không tải được phiếu điều chuyển.'); }
    });
  }

  function init() {
    if (!Auth.isLoggedIn()) { return; }
    var user = Auth.getUser();
    if (!user || (user.vaiTro !== 'QUAN_LY' && user.vaiTro !== 'QUAN_TRI_VIEN')) {
      $('ivDenied').classList.remove('d-none');
      return;
    }
    $('ivContent').classList.remove('d-none');
    $('ivSearchForm').addEventListener('submit', function (e) { e.preventDefault(); loadStock(); });
    $('ivStatus').addEventListener('change', loadTransfers);
    loadStock();
    loadTransfers();
  }

  init();
})();
