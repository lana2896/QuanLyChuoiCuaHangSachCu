/*
 * Khối nhập tài khoản nhận tiền dùng chung: khách nhận hoàn tiền, chủ cửa hàng nhận doanh thu, hộp thoại hủy đơn.
 * Gồm ngân hàng, số tài khoản và tên chủ tài khoản; cả ba đều bắt buộc.
 *
 *   var form = OldBook.bankForm.create(container, { idPrefix: 'refund' });
 *   form.setValues({ maNganHang, soTaiKhoan, tenChuTaiKhoan });
 *   var values = form.getValues();     // { maNganHang, soTaiKhoan, tenChuTaiKhoan } đã chuẩn hóa
 *   var error = form.validate();       // chuỗi lỗi, hoặc '' khi hợp lệ
 */
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || OB.bankForm) { return; }

  var Banks = window.OldBookBanks;
  var ACCOUNT_PATTERN = /^[0-9A-Za-z]{4,30}$/;

  // Tên chủ tài khoản: chữ in hoa, không dấu (đúng như ngân hàng ghi).
  function normalizeName(value) {
    return String(value || '')
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D')
      .toUpperCase().replace(/\s+/g, ' ').trim();
  }

  function blank(value) { return value === null || value === undefined || String(value).trim() === ''; }

  // Hàm thuần để kiểm tra (và để test): trả về thông báo lỗi hoặc ''.
  function validate(values) {
    if (blank(values.maNganHang)) { return 'Vui lòng chọn ngân hàng.'; }
    if (blank(values.soTaiKhoan)) { return 'Vui lòng nhập số tài khoản.'; }
    if (!ACCOUNT_PATTERN.test(String(values.soTaiKhoan).trim())) {
      return 'Số tài khoản chỉ gồm chữ và số, từ 4 đến 30 ký tự.';
    }
    if (blank(values.tenChuTaiKhoan)) { return 'Vui lòng nhập tên chủ tài khoản.'; }
    return '';
  }

  function create(root, options) {
    options = options || {};
    var p = options.idPrefix || 'bank';

    root.innerHTML =
      '<div class="mb-3"><label for="' + p + 'Bank" class="form-label">Ngân hàng</label><select id="' + p + 'Bank" class="form-select"></select></div>'
      + '<div class="mb-3"><label for="' + p + 'Account" class="form-label">Số tài khoản</label>'
      + '<input id="' + p + 'Account" class="form-control" maxlength="30" inputmode="numeric" autocomplete="off"></div>'
      + '<div class="mb-2"><label for="' + p + 'Owner" class="form-label">Tên chủ tài khoản</label>'
      + '<input id="' + p + 'Owner" class="form-control" maxlength="255" autocomplete="off" style="text-transform:uppercase"'
      + ' placeholder="Viết in hoa, không dấu, ví dụ NGUYEN VAN A"></div>';

    function $(suffix) { return root.querySelector('#' + p + suffix); }

    $('Owner').addEventListener('blur', function () { $('Owner').value = normalizeName($('Owner').value); });

    var api = {
      getValues: function () {
        var account = $('Account').value.trim();
        var owner = normalizeName($('Owner').value);
        return {
          maNganHang: $('Bank').value || null,
          soTaiKhoan: account || null,
          tenChuTaiKhoan: owner || null
        };
      },
      setValues: function (values) {
        values = values || {};
        Banks.fill($('Bank'), values.maNganHang || '');
        $('Account').value = values.soTaiKhoan || '';
        $('Owner').value = values.tenChuTaiKhoan || '';
      },
      validate: function () { return validate(api.getValues()); },
      isEmpty: function () {
        var v = api.getValues();
        return blank(v.maNganHang) && blank(v.soTaiKhoan) && blank(v.tenChuTaiKhoan);
      },
      // Giữ cho các trang đang gọi: không còn tải ảnh nên không bao giờ "đang tải".
      isUploading: function () { return false; },
      focus: function () { $('Bank').focus(); }
    };
    api.setValues({});
    return api;
  }

  OB.bankForm = { create: create, validate: validate, normalizeName: normalizeName };
})();
