(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || OB.cart) { return; }

  var Auth = OB.auth;
  var ROLE = OB.ROLE_CUSTOMER;
  var BASE = '/api/cart';

  var Cart = {

    get: function () {
      return OB.request('GET', BASE);
    },

    add: function (maSach, soLuong) {
      return OB.request('POST', BASE + '/items', { maSach: maSach, soLuong: soLuong || 1 });
    },

    update: function (maCTGioHang, soLuong) {
      return OB.request('PUT', BASE + '/items/' + encodeURIComponent(maCTGioHang), { soLuong: soLuong });
    },

    remove: function (maCTGioHang) {
      return OB.request('DELETE', BASE + '/items/' + encodeURIComponent(maCTGioHang));
    },

    clear: function () {
      return OB.request('DELETE', BASE);
    },

    setBadge: function (count) {
      var badges = document.querySelectorAll('.cart-count');
      for (var i = 0; i < badges.length; i++) {
        badges[i].textContent = String(count || 0);
      }
    },

    refreshBadge: function () {
      if (!Auth.isLoggedIn() || !Auth.hasRole(ROLE)) {
        Cart.setBadge(0);
        return;
      }

      Cart.get().then(function (cart) {
        Cart.setBadge(cart.soMuc);
      }, function (err) {
        if (err.status === 401) {
          Auth.clear();
          OB.renderHeader();
        }
        Cart.setBadge(0);
      });
    }
  };

  OB.cart = Cart;

  function setLoading(btn, loading) {
    if (loading) {
      btn.disabled = true;
      btn.setAttribute('data-original-html', btn.innerHTML);
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Đang thêm...';
    } else {
      btn.disabled = false;
      var original = btn.getAttribute('data-original-html');
      if (original !== null) { btn.innerHTML = original; }
    }
  }

  document.addEventListener('click', function (e) {
    var btn = e.target.closest ? e.target.closest('[data-add-to-cart]') : null;
    if (!btn) { return; }

    e.preventDefault();
    if (btn.disabled) { return; }

    if (!Auth.isLoggedIn()) {
      Auth.redirectToLogin();
      return;
    }

    if (!Auth.hasRole(ROLE)) {
      OB.toast('Chỉ tài khoản khách hàng mới dùng được giỏ hàng. Vui lòng đăng nhập bằng tài khoản khách hàng.', 'warning', { duration: 5000 });
      return;
    }

    var maSach = parseInt(btn.getAttribute('data-ma-sach'), 10);
    var soLuong = parseInt(btn.getAttribute('data-so-luong') || '1', 10);

    if (!maSach || !soLuong || soLuong < 1) {
      OB.toast('Không xác định được sách cần thêm.', 'danger');
      return;
    }

    setLoading(btn, true);

    Cart.add(maSach, soLuong).then(function (cart) {
      Cart.setBadge(cart.soMuc);
      OB.toast('Đã thêm sách vào giỏ hàng.', 'success', { link: { text: 'Xem giỏ hàng', href: '/cart' } });
    }).catch(function (err) {
      if (Auth.handleAuthError(err, ROLE)) { return; }
      OB.toast(err.message, 'danger', { duration: 5000 });
    }).then(function () {
      setLoading(btn, false);
    });
  });

  function init() {
    Cart.refreshBadge();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
