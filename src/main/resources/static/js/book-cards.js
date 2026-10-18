(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || OB.bookCards) { return; }
  var esc = OB.escapeHtml;

  function bookId(value) {
    var id = Number(value);
    return Number.isSafeInteger(id) && id > 0 ? id : null;
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

  function render(book, options) {
    var id = bookId(book.maSach);
    if (!id) { return ''; }
    var href = '/books/' + id;
    var image = safeImage(book.hinhAnhUrl);
    var cover = '<span class="cover-title">' + esc(book.tenSach) + '</span>' +
      (image ? '<img class="book-card-image" src="' + esc(image) + '" alt="' + esc(book.tenSach) +
        '" loading="lazy" referrerpolicy="no-referrer">' : '');
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
    var heart = options && options.showWishlist === false ? '' :
      '<button type="button" class="wishlist-mini" data-toggle-wishlist data-requires-login data-ma-sach="' + id +
      '" aria-pressed="false" aria-label="Yêu thích sách ' + esc(book.tenSach) + '"><i class="bi bi-heart" aria-hidden="true"></i></button>';
    var originalPrice = book.giaBia != null
      ? '<p class="price-line">Giá bìa: <s>' + esc(OB.formatMoney(book.giaBia)) + '</s></p>' : '';

    return '<article class="col-6 col-md-4 col-lg-3"><div class="book-card">' +
      '<div class="book-cover-wrap"><a href="' + href + '" class="cover c' + (id % 6) + '">' + cover + '</a>' + heart + '</div>' +
      '<div class="book-info">' + condition +
      '<h3 class="book-title"><a href="' + href + '">' + esc(book.tenSach) + '</a></h3>' +
      '<p class="book-author">Tác giả: ' + esc(book.tacGia || 'Chưa cập nhật') + '</p>' +
      '<p class="shop-line mb-1"><i class="bi bi-shop me-1" aria-hidden="true"></i>' + esc(book.tenCuaHang) + '</p>' +
      originalPrice + '<p class="price-line mb-2">Giá bán: ' + price + '</p>' + cartButton +
      (!inStock ? '<span class="small text-muted mt-2">Hết hàng</span>' : '') +
      '</div></div></article>';
  }

  document.addEventListener('error', function (event) {
    var image = event.target;
    if (image && image.classList && image.classList.contains('book-card-image')) {
      image.classList.add('d-none');
    }
  }, true);

  OB.bookCards = { bookId: bookId, render: render };
})();
