(function () {
  'use strict';

  var OB = window.OldBook;
  var grid = document.getElementById('bookGrid');

  if (!OB || !grid) { return; }

  var esc = OB.escapeHtml;

  function $(id) { return document.getElementById(id); }
  function show(id) { $(id).classList.remove('d-none'); }
  function hide(id) { $(id).classList.add('d-none'); }

  function safeImg(url) {
    if (!url) { return null; }
    if (/^https?:\/\//i.test(url)) { return url; }
    if (url.charAt(0) === '/' && url.charAt(1) !== '/') { return url; }
    return null;
  }

  function params() {
    var sp = new window.URLSearchParams(window.location.search);
    return {
      q: (sp.get('q') || '').trim(),
      category: (sp.get('category') || '').trim()
    };
  }

  function buildUrl(p) {
    var parts = [];
    if (p.q) { parts.push('q=' + encodeURIComponent(p.q)); }
    if (p.category) { parts.push('category=' + encodeURIComponent(p.category)); }
    return '/api/books' + (parts.length ? '?' + parts.join('&') : '');
  }

  function cardHtml(b, columns) {
    var ton = b.soLuongTon || 0;
    var conHang = ton > 0;
    var img = safeImg(b.hinhAnhUrl);

    var cover = img
      ? '<div class="cover" style="padding:0">' +
          '<img src="' + esc(img) + '" alt="' + esc(b.tenSach) + '" ' +
          'style="position:absolute;inset:0;width:100%;height:100%;object-fit:cover">' +
        '</div>'
      : '<div class="cover c' + (b.maSach % 6) + '">' +
          '<span class="cover-title">' + esc(b.tenSach) + '</span>' +
        '</div>';

    var badge = b.doMoiPercent
      ? '<span class="condition-badge mb-1"' +
          (b.tinhTrangVatLy ? ' title="' + esc(b.tinhTrangVatLy) + '"' : '') + '>✓ ' +
          Math.round(b.doMoiPercent) + '% mới</span>'
      : '';

    var giaBia = b.giaBia
      ? '<p class="price-line">Giá bìa: <span>' + OB.formatMoney(b.giaBia) + '</span></p>'
      : '';

    var stock = '';
    if (!conHang) {
      stock = '<p class="small text-danger mb-0">Hết hàng</p>';
    } else if (ton <= 3) {
      stock = '<p class="small text-danger mb-0">Chỉ còn ' + ton + ' cuốn</p>';
    }

    var button = conHang
      ? '<button type="button" class="btn btn-accent btn-sm w-100 mt-2" ' +
          'data-add-to-cart data-requires-login data-so-luong="1" ' +
          'data-ma-sach="' + b.maSach + '">' +
          '<i class="bi bi-cart-plus me-1"></i>Thêm vào giỏ</button>'
      : '<button type="button" class="btn btn-outline-secondary btn-sm w-100 mt-2" disabled>' +
          'Hết hàng</button>';

    return '' +
      '<div class="' + (columns || 'col-6 col-md-4 col-lg-3') + '">' +
        '<div class="book-card">' +
          cover +
          '<div class="book-info">' +
            badge +
            '<h3 class="book-title"><a href="/books/' + encodeURIComponent(b.maSach) + '" class="text-reset text-decoration-none">' + esc(b.tenSach) + '</a></h3>' +
            '<p class="book-author">Tác giả: ' + esc(b.tacGia || 'Đang cập nhật') + '</p>' +
            '<p class="shop-line mb-1"><i class="bi bi-shop"></i> ' + esc(b.tenCuaHang) + '</p>' +
            giaBia +
            '<p class="price-line">Giá bán: <strong class="price">' + OB.formatMoney(b.giaBanCu) + '</strong></p>' +
            stock +
            button +
          '</div>' +
        '</div>' +
      '</div>';
  }

  function isHome(p) {
    return window.location.pathname === '/' && !p.q && !p.category;
  }

  function renderGrid(element, books, columns) {
    element.innerHTML = books.map(function (book) { return cardHtml(book, columns); }).join('');
  }

  function newestFirst(a, b) {
    var dateOrder = String(b.ngayTao || '').localeCompare(String(a.ngayTao || ''));
    return dateOrder || b.maSach - a.maSach;
  }

  function renderHome(books) {
    var cheapBooks = books.filter(function (book) {
      return book.soLuongTon > 0 && book.giaBanCu != null;
    }).sort(function (a, b) {
      return Number(a.giaBanCu) - Number(b.giaBanCu) || newestFirst(a, b);
    }).slice(0, 4);
    var newBooks = books.slice().sort(newestFirst).slice(0, 8);
    var bookSets = books.filter(function (book) {
      var category = (book.tenDanhMuc || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();
      return category === 'bo sach';
    }).sort(newestFirst).slice(0, 6);

    renderGrid($('cheapBookGrid'), cheapBooks, 'col-6 col-md-3');
    renderGrid(grid, newBooks);
    renderGrid($('bookSetGrid'), bookSets, 'col-6 col-md-4 col-lg-2');
    $('cheapBooksEmpty').classList.toggle('d-none', cheapBooks.length > 0);
    $('bookSetsEmpty').classList.toggle('d-none', bookSets.length > 0);
    show('catalogHomeIntro');
    show('catalogHomeOutro');
  }

  function setHeading(p) {
    var title = isHome(p) ? 'Sách mới' : 'Sách nổi bật';

    if (p.q && p.category) {
      title = 'Kết quả cho "' + p.q + '" trong ' + p.category;
    } else if (p.q) {
      title = 'Kết quả cho "' + p.q + '"';
    } else if (p.category) {
      title = 'Danh mục: ' + p.category;
    }

    $('catalogTitle').textContent = title;

    $('catalogSeeAll').classList.toggle('d-none', !isHome(p) && !p.q && !p.category);
  }

  function load() {
    var p = params();
    setHeading(p);

    OB.request('GET', buildUrl(p)).then(function (books) {
      hide('catalogLoading');

      if (isHome(p)) {
        renderHome(books || []);
      }

      if (!books || books.length === 0) {
        $('catalogEmptyText').textContent = (p.q || p.category)
          ? 'Không tìm thấy sách phù hợp.'
          : 'Chưa có sách nào được đăng bán.';
        show('catalogEmpty');
        return;
      }

      if (!isHome(p)) {
        renderGrid(grid, books);
      }
    }).catch(function (err) {
      hide('catalogLoading');
      $('catalogAlert').textContent = err.message || 'Không tải được danh sách sách.';
      show('catalogAlert');
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', load);
  } else {
    load();
  }
})();
