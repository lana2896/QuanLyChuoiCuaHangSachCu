// Trang "Hoàn tiền của tôi": khách xem các khoản sàn phải hoàn và nhập tài khoản nhận.
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('refundsPage')) { return; }

  var Auth = OB.auth;
  var Banks = window.OldBookBanks;
  var API = '/api/payment/refunds';
  var STATUS = {
    CHO_THONG_TIN: { label: 'Chờ bạn nhập tài khoản nhận', css: 'is-bad' },
    CHO_XU_LY: { label: 'Sàn đang chuyển khoản', css: 'is-wait' },
    DA_CHUYEN: { label: 'Đã hoàn', css: 'is-ok' }
  };

  var refunds = [];
  var editing = null;
  var saving = false;

  function $(id) { return document.getElementById(id); }
  function show(id) { $(id).classList.remove('d-none'); }
  function hide(id) { $(id).classList.add('d-none'); }
  function esc(value) { return OB.escapeHtml(value); }
  function modal() { return window.bootstrap.Modal.getOrCreateInstance($('refundAccountModal')); }

  function card(refund) {
    var status = STATUS[refund.trangThai] || { label: refund.trangThai, css: 'is-wait' };
    var source = refund.maDHCH ? 'Đơn #' + refund.maDHCH : 'Lần thanh toán chưa tạo được đơn';
    var account = refund.soTaiKhoan
      ? esc(Banks.name(refund.maNganHang)) + ' · ' + esc(refund.soTaiKhoan) + ' · ' + esc(refund.tenChuTaiKhoan || '')
      : 'Chưa có';
    var action = '';
    if (refund.trangThai === 'CHO_THONG_TIN') {
      // Tài khoản điền sẵn (nếu có) lấy từ lần chuyển tiền, nên vẫn cần khách xác nhận.
      action = '<button type="button" class="btn btn-accent btn-sm" data-refund="' + refund.maYeuCau + '">'
        + (refund.soTaiKhoan ? 'Xác nhận tài khoản nhận' : 'Nhập tài khoản nhận') + '</button>';
    } else if (refund.trangThai === 'CHO_XU_LY') {
      action = '<button type="button" class="btn btn-outline-brown btn-sm" data-refund="' + refund.maYeuCau + '">Sửa tài khoản</button>';
    }

    return '<article class="panel refund-card mb-3">'
      + '<div class="d-flex flex-wrap justify-content-between align-items-start gap-2">'
      + '<div><h3 class="h6 fw-bold mb-1">' + esc(source) + '</h3>'
      + '<p class="text-muted small mb-0">' + esc(refund.lyDo || '') + '</p></div>'
      + '<div class="text-end"><div class="price fs-5">' + esc(OB.formatMoney(refund.soTien)) + '</div>'
      + '<span class="pay-status ' + status.css + '">' + esc(status.label) + '</span></div></div>'
      + '<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3">'
      + '<span class="small"><span class="text-muted">Tài khoản nhận:</span> ' + account + '</span>' + action
      + '</div></article>';
  }

  function render() {
    hide('refundsLoading');
    hide('refundsError');
    if (!refunds.length) {
      show('refundsEmpty');
      $('refundsList').innerHTML = '';
      return;
    }
    hide('refundsEmpty');
    $('refundsList').innerHTML = refunds.map(card).join('');
  }

  function load() {
    OB.request('GET', API).then(function (data) {
      refunds = data || [];
      render();
    }, function (err) {
      hide('refundsLoading');
      $('refundsError').textContent = err.message;
      show('refundsError');
    });
  }

  function openForm(id) {
    editing = refunds.find(function (r) { return String(r.maYeuCau) === String(id); }) || null;
    if (!editing) { return; }
    hide('refundAccountError');
    $('refundAccountAmount').textContent = OB.formatMoney(editing.soTien);
    Banks.fill($('refundBank'), editing.maNganHang);
    $('refundAccountNo').value = editing.soTaiKhoan || '';
    $('refundAccountName').value = editing.tenChuTaiKhoan || '';
    modal().show();
  }

  function save(event) {
    event.preventDefault();
    if (saving || !editing) { return; }

    var body = {
      maNganHang: $('refundBank').value,
      soTaiKhoan: $('refundAccountNo').value.trim(),
      tenChuTaiKhoan: $('refundAccountName').value.trim()
    };
    if (!body.maNganHang || !body.soTaiKhoan || !body.tenChuTaiKhoan) {
      $('refundAccountError').textContent = 'Vui lòng nhập đủ ngân hàng, số tài khoản và tên chủ tài khoản.';
      show('refundAccountError');
      return;
    }

    saving = true;
    $('refundAccountSubmit').disabled = true;
    OB.request('PUT', API + '/' + encodeURIComponent(editing.maYeuCau) + '/account', body).then(function () {
      modal().hide();
      OB.toast('Đã lưu tài khoản nhận hoàn tiền', 'success');
      load();
    }, function (err) {
      $('refundAccountError').textContent = err.message;
      show('refundAccountError');
    }).then(function () {
      saving = false;
      $('refundAccountSubmit').disabled = false;
    });
  }

  function init() {
    // Chưa đăng nhập: auth.js đang chuyển về trang đăng nhập (body có data-page-requires-login).
    if (!Auth.isLoggedIn()) { return; }
    $('refundsList').addEventListener('click', function (event) {
      var button = event.target.closest('[data-refund]');
      if (button) { openForm(button.getAttribute('data-refund')); }
    });
    $('refundAccountForm').addEventListener('submit', save);
    load();
    if (OB.notifications) {
      OB.notifications.refreshOn(['CAN_TAI_KHOAN_HOAN_TIEN', 'DA_HOAN_TIEN'], load, function () { return !saving; });
    }
  }

  init();
})();
