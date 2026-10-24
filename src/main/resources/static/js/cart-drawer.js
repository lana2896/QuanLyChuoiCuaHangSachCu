/*
 * Ngăn giỏ hàng trượt từ bên phải, mở bằng nút giỏ hàng nổi hoặc bất kỳ link "/cart" nào trên trang.
 * Dùng lại OB.cart của cart.js để gọi API, và ghi các dòng đã chọn vào đúng khóa sessionStorage
 * mà trang /cart dùng, nên trang /checkout nhận lựa chọn y như khi đi từ trang /cart.
 */
(function () {
  'use strict';

  var OB = window.OldBook;
  var drawer = document.getElementById('cartDrawer');
  if (!OB || !OB.cart || !drawer) { return; }

  var Auth = OB.auth;
  var Cart = OB.cart;
  var esc = OB.escapeHtml;
  var ROLE = OB.ROLE_CUSTOMER;
  var SELECTED_KEY = 'ob_checkout_items';

  // Trên hai trang này người dùng đang xem hoặc đang chốt giỏ hàng: không hiện nút nổi, link /cart đi như thường.
  var path = window.location.pathname;
  var drawerDisabled = path === '/cart' || path === '/checkout';

  var fab = document.querySelector('[data-cart-fab]');
  var list = drawer.querySelector('[data-cart-list]');
  var foot = drawer.querySelector('[data-cart-foot]');
  var alertBox = drawer.querySelector('[data-cart-alert]');
  var checkoutBtn = drawer.querySelector('[data-cart-checkout]');

  var cart = null;
  var selected = {};
  var selectionReady = false;
  var busy = false;

  function canUse() {
    return !drawerDisabled && Auth.isLoggedIn() && Auth.hasRole(ROLE);
  }

  function showState(name) {
    var states = drawer.querySelectorAll('[data-cart-state]');
    for (var i = 0; i < states.length; i++) {
      states[i].classList.toggle('d-none', states[i].getAttribute('data-cart-state') !== name);
    }
    foot.classList.toggle('d-none', name !== 'list');
  }

  function showAlert(message) {
    alertBox.textContent = message || '';
    alertBox.classList.toggle('d-none', !message);
  }

  function safeImage(url) {
    if (typeof url !== 'string') { return null; }
    if (/^https?:\/\//i.test(url)) { return url; }
    return url.charAt(0) === '/' && url.charAt(1) !== '/' ? url : null;
  }

  function lineHtml(item) {
    var id = Number(item.maCTGioHang);
    var href = '/books/' + encodeURIComponent(item.maSach);
    var image = safeImage(item.hinhAnhUrl);
    var max = Math.min(Number(item.soLuongTon) || 0, 999);
    var name = esc(item.tenSach);

    return '' +
      '<div class="cart-line' + (item.coTheMua ? '' : ' is-blocked') + '" data-id="' + id + '" data-qty="' + Number(item.soLuong) + '">' +
        '<input type="checkbox" class="form-check-input cart-line-check" aria-label="Chọn ' + name + '"' +
          (item.coTheMua ? '' : ' disabled') + (selected[id] ? ' checked' : '') + '>' +
        '<a class="cart-line-cover c' + (Number(item.maSach) % 6) + '" href="' + href + '" tabindex="-1" aria-hidden="true">' +
          (image ? '<img src="' + esc(image) + '" alt="" loading="lazy">' : '') +
        '</a>' +
        '<div class="cart-line-main">' +
          '<a class="cart-line-title" href="' + href + '">' + name + '</a>' +
          '<div class="cart-line-author">' + esc(item.tacGia || 'Đang cập nhật') + '</div>' +
          (item.canhBao
            ? '<div class="cart-line-warn"><i class="bi bi-exclamation-triangle me-1" aria-hidden="true"></i>' + esc(item.canhBao) + '</div>'
            : '') +
          '<div class="cart-line-bottom">' +
            '<span class="cart-line-price">' + esc(OB.formatMoney(item.donGia)) + '</span>' +
            '<div class="cart-qty">' +
              '<button type="button" data-cart-minus aria-label="Giảm số lượng ' + name + '"' + (item.soLuong <= 1 ? ' disabled' : '') + '>−</button>' +
              '<span class="cart-qty-value">' + Number(item.soLuong) + '</span>' +
              '<button type="button" data-cart-plus aria-label="Tăng số lượng ' + name + '"' +
                (!item.coTheMua || item.soLuong >= max ? ' disabled' : '') + '>+</button>' +
            '</div>' +
            '<button type="button" class="cart-line-remove" data-cart-remove aria-label="Xóa ' + name + ' khỏi giỏ" title="Xóa">' +
              '<i class="bi bi-trash" aria-hidden="true"></i></button>' +
          '</div>' +
        '</div>' +
      '</div>';
  }

  // Gom các dòng theo cửa hàng, giữ thứ tự xuất hiện trong giỏ.
  function listHtml(items) {
    var shops = [];
    var byShop = {};
    items.forEach(function (item) {
      var shop = item.tenCuaHang || 'Cửa hàng';
      if (!byShop[shop]) {
        byShop[shop] = [];
        shops.push(shop);
      }
      byShop[shop].push(item);
    });

    return shops.map(function (shop) {
      return '<div class="cart-shop"><i class="bi bi-shop" aria-hidden="true"></i>' + esc(shop) + '</div>' +
        byShop[shop].map(lineHtml).join('');
    }).join('');
  }

  // Lần đầu: chọn sẵn mọi dòng mua được. Về sau: giữ lựa chọn, bỏ dòng đã xóa hoặc không còn mua được.
  function syncSelection(items) {
    var buyable = {};
    items.forEach(function (item) {
      if (item.coTheMua) { buyable[Number(item.maCTGioHang)] = true; }
    });
    if (!selectionReady) {
      selected = buyable;
      selectionReady = true;
      return;
    }
    Object.keys(selected).forEach(function (id) {
      if (!buyable[id]) { delete selected[id]; }
    });
  }

  function selectedItems() {
    return ((cart && cart.items) || []).filter(function (item) {
      return item.coTheMua && selected[Number(item.maCTGioHang)];
    });
  }

  function updateSummary() {
    var items = selectedItems();
    var total = 0;
    items.forEach(function (item) { total += Number(item.thanhTien || 0); });

    drawer.querySelector('[data-cart-selected]').textContent = items.length + ' sản phẩm';
    drawer.querySelector('[data-cart-total]').textContent = OB.formatMoney(total);
    checkoutBtn.disabled = items.length === 0;
  }

  function render(data) {
    cart = data || { items: [], soMuc: 0 };
    var items = cart.items || [];
    Cart.setBadge(cart.soMuc);

    drawer.querySelector('[data-cart-count-line]').textContent = items.length
      ? items.length + ' đầu sách đang chờ bạn'
      : 'Chưa có cuốn nào';

    if (!items.length) {
      list.innerHTML = '';
      showState('empty');
      return;
    }

    syncSelection(items);
    list.innerHTML = listHtml(items);
    updateSummary();
    showState('list');
  }

  function fail(err) {
    if (Auth.handleAuthError(err, ROLE)) { return; }
    drawer.querySelector('[data-cart-error-text]').textContent =
      (err && err.message) || 'Không kết nối được máy chủ. Vui lòng thử lại.';
    showState('error');
  }

  function load() {
    showAlert('');
    if (!cart) { showState('loading'); }
    return Cart.get().then(render, fail);
  }

  // Tăng, giảm, xóa: khóa riêng dòng đang sửa, xong thì vẽ lại theo giỏ máy chủ trả về.
  function change(line, action) {
    if (busy) { return; }
    busy = true;
    showAlert('');
    line.classList.add('is-busy');

    action().then(render).catch(function (err) {
      if (Auth.handleAuthError(err, ROLE)) { return; }
      line.classList.remove('is-busy');
      showAlert((err && err.message) || 'Không cập nhật được giỏ hàng.');
      return Cart.get().then(render, function () { });
    }).then(function () {
      busy = false;
    });
  }

  list.addEventListener('click', function (e) {
    var line = e.target.closest ? e.target.closest('.cart-line') : null;
    if (!line) { return; }

    var id = line.getAttribute('data-id');
    var qty = parseInt(line.getAttribute('data-qty'), 10);

    if (e.target.closest('[data-cart-plus]')) {
      change(line, function () { return Cart.update(id, qty + 1); });
    } else if (e.target.closest('[data-cart-minus]')) {
      if (qty > 1) { change(line, function () { return Cart.update(id, qty - 1); }); }
    } else if (e.target.closest('[data-cart-remove]')) {
      change(line, function () { return Cart.remove(id); });
    }
  });

  list.addEventListener('change', function (e) {
    if (!e.target.classList || !e.target.classList.contains('cart-line-check')) { return; }
    var id = e.target.closest('.cart-line').getAttribute('data-id');
    if (e.target.checked) { selected[id] = true; } else { delete selected[id]; }
    updateSummary();
  });

  checkoutBtn.addEventListener('click', function () {
    var ids = selectedItems().map(function (item) { return item.maCTGioHang; });
    if (!ids.length) { return; }
    try {
      window.sessionStorage.setItem(SELECTED_KEY, JSON.stringify(ids));
    } catch (err) {
      showAlert('Trình duyệt không cho lưu lựa chọn. Vui lòng bật lưu trữ phiên (sessionStorage) rồi thử lại.');
      return;
    }
    Auth.navigate('/checkout');
  });

  drawer.querySelector('[data-cart-retry]').addEventListener('click', load);
  drawer.addEventListener('show.bs.offcanvas', load);

  function open() {
    window.bootstrap.Offcanvas.getOrCreateInstance(drawer).show();
  }

  // Mọi link tới /cart (nút nổi, toast "Xem giỏ hàng", footer) mở ngăn thay vì chuyển trang.
  // Riêng link "Xem giỏ hàng đầy đủ" trong ngăn (data-cart-page) vẫn chuyển trang.
  document.addEventListener('click', function (e) {
    var link = e.target.closest ? e.target.closest('a[href="/cart"]') : null;
    if (!link || link.hasAttribute('data-cart-page')) { return; }
    if (e.defaultPrevented || e.button || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) { return; }
    if (!canUse()) { return; }

    e.preventDefault();
    open();
  });

  if (fab) {
    if (drawerDisabled) { fab.classList.add('d-none'); }

    // cart.js cập nhật số trong .cart-count; ở đây chỉ theo dõi để ẩn số 0 và làm nút nảy nhẹ khi số đổi.
    var badge = fab.querySelector('.cart-count');
    var syncBadge = function (bump) {
      var count = parseInt(badge.textContent, 10) || 0;
      fab.classList.toggle('is-empty', count === 0);
      fab.setAttribute('aria-label', count ? 'Mở giỏ hàng, ' + count + ' sản phẩm' : 'Mở giỏ hàng');
      if (bump && count > 0) {
        fab.classList.remove('is-bumped');
        void fab.offsetWidth;
        fab.classList.add('is-bumped');
      }
    };
    syncBadge(false);
    if (window.MutationObserver) {
      new window.MutationObserver(function () { syncBadge(true); })
        .observe(badge, { childList: true, characterData: true, subtree: true });
    }
  }

  OB.cartDrawer = { open: open, reload: load };
})();
