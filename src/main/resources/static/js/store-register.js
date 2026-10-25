/*
 * Trang /store/register: khách hàng tự đăng ký gian hàng.
 * Gửi form xong, máy chủ tạo tài khoản Chủ cửa hàng và một cửa hàng chờ duyệt;
 * trang chuyển luôn sang tài khoản đó rồi mở /store/books để người dùng theo dõi trạng thái duyệt.
 */
(function () {
  'use strict';

  var OB = window.OldBook;
  var form = document.getElementById('storeRegisterForm');
  if (!OB || !form) { return; }

  var Auth = OB.auth;
  // Chưa đăng nhập: auth.js đang chuyển về trang đăng nhập (body có data-page-requires-login).
  if (!Auth.isLoggedIn()) { return; }

  var PANELS = ['registerLoading', 'registerFormPanel', 'alreadySellerPanel', 'wrongRolePanel', 'registerLoadError'];
  var ACCOUNT_ACTIVE = 'HOAT_DONG';
  var PHONE_PATTERN = /^[0-9+ ().-]{8,20}$/;

  var fields = {
    name: document.getElementById('registerStoreName'),
    phone: document.getElementById('registerStorePhone'),
    address: document.getElementById('registerStoreAddress'),
    description: document.getElementById('registerStoreDescription')
  };
  var button = document.getElementById('registerBtn');
  var errorBox = document.getElementById('registerError');

  function $(id) { return document.getElementById(id); }

  function showPanel(id) {
    PANELS.forEach(function (panel) { $(panel).classList.toggle('d-none', panel !== id); });
  }

  function showError(message) {
    errorBox.textContent = message || '';
    errorBox.classList.toggle('d-none', !message);
  }

  function setFieldError(input, message) {
    input.classList.toggle('is-invalid', !!message);
    var box = form.querySelector('[data-error-for="' + input.id + '"]');
    if (box) { box.textContent = message || ''; }
  }

  function sellerAccount(accounts) {
    for (var i = 0; i < accounts.length; i++) {
      if (accounts[i].vaiTro === OB.ROLE_SELLER) { return accounts[i]; }
    }
    return null;
  }

  // Quyết định hiện form hay lời nhắn, theo vai trò đang dùng và các tài khoản người dùng đã có.
  function decide(profile) {
    var user = Auth.getUser();
    var accounts = (profile && profile.taiKhoans) || [];

    if (!user || user.vaiTro !== OB.ROLE_CUSTOMER) {
      showPanel('wrongRolePanel');
      return;
    }

    var seller = sellerAccount(accounts);
    if (seller) {
      if (seller.trangThai !== ACCOUNT_ACTIVE) {
        $('alreadySellerText').textContent =
          'Tài khoản Chủ cửa hàng của bạn hiện không hoạt động. Vui lòng liên hệ hỗ trợ để được kiểm tra.';
      }
      showPanel('alreadySellerPanel');
      return;
    }

    if (!fields.phone.value && profile && profile.soDienThoai) {
      fields.phone.value = profile.soDienThoai;
    }
    showPanel('registerFormPanel');
  }

  function load() {
    showPanel('registerLoading');
    OB.request('GET', '/api/profile').then(decide, function (err) {
      if (Auth.handleAuthError(err)) { return; }
      $('registerLoadErrorText').textContent = (err && err.message) || 'Không kết nối được máy chủ.';
      showPanel('registerLoadError');
    });
  }

  function validate() {
    var name = fields.name.value.trim();
    var phone = fields.phone.value.trim();
    var address = fields.address.value.trim();

    setFieldError(fields.name, name ? '' : 'Nhập tên gian hàng.');
    setFieldError(fields.phone, !phone ? 'Nhập số điện thoại.'
      : (PHONE_PATTERN.test(phone) ? '' : 'Số điện thoại gồm 8 đến 20 chữ số.'));
    setFieldError(fields.address, address ? '' : 'Nhập địa chỉ lấy hàng.');

    var firstInvalid = form.querySelector('.is-invalid');
    if (firstInvalid) {
      firstInvalid.focus();
      return null;
    }

    return {
      tenCuaHang: name,
      soDienThoaiCH: phone,
      diaChiCH: address,
      moTa: fields.description.value.trim() || null
    };
  }

  function setBusy(busy) {
    button.disabled = busy;
    button.textContent = busy ? 'Đang gửi...' : 'Gửi đăng ký';
  }

  form.addEventListener('submit', function (event) {
    event.preventDefault();
    showError('');

    var body = validate();
    if (!body) { return; }

    setBusy(true);
    OB.request('POST', '/api/store/register', body).then(function (result) {
      // Gian hàng đã được tạo; từ đây lỗi chỉ còn là không tự chuyển được tài khoản.
      return Auth.switchAccount(result.maTK).catch(function () {
        Auth.loadProfileName();
        $('alreadySellerText').textContent =
          'Đã gửi đăng ký gian hàng. Chưa tự chuyển được tài khoản, hãy bấm nút bên dưới để chuyển.';
        showPanel('alreadySellerPanel');
      });
    }, function (err) {
      if (Auth.handleAuthError(err)) { return; }
      showError((err && err.message) || 'Không gửi được đăng ký. Vui lòng thử lại.');
      setBusy(false);
    });
  });

  ['name', 'phone', 'address'].forEach(function (key) {
    fields[key].addEventListener('input', function () { setFieldError(fields[key], ''); });
  });

  fields.description.addEventListener('input', function () {
    $('registerDescriptionCount').textContent = String(fields.description.value.length);
  });

  $('registerRetry').addEventListener('click', load);

  load();
})();
