/*
 * Thanh danh mục trên header khách hàng.
 * HTML có sẵn vài chip mặc định; file này thay chúng bằng danh mục thật từ /api/books/categories
 * và đánh dấu chip ứng với trang đang xem. API lỗi thì các chip mặc định vẫn dùng được.
 */
(function () {
  'use strict';

  var OB = window.OldBook;
  var list = document.querySelector('[data-category-chips]');
  if (!OB || !list) { return; }

  // Danh mục hiếm khi đổi: mỗi phiên trình duyệt chỉ hỏi máy chủ một lần.
  var CACHE_KEY = 'oldbook_categories';

  function readCache() {
    try {
      var names = JSON.parse(window.sessionStorage.getItem(CACHE_KEY));
      return Array.isArray(names) ? names : null;
    } catch (e) {
      return null;
    }
  }

  function writeCache(names) {
    try { window.sessionStorage.setItem(CACHE_KEY, JSON.stringify(names)); } catch (e) { }
  }

  // Trả về tên danh mục đang xem, '' nếu đang xem tất cả sách, null nếu trang này không thuộc chip nào.
  function currentCategory() {
    var path = window.location.pathname;
    if (path !== '/' && path !== '/books') { return null; }

    var params = new URLSearchParams(window.location.search);
    var category = (params.get('category') || '').trim();
    if (category) { return category; }

    return path === '/books' && !params.get('q') ? '' : null;
  }

  function markActive() {
    var current = currentCategory();
    var chips = list.querySelectorAll('.category-chip');
    for (var i = 0; i < chips.length; i++) {
      var active = current !== null && chips[i].getAttribute('data-category') === current;
      chips[i].classList.toggle('active', active);
      if (active) {
        chips[i].setAttribute('aria-current', 'page');
        if (chips[i].scrollIntoView) {
          chips[i].scrollIntoView({ block: 'nearest', inline: 'center' });
        }
      } else {
        chips[i].removeAttribute('aria-current');
      }
    }
  }

  function chip(name) {
    var item = document.createElement('li');
    var link = document.createElement('a');
    link.className = 'category-chip';
    link.setAttribute('href', '/books?category=' + encodeURIComponent(name));
    link.setAttribute('data-category', name);
    link.textContent = name;
    item.appendChild(link);
    return item;
  }

  // Giữ chip đầu ("Tất cả sách"), thay các chip còn lại.
  function render(names) {
    if (!names.length) { return; }
    while (list.children.length > 1) {
      list.removeChild(list.children[list.children.length - 1]);
    }
    names.forEach(function (name) { list.appendChild(chip(name)); });
    markActive();
  }

  markActive();

  var cached = readCache();
  if (cached) {
    render(cached);
    return;
  }

  OB.request('GET', '/api/books/categories', undefined, { token: null }).then(function (categories) {
    var names = (categories || [])
      .map(function (category) { return (category.tenDanhMuc || '').trim(); })
      .filter(function (name) { return !!name; });
    writeCache(names);
    render(names);
  }, function () { });
})();
