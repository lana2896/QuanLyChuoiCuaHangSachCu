(function () {
  'use strict';

  var OB = window.OldBook;
  var grid = document.getElementById('bookGrid');

  // Thẻ sách do book-cards.js vẽ (nạp trong footer, trước file này).
  if (!OB || !OB.bookCards || !grid) { return; }

  var NEW_BOOKS_LIMIT = 8;
  var FILTER_CHIPS_LIMIT = 5;
  var SHOP_LIST_LIMIT = 4;

  var allBooks = [];

  function $(id) { return document.getElementById(id); }
  function show(id) { $(id).classList.remove('d-none'); }
  function hide(id) { $(id).classList.add('d-none'); }

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

  function isHome(p) {
    return window.location.pathname === '/' && !p.q && !p.category;
  }

  function renderGrid(element, books, columns) {
    element.innerHTML = books.map(function (book) {
      return OB.bookCards.render(book, { columns: columns });
    }).join('');
  }

  function newestFirst(a, b) {
    var dateOrder = String(b.ngayTao || '').localeCompare(String(a.ngayTao || ''));
    return dateOrder || b.maSach - a.maSach;
  }

  // Đếm theo một trường, trả về [{ name, count }] nhiều nhất trước.
  function countBy(books, field) {
    var counts = {};
    var names = [];
    books.forEach(function (book) {
      var name = (book[field] || '').trim();
      if (!name) { return; }
      if (!counts[name]) { counts[name] = 0; names.push(name); }
      counts[name]++;
    });
    return names
      .map(function (name) { return { name: name, count: counts[name] }; })
      .sort(function (a, b) { return b.count - a.count || a.name.localeCompare(b.name); });
  }

  function renderNewBooks(category) {
    var books = category
      ? allBooks.filter(function (book) { return book.tenDanhMuc === category; })
      : allBooks;
    renderGrid(grid, books.slice().sort(newestFirst).slice(0, NEW_BOOKS_LIMIT));
  }

  // Chip lọc nhanh khối "Sách mới" theo danh mục, lọc ngay trên danh sách đã tải.
  function renderNewBooksFilter() {
    var box = $('newBooksFilter');
    var categories = countBy(allBooks, 'tenDanhMuc').slice(0, FILTER_CHIPS_LIMIT);
    if (categories.length < 2) { return; }

    box.textContent = '';
    [{ name: '', label: 'Tất cả' }].concat(categories.map(function (category) {
      return { name: category.name, label: category.name };
    })).forEach(function (item, index) {
      var chip = document.createElement('button');
      chip.type = 'button';
      chip.className = 'filter-chip' + (index === 0 ? ' active' : '');
      chip.setAttribute('aria-pressed', index === 0 ? 'true' : 'false');
      chip.setAttribute('data-new-category', item.name);
      chip.textContent = item.label;
      box.appendChild(chip);
    });

    box.addEventListener('click', function (event) {
      var chip = event.target.closest ? event.target.closest('[data-new-category]') : null;
      if (!chip) { return; }
      var chips = box.querySelectorAll('[data-new-category]');
      for (var i = 0; i < chips.length; i++) {
        chips[i].classList.toggle('active', chips[i] === chip);
        chips[i].setAttribute('aria-pressed', chips[i] === chip ? 'true' : 'false');
      }
      renderNewBooks(chip.getAttribute('data-new-category'));
    });
    box.classList.remove('d-none');
  }

  // Số liệu và danh sách cửa hàng đều tính từ sách đang bán, không có con số nào viết sẵn.
  function renderShops() {
    var shops = countBy(allBooks, 'tenCuaHang');

    $('homeStatBooks').textContent = String(allBooks.length);
    $('homeStatShops').textContent = String(shops.length);
    $('homeStats').classList.toggle('d-none', allBooks.length === 0);

    $('shopCount').textContent = shops.length ? shops.length + ' cửa hàng' : '';
    var list = $('shopList');
    list.textContent = '';
    shops.slice(0, SHOP_LIST_LIMIT).forEach(function (shop) {
      var item = document.createElement('li');
      item.className = 'shop-network-item';

      var name = document.createElement('span');
      name.className = 'shop-network-name';
      name.textContent = shop.name;

      var count = document.createElement('span');
      count.className = 'shop-network-books';
      count.textContent = shop.count + ' đầu sách';

      item.appendChild(name);
      item.appendChild(count);
      list.appendChild(item);
    });
  }

  function renderHome(books) {
    allBooks = books;

    var cheapBooks = books.filter(function (book) {
      return book.soLuongTon > 0 && book.giaBanCu != null;
    }).sort(function (a, b) {
      return Number(a.giaBanCu) - Number(b.giaBanCu) || newestFirst(a, b);
    }).slice(0, 4);
    var bookSets = books.filter(function (book) {
      var category = (book.tenDanhMuc || '').normalize('NFD').replace(/[̀-ͯ]/g, '').trim().toLowerCase();
      return category === 'bo sach';
    }).sort(newestFirst).slice(0, 6);

    renderGrid($('cheapBookGrid'), cheapBooks, 'col-6 col-md-3');
    renderNewBooks('');
    renderGrid($('bookSetGrid'), bookSets, 'col-6 col-md-4 col-lg-2');

    // Khối không có sách thì ẩn hẳn, không để lại tiêu đề trống.
    $('cheapBooksSection').classList.toggle('d-none', cheapBooks.length === 0);
    $('bookSetsSection').classList.toggle('d-none', bookSets.length === 0);

    renderNewBooksFilter();
    renderShops();
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
