(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || OB.bookCards) { return; }
  var esc = OB.escapeHtml;

  var DEFAULT_COLUMNS = 'col-6 col-md-4 col-lg-3';

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

  // Phần trăm giảm so với giá bìa; 0 khi không có giá bìa hoặc giá bán không thấp hơn.
  function discountPercent(book) {
    var cover = Number(book.giaBia);
    var price = Number(book.giaBanCu);
    if (book.giaBia == null || book.giaBanCu == null || !(cover > 0) || !(price < cover)) { return 0; }
    return Math.round((1 - price / cover) * 100);
  }

  /*
   * Thẻ sách dùng chung cho trang chủ, danh sách, yêu thích và đã xem gần đây.
   * options.columns: lớp cột Bootstrap bọc ngoài thẻ. options.showWishlist = false để bỏ nút tim.
   */
  function render(book, options) {
    options = options || {};
    var id = bookId(book.maSach);
    if (!id) { return ''; }

    var href = '/books/' + id;
    var image = safeImage(book.hinhAnhUrl);
    var cover = '<span class="cover-title">' + esc(book.tenSach) + '</span>' +
      (image ? '<img class="book-card-image" src="' + esc(image) + '" alt="' + esc(book.tenSach) +
        '" loading="lazy" referrerpolicy="no-referrer">' : '');

    var percent = Number(book.doMoiPercent);
    var condition = book.doMoiPercent != null && Number.isFinite(percent)
      ? '<span class="condition-badge"' + (book.tinhTrangVatLy ? ' title="' + esc(book.tinhTrangVatLy) + '"' : '') +
        '>Độ mới ' + Math.round(percent) + '%</span>' : '';

    var category = book.tenDanhMuc
      ? '<span class="book-category-tag">' + esc(book.tenDanhMuc) + '</span>' : '';

    var heart = options.showWishlist === false ? '' :
      '<button type="button" class="wishlist-mini" data-toggle-wishlist data-requires-login data-ma-sach="' + id +
      '" aria-pressed="false" aria-label="Yêu thích sách ' + esc(book.tenSach) + '"><i class="bi bi-heart" aria-hidden="true"></i></button>';

    var discount = discountPercent(book);
    var priceRow = book.giaBanCu != null
      ? '<strong class="price">' + esc(OB.formatMoney(book.giaBanCu)) + '</strong>' +
        (discount ? '<s class="price-old">' + esc(OB.formatMoney(book.giaBia)) + '</s>' +
          '<span class="price-off">-' + discount + '%</span>' : '')
      : '<span class="text-muted">Chưa cập nhật giá</span>';

    var stock = Number(book.soLuongTon) || 0;
    var inStock = stock > 0;
    var stockNote = !inStock
      ? '<span class="book-stock-note">Hết hàng</span>'
      : (stock <= 3 ? '<span class="book-stock-note">Chỉ còn ' + stock + ' cuốn</span>' : '');

    var cartButton = inStock && book.giaBanCu != null
      ? '<button type="button" class="btn btn-accent btn-sm w-100" data-add-to-cart data-requires-login data-so-luong="1" data-ma-sach="' + id + '"><i class="bi bi-cart-plus me-1" aria-hidden="true"></i>Thêm vào giỏ</button>'
      : '<a href="' + href + '" class="btn btn-outline-brown btn-sm w-100">Xem chi tiết</a>';

    return '<article class="' + (options.columns || DEFAULT_COLUMNS) + '"><div class="book-card">' +
      '<div class="book-card-top">' +
        '<span class="shop-line"><i class="bi bi-shop me-1" aria-hidden="true"></i>' + esc(book.tenCuaHang || 'Cửa hàng') + '</span>' +
        condition +
      '</div>' +
      '<div class="book-cover-wrap"><a href="' + href + '" class="cover c' + (id % 6) + '">' + cover + '</a>' + category + heart + '</div>' +
      '<div class="book-info">' +
        '<h3 class="book-title"><a href="' + href + '">' + esc(book.tenSach) + '</a></h3>' +
        '<p class="book-author">Tác giả: ' + esc(book.tacGia || 'Chưa cập nhật') + '</p>' +
        '<div class="book-price-row">' + priceRow + '</div>' +
        stockNote +
        cartButton +
      '</div></div></article>';
  }

  document.addEventListener('error', function (event) {
    var image = event.target;
    if (image && image.classList && image.classList.contains('book-card-image')) {
      image.classList.add('d-none');
    }
  }, true);

  OB.bookCards = { bookId: bookId, render: render, discountPercent: discountPercent };
})();
