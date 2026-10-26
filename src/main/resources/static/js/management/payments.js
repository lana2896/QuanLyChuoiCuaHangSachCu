// Trang quản lý thanh toán: các khoản sàn phải hoàn cho khách, chi cho cửa hàng, và sổ giao dịch ngân hàng.
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('paymentsPage')) { return; }

  var Auth = OB.auth;
  var Banks = window.OldBookBanks;
  // Hoàn tiền và chi tiền là việc vận hành của Quản lý; quản trị viên lo hệ thống nên không vào trang này.
  var ROLES = ['QUAN_LY'];
  var BASE = '/api/management/payments';
  var SIZE = 20;
  var TRANSACTIONS = 'GIAO_DICH';

  var TRANSFER_STATUS = {
    CHO_THONG_TIN: { label: 'Chờ khách nhập tài khoản', css: 'is-bad' },
    CHO_XU_LY: { label: 'Chờ chuyển khoản', css: 'is-wait' },
    DA_CHUYEN: { label: 'Đã chuyển', css: 'is-ok' }
  };
  var MATCH_STATUS = {
    KHOP: { label: 'Khớp, đã tạo đơn', css: 'is-ok' },
    KHONG_KHOP: { label: 'Không khớp', css: 'is-wait' },
    CAN_HOAN_TIEN: { label: 'Phải hoàn tiền', css: 'is-bad' },
    DANG_XU_LY: { label: 'Đang xử lý', css: 'is-wait' }
  };

  var tab = 'HOAN_TIEN_KHACH';
  var page = 0;
  var rows = [];
  var selected = null;
  var busy = false;
  var requestId = 0;

  function $(id) { return document.getElementById(id); }
  function show(id) { $(id).classList.remove('d-none'); }
  function hide(id) { $(id).classList.add('d-none'); }
  function esc(value) { return OB.escapeHtml(value === null || value === undefined ? '' : value); }
  function money(value) { return OB.formatMoney(value); }
  function modal() { return window.bootstrap.Modal.getOrCreateInstance($('transferModal')); }

  function dateTime(value) {
    if (!value) { return '—'; }
    var m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(String(value));
    return m ? m[3] + '/' + m[2] + '/' + m[1] + ' ' + m[4] + ':' + m[5] : String(value);
  }

  function pill(map, value) {
    var status = map[value] || { label: value, css: 'is-wait' };
    return '<span class="pay-status ' + status.css + '">' + esc(status.label) + '</span>';
  }

  function hasAccess() {
    var user = Auth.getUser();
    return !!user && ROLES.indexOf(user.vaiTro) >= 0;
  }

  function isRefund() { return tab === 'HOAN_TIEN_KHACH'; }

  function account(row) {
    if (!row.soTaiKhoan) {
      return '<span class="text-muted">' + (isRefund() ? 'Khách chưa nhập' : 'Cửa hàng chưa khai') + '</span>';
    }
    return esc(Banks.name(row.maNganHang)) + '<br><span class="small text-muted">' + esc(row.soTaiKhoan) + '</span>';
  }

  // Chỉ chuyển được khi đã biết tài khoản nhận; khoản hoàn còn phải được khách xác nhận tài khoản.
  function canTransfer(row) {
    return !!row.soTaiKhoan && row.trangThai === 'CHO_XU_LY';
  }

  function transferRow(row) {
    var recipient = isRefund()
      ? esc(row.tenNguoiNhan || row.emailNguoiNhan)
      : esc(row.tenCuaHang || row.tenNguoiNhan);
    var source = row.maDHCH ? 'Đơn #' + row.maDHCH : 'Chưa tạo được đơn';
    var action = row.trangThai === 'DA_CHUYEN'
      ? '<span class="small text-muted">' + esc(dateTime(row.ngayXuLy)) + '</span>'
      : '<button type="button" class="btn btn-sm ' + (canTransfer(row) ? 'btn-accent' : 'btn-outline-brown')
        + '" data-transfer="' + row.maYeuCau + '">' + (canTransfer(row) ? 'Chuyển khoản' : 'Xem') + '</button>';

    return '<tr>'
      + '<td><strong>' + esc(row.noiDungChuyenKhoan) + '</strong><br><span class="small text-muted">' + esc(dateTime(row.ngayTao)) + '</span></td>'
      + '<td>' + esc(source) + (row.lyDo ? '<br><span class="small text-muted">' + esc(row.lyDo) + '</span>' : '') + '</td>'
      + '<td>' + recipient + '<br><span class="small text-muted">' + esc(row.soDienThoaiNguoiNhan || row.emailNguoiNhan) + '</span></td>'
      + '<td>' + account(row) + '</td>'
      + '<td class="text-end price">' + esc(money(row.soTien)) + '</td>'
      + '<td>' + pill(TRANSFER_STATUS, row.trangThai) + '</td>'
      + '<td class="text-end">' + action + '</td>'
      + '</tr>';
  }

  function transactionRow(row) {
    return '<tr>'
      + '<td>' + esc(dateTime(row.thoiGianGiaoDich || row.ngayNhan)) + '<br><span class="small text-muted">' + esc(row.maGiaoDichCong) + '</span></td>'
      + '<td class="pay-ledger-text">' + esc(row.noiDung) + '</td>'
      + '<td>' + esc(row.tenNguoiChuyen || '—') + '<br><span class="small text-muted">' + esc(row.soTaiKhoanChuyen) + '</span></td>'
      + '<td class="text-end price">' + esc(money(row.soTien)) + '</td>'
      + '<td>' + pill(MATCH_STATUS, row.ketQua) + (row.maThanhToan ? '<br><span class="small text-muted">' + esc(row.maThanhToan) + '</span>' : '') + '</td>'
      + '<td class="small text-muted">' + esc(row.ghiChu) + '</td>'
      + '</tr>';
  }

  function renderHead() {
    $('paymentsHead').innerHTML = tab === TRANSACTIONS
      ? '<tr><th scope="col">Thời gian</th><th scope="col">Nội dung</th><th scope="col">Người chuyển</th>'
        + '<th scope="col" class="text-end">Số tiền</th><th scope="col">Đối soát</th><th scope="col">Ghi chú</th></tr>'
      : '<tr><th scope="col">Mã</th><th scope="col">Nguồn</th><th scope="col">' + (isRefund() ? 'Khách' : 'Cửa hàng')
        + '</th><th scope="col">Tài khoản nhận</th><th scope="col" class="text-end">Số tiền</th>'
        + '<th scope="col">Trạng thái</th><th scope="col"><span class="visually-hidden">Thao tác</span></th></tr>';
  }

  function render(data) {
    rows = data.content || [];
    hide('paymentsLoading');
    renderHead();
    $('paymentsRows').innerHTML = rows.map(tab === TRANSACTIONS ? transactionRow : transferRow).join('');
    if (rows.length) { hide('paymentsEmpty'); } else { show('paymentsEmpty'); }

    var totalPages = data.totalPages || 0;
    $('paymentsSummary').textContent = (data.totalElements || 0) + ' dòng'
      + (totalPages > 1 ? ' · trang ' + (page + 1) + '/' + totalPages : '');
    $('paymentsPrev').disabled = page <= 0;
    $('paymentsNext').disabled = page + 1 >= totalPages;
  }

  function load() {
    var id = ++requestId;
    var url = BASE + (tab === TRANSACTIONS
      ? '/transactions?page=' + page + '&size=' + SIZE
      : '/transfers?loai=' + tab + '&page=' + page + '&size=' + SIZE
        + ($('paymentsStatus').value ? '&trangThai=' + $('paymentsStatus').value : ''));

    hide('paymentsError');
    hide('paymentsEmpty');
    show('paymentsLoading');
    OB.request('GET', url).then(function (data) {
      // Bỏ kết quả của lần tải cũ nếu người dùng đã đổi thẻ hoặc trang trong lúc chờ.
      if (id === requestId) { render(data || {}); }
    }, function (err) {
      if (id !== requestId) { return; }
      hide('paymentsLoading');
      $('paymentsRows').innerHTML = '';
      $('paymentsError').textContent = err.message;
      show('paymentsError');
    });
  }

  function selectTab(next) {
    tab = next;
    page = 0;
    document.querySelectorAll('[data-tab]').forEach(function (button) {
      var active = button.getAttribute('data-tab') === tab;
      button.classList.toggle('active', active);
      button.setAttribute('aria-selected', active ? 'true' : 'false');
    });
    if (tab === TRANSACTIONS) { hide('paymentsFilter'); } else { show('paymentsFilter'); }
    $('paymentsRows').innerHTML = '';
    load();
  }

  function openTransfer(id) {
    selected = rows.find(function (r) { return String(r.maYeuCau) === String(id); }) || null;
    if (!selected) { return; }

    $('transferTitle').textContent = (isRefund() ? 'Hoàn tiền ' : 'Chi tiền ') + selected.noiDungChuyenKhoan;
    $('transferRecipient').textContent = isRefund()
      ? (selected.tenNguoiNhan || selected.emailNguoiNhan || '')
      : (selected.tenCuaHang || '');
    $('transferBank').textContent = selected.soTaiKhoan ? Banks.name(selected.maNganHang) : '—';
    $('transferAccount').textContent = selected.soTaiKhoan || '—';
    $('transferAccountName').textContent = selected.tenChuTaiKhoan || '—';
    $('transferAmount').textContent = money(selected.soTien);
    $('transferCode').textContent = selected.noiDungChuyenKhoan;

    if (selected.qrUrl) {
      $('transferQr').setAttribute('src', selected.qrUrl);
      show('transferQrBox');
    } else {
      hide('transferQrBox');
    }

    if (canTransfer(selected)) {
      hide('transferNoAccount');
      show('transferDone');
    } else {
      $('transferNoAccount').textContent = !selected.soTaiKhoan
        ? (isRefund() ? 'Khách chưa nhập tài khoản nhận hoàn tiền.' : 'Cửa hàng chưa khai tài khoản nhận tiền.')
        : 'Tài khoản này được điền sẵn từ lần chuyển tiền; chờ khách xác nhận rồi mới chuyển.';
      show('transferNoAccount');
      hide('transferDone');
    }
    modal().show();
  }

  function markDone() {
    if (busy || !selected) { return; }
    var item = selected;
    OB.confirm('Xác nhận đã chuyển ' + money(item.soTien) + ' với nội dung ' + item.noiDungChuyenKhoan + '? Thao tác này không hoàn tác được.', {
      confirmText: 'Đã chuyển'
    }).then(function (ok) {
      if (!ok) { return; }
      busy = true;
      $('transferDone').disabled = true;
      OB.request('POST', BASE + '/transfers/' + encodeURIComponent(item.maYeuCau) + '/done').then(function () {
        modal().hide();
        OB.toast('Đã ghi nhận khoản ' + item.noiDungChuyenKhoan, 'success');
        load();
      }, function (err) {
        OB.toast(err.message, 'error');
      }).then(function () {
        busy = false;
        $('transferDone').disabled = false;
      });
    });
  }

  function init() {
    // Chưa đăng nhập: auth.js đang chuyển về trang đăng nhập (body có data-page-requires-login).
    if (!Auth.isLoggedIn()) { return; }
    if (!hasAccess()) {
      show('paymentsDenied');
      return;
    }
    show('paymentsContent');

    document.querySelectorAll('[data-tab]').forEach(function (button) {
      button.addEventListener('click', function () { selectTab(button.getAttribute('data-tab')); });
    });
    $('paymentsStatus').addEventListener('change', function () { page = 0; load(); });
    $('paymentsPrev').addEventListener('click', function () { if (page > 0) { page -= 1; load(); } });
    $('paymentsNext').addEventListener('click', function () { page += 1; load(); });
    $('paymentsRows').addEventListener('click', function (event) {
      var button = event.target.closest('[data-transfer]');
      if (button) { openTransfer(button.getAttribute('data-transfer')); }
    });
    $('transferDone').addEventListener('click', markDone);

    load();
  }

  init();
})();
