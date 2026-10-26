// Tài khoản nhận tiền của cửa hàng trên trang /store/books. Tách khỏi store.js vì việc này không qua kiểm duyệt:
// đổi tài khoản không đưa cửa hàng về trạng thái chờ duyệt.
(function () {
  'use strict';

  var OB = window.OldBook;
  var row = document.getElementById('storeBankRow');
  if (!OB || !row) { return; }

  var Auth = OB.auth;
  var Banks = window.OldBookBanks;
  var ACCOUNT_PATTERN = /^[0-9A-Za-z]{4,30}$/;
  var store = null;
  var saving = false;

  function $(id) { return document.getElementById(id); }
  function modal() { return window.bootstrap.Modal.getOrCreateInstance($('storeBankModal')); }

  function error(message) {
    $('storeBankError').textContent = message || '';
    $('storeBankError').classList.toggle('d-none', !message);
  }

  function render() {
    if (!store) {
      row.classList.add('d-none');
      return;
    }
    var has = !!store.soTaiKhoan;
    $('storeBankLine').textContent = has
      ? 'Nhận tiền: ' + Banks.name(store.maNganHang) + ' · ' + store.soTaiKhoan + ' · ' + (store.tenChuTaiKhoan || '')
      : 'Chưa khai tài khoản nhận tiền. Sàn chưa chi được tiền các đơn khách chuyển khoản trước.';
    $('editStoreBank').textContent = has ? 'Đổi tài khoản nhận tiền' : 'Khai tài khoản nhận tiền';
    row.classList.remove('d-none');
  }

  function open() {
    if (!store) { return; }
    error('');
    Banks.fill($('storeBank'), store.maNganHang);
    $('storeBankAccount').value = store.soTaiKhoan || '';
    $('storeBankOwner').value = store.tenChuTaiKhoan || '';
    modal().show();
  }

  function save(event) {
    event.preventDefault();
    if (saving) { return; }

    var body = {
      maNganHang: $('storeBank').value,
      soTaiKhoan: $('storeBankAccount').value.trim(),
      tenChuTaiKhoan: $('storeBankOwner').value.trim()
    };
    if (!body.maNganHang || !body.tenChuTaiKhoan) {
      error('Vui lòng nhập đủ ngân hàng, số tài khoản và tên chủ tài khoản.');
      return;
    }
    if (!ACCOUNT_PATTERN.test(body.soTaiKhoan)) {
      error('Số tài khoản chỉ gồm chữ và số, từ 4 đến 30 ký tự.');
      return;
    }

    saving = true;
    $('storeBankSubmit').disabled = true;
    OB.request('PUT', '/api/store/me/bank-account', body).then(function (data) {
      store = data;
      render();
      modal().hide();
      OB.toast('Đã lưu tài khoản nhận tiền', 'success');
    }, function (err) {
      error(err.message);
    }).then(function () {
      saving = false;
      $('storeBankSubmit').disabled = false;
    });
  }

  if (!Auth.isLoggedIn()) { return; }

  $('editStoreBank').addEventListener('click', open);
  $('storeBankForm').addEventListener('submit', save);

  // Chưa có cửa hàng hoặc không phải chủ cửa hàng thì dòng này cứ ẩn, phần còn lại của trang do store.js lo.
  OB.request('GET', '/api/store/me').then(function (data) {
    store = data;
    render();
  }, function () { });
})();
