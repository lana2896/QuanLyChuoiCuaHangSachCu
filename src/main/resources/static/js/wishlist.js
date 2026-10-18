(function () {
  'use strict';

  var OB = window.OldBook;
  var grid = document.getElementById('wishlistGrid');
  var toggles = document.querySelectorAll('[data-toggle-wishlist]');
  if (!OB || (!grid && !toggles.length) || OB.wishlist) { return; }

  var Auth = OB.auth;
  var ROLE = OB.ROLE_CUSTOMER;
  var esc = OB.escapeHtml;
  var books = [];
  var known = false;
  var loading = false;
  var failedLoad = false;
  var pending = Object.create(null);
  var BASE = '/api/wishlist';

  var Wishlist = {
    get: function () { return OB.request('GET', BASE); },
    add: function (id) { return OB.request('PUT', BASE + '/' + encodeURIComponent(id)); },
    remove: function (id) { return OB.request('DELETE', BASE + '/' + encodeURIComponent(id)); }
  };
  OB.wishlist = Wishlist;

  function $(id) { return document.getElementById(id); }
  function visible(id, show) {
    var element = $(id);
    if (element) { element.classList.toggle('d-none', !show); }
  }

  function bookId(value) {
    var id = Number(value);
    return Number.isSafeInteger(id) && id > 0 ? id : null;
  }

  function isSaved(id) {
    return books.some(function (book) { return Number(book.maSach) === id; });
  }

  function updateButtons() {
    var buttons = document.querySelectorAll('[data-toggle-wishlist], [data-remove-wishlist]');
    for (var i = 0; i < buttons.length; i++) {
      var button = buttons[i];
      var id = bookId(button.getAttribute('data-ma-sach'));
      var busy = loading || !!pending[id];
      button.disabled = busy;
      button.setAttribute('aria-busy', String(busy));
      if (!button.hasAttribute('data-toggle-wishlist')) { continue; }

      var saved = known && isSaved(id);
      button.setAttribute('aria-pressed', String(saved));
      button.classList.toggle('active', saved);
      var heart = button.querySelector('i');
      if (heart) {
        heart.classList.toggle('bi-heart-fill', saved);
        heart.classList.toggle('bi-heart', !saved);
      }
      var label = button.querySelector('[data-wishlist-label]');
      if (label) { label.textContent = !known && failedLoad ? 'Thử lại' : saved ? 'Đã yêu thích' : 'Yêu thích'; }
    }
    if ($('wishlistRetry')) { $('wishlistRetry').disabled = loading; }
  }

  function safeImage(value) {
    if (typeof value !== 'string' || /[\\\u0000-\u001f\u007f]/.test(value)) { return null; }
    if (/^\/(?![\/\\])/.test(value)) { return value; }
    if (!/^https?:\/\//i.test(value)) { return null; }
    try {
      var url = new URL(value);
      return url.protocol === 'http:' || url.protocol === 'https:' ? value : null;
    } catch (e) { return null; }
  }

  function cardHtml(book) {
    var id = bookId(book.maSach);
    var href = '/books/' + id;
    var image = safeImage(book.hinhAnhUrl);
    var cover = image
      ? '<img src="' + esc(image) + '" alt="' + esc(book.tenSach) + '" style="position:absolute;inset:0;width:100%;height:100%;object-fit:cover" loading="lazy">'
      : '<span class="cover-title">' + esc(book.tenSach) + '</span>';
    var percent = Number(book.doMoiPercent);
    var condition = book.doMoiPercent != null && Number.isFinite(percent)
      ? '<span class="condition-badge align-self-start mb-2">✓ ' + Math.round(percent) + '% mới</span>' : '';
    var price = book.giaBanCu != null
      ? '<strong class="price">' + esc(OB.formatMoney(book.giaBanCu)) + '</strong>'
      : '<span class="text-muted">Chưa cập nhật</span>';
    var inStock = Number(book.soLuongTon) > 0;
    var cartButton = inStock && book.giaBanCu != null
      ? '<button type="button" class="btn btn-accent btn-sm w-100" data-add-to-cart data-requires-login data-so-luong="1" data-ma-sach="' + id + '"><i class="bi bi-cart-plus me-1" aria-hidden="true"></i>Thêm vào giỏ</button>'
      : '<a href="' + href + '" class="btn btn-outline-brown btn-sm w-100">Xem chi tiết</a>';

    return '<article class="col-6 col-md-4 col-lg-3"><div class="book-card">' +
      '<a href="' + href + '" class="cover c' + (id % 6) + '">' + cover + '</a>' +
      '<div class="book-info">' + condition +
      '<h3 class="book-title"><a href="' + href + '">' + esc(book.tenSach) + '</a></h3>' +
      '<p class="book-author">Tác giả: ' + esc(book.tacGia || 'Chưa cập nhật') + '</p>' +
      '<p class="shop-line mb-1"><i class="bi bi-shop me-1" aria-hidden="true"></i>' + esc(book.tenCuaHang) + '</p>' +
      '<p class="price-line mb-2">Giá bán: ' + price + '</p>' + cartButton +
      (!inStock ? '<span class="small text-muted mt-2">Hết hàng</span>' : '') +
      '<button type="button" class="btn btn-outline-brown btn-sm w-100 mt-2" data-remove-wishlist data-ma-sach="' + id + '"><i class="bi bi-heartbreak me-1" aria-hidden="true"></i>Bỏ yêu thích</button>' +
      '</div></div></article>';
  }

  function render() {
    if (grid) { grid.innerHTML = books.map(cardHtml).join(''); }
    if ($('wishlistCount')) { $('wishlistCount').textContent = books.length + ' cuốn'; }
    visible('wishlistEmpty', known && books.length === 0);
    updateButtons();
  }

  function clearError() {
    visible('wishlistError', false);
    visible('wishlistRetry', false);
  }

  function reportError(err, retry) {
    if (Auth.handleAuthError(err, ROLE)) { return; }
    var message = err.message || 'Không tải được danh sách yêu thích. Vui lòng thử lại.';
    if ($('wishlistError')) { $('wishlistError').textContent = message; }
    visible('wishlistError', true);
    visible('wishlistRetry', retry);
    OB.toast(message, 'danger', { duration: 5000 });
  }

  function load() {
    if (loading || Object.keys(pending).length) { return; }
    loading = true;
    clearError();
    visible('wishlistLoading', true);
    updateButtons();
    Wishlist.get().then(function (items) {
      books = (items || []).filter(function (book) { return book && bookId(book.maSach); });
      known = true;
      failedLoad = false;
      render();
    }, function (err) {
      failedLoad = true;
      reportError(err, true);
    }).then(function () {
      loading = false;
      visible('wishlistLoading', false);
      updateButtons();
    });
  }

  function mutate(id, remove) {
    pending[id] = true;
    clearError();
    updateButtons();
    (remove ? Wishlist.remove(id) : Wishlist.add(id)).then(function (book) {
      books = books.filter(function (item) { return Number(item.maSach) !== id; });
      if (!remove && book && bookId(book.maSach)) { books.unshift(book); }
      render();
      OB.toast(remove ? 'Đã bỏ sách khỏi danh sách yêu thích.' : 'Đã thêm sách vào danh sách yêu thích.', 'success',
        remove ? {} : { link: { text: 'Xem danh sách yêu thích', href: '/wishlist' } });
    }, function (err) {
      reportError(err, false);
    }).then(function () {
      delete pending[id];
      updateButtons();
    });
  }

  document.addEventListener('click', function (event) {
    var button = event.target.closest ? event.target.closest('[data-toggle-wishlist], [data-remove-wishlist], #wishlistRetry') : null;
    if (!button) { return; }
    event.preventDefault();
    if (button.disabled) { return; }
    if (!Auth.isLoggedIn()) { Auth.redirectToLogin(); return; }
    if (!Auth.hasRole(ROLE)) {
      OB.toast('Chỉ tài khoản khách hàng mới sử dụng được danh sách yêu thích.', 'warning');
      return;
    }
    if (button.id === 'wishlistRetry' || !known) { load(); return; }
    var id = bookId(button.getAttribute('data-ma-sach'));
    if (!id) { OB.toast('Không xác định được sách cần yêu thích.', 'danger'); return; }
    if (pending[id]) { return; }
    mutate(id, button.hasAttribute('data-remove-wishlist') || isSaved(id));
  });

  function init() {
    if (!Auth.isLoggedIn()) { return; }
    if (!Auth.hasRole(ROLE)) {
      visible('wishlistLoading', false);
      visible('wishlistAccessError', true);
      return;
    }
    load();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
