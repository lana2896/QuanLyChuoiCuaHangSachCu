/*
 * Chuyển mục trong cổng Quản lý mà không tải lại cả trang (điều hướng phía trình duyệt kiểu PJAX).
 *
 * Link có data-shell-link: tải HTML của trang đích, chỉ lấy <main id="managerMain"> và <title>, thay vào trang hiện tại
 * rồi đổi địa chỉ bằng History API. Script riêng của từng trang nằm trong <main> dưới dạng
 * <script type="text/x-page-script" src="..."> và được tạo lại thành thẻ chạy thật ở đây (cả lần tải đầu tiên).
 *
 * Trước khi thay nội dung, phát sự kiện "manager:leave" trên document để trang cũ dọn hẹn giờ, đăng ký realtime.
 * Lỗi mạng, hoặc trang đích không có #managerMain (ví dụ bị chuyển sang trang đăng nhập): tải lại bình thường.
 */
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || OB.managerShell) { return; }

  var MAIN_ID = 'managerMain';
  var progressTimer = null;
  var navId = 0;

  function main() { return document.getElementById(MAIN_ID); }

  // ---------- thanh tiến trình ----------
  function progress(state) {
    var bar = document.getElementById('mgProgress');
    if (!bar) { return; }
    window.clearTimeout(progressTimer);
    if (state === 'start') {
      bar.style.width = '0';
      bar.classList.add('is-active');
      progressTimer = window.setTimeout(function () { bar.style.width = '70%'; }, 20);
    } else {
      bar.style.width = '100%';
      progressTimer = window.setTimeout(function () {
        bar.classList.remove('is-active');
        bar.style.width = '0';
      }, 250);
    }
  }

  // ---------- script của trang ----------
  // Chạy lần lượt từng script theo thứ tự trong <main>, vì script sau có thể dựa vào script trước.
  function activateScripts(root) {
    var nodes = Array.prototype.slice.call(root.querySelectorAll('script[data-page-script]'));
    return nodes.reduce(function (chain, old) {
      return chain.then(function () {
        return new Promise(function (resolve) {
          var script = document.createElement('script');
          script.src = old.getAttribute('src');
          script.onload = resolve;
          script.onerror = resolve;
          old.parentNode.replaceChild(script, old);
        });
      });
    }, Promise.resolve());
  }

  // ---------- dọn trang cũ ----------
  function leave() {
    document.dispatchEvent(new CustomEvent('manager:leave'));
    // Modal của trang cũ nằm trong <main> nên sẽ bị xóa; gỡ luôn lớp nền và khóa cuộn của Bootstrap.
    document.querySelectorAll('.modal-backdrop').forEach(function (n) { n.remove(); });
    document.body.classList.remove('modal-open');
    document.body.style.removeProperty('overflow');
    document.body.style.removeProperty('padding-right');
  }

  function closeOffcanvas() {
    var el = document.getElementById('mgSidebar');
    if (el && window.bootstrap && el.classList.contains('show')) {
      var inst = window.bootstrap.Offcanvas.getInstance(el);
      if (inst) { inst.hide(); }
    }
  }

  function markMenu(menu) {
    document.querySelectorAll('.mg-nav [data-menu]').forEach(function (a) {
      var active = a.getAttribute('data-menu') === menu;
      a.classList.toggle('active', active);
      if (active) { a.setAttribute('aria-current', 'page'); } else { a.removeAttribute('aria-current'); }
    });
  }

  function applyPage(doc) {
    var next = doc.getElementById(MAIN_ID);
    var cur = main();
    cur.innerHTML = next.innerHTML;
    cur.setAttribute('data-title', next.getAttribute('data-title') || '');
    cur.setAttribute('data-menu', next.getAttribute('data-menu') || '');

    var title = next.getAttribute('data-title') || '';
    document.title = doc.title || document.title;
    var heading = document.getElementById('mgTitle');
    if (heading) { heading.textContent = title; }
    var crumb = document.getElementById('mgCrumb');
    if (crumb) { crumb.textContent = title; }
    markMenu(next.getAttribute('data-menu'));
    var updated = document.getElementById('mgUpdated');
    if (updated) { updated.textContent = ''; }
    window.scrollTo(0, 0);
    closeOffcanvas();

    // auth.js điền tên người dùng, ẩn/hiện theo vai trò cho phần tử mới.
    if (OB.renderHeader) { OB.renderHeader(); }
    return activateScripts(cur);
  }

  function hardLoad(url) { window.location.href = url; }

  // push = true: link được bấm (thêm vào lịch sử); false: Back/Forward.
  function navigate(url, push) {
    var id = ++navId;
    progress('start');

    return window.fetch(url, { credentials: 'same-origin', headers: { 'X-Requested-With': 'manager-shell' } })
      .then(function (res) {
        // Bị chuyển sang miền/đường dẫn khác (ví dụ trang đăng nhập) hoặc lỗi máy chủ: tải lại bình thường.
        if (!res.ok || new URL(res.url).origin !== window.location.origin) { throw new Error('hard'); }
        return res.text();
      })
      .then(function (html) {
        if (id !== navId) { return; }
        var doc = new DOMParser().parseFromString(html, 'text/html');
        if (!doc.getElementById(MAIN_ID)) { throw new Error('hard'); }
        // Trang đánh dấu data-full-reload (danh mục, voucher) phải tải lại cả trang.
        if (doc.getElementById(MAIN_ID).querySelector('[data-full-reload]')) { throw new Error('hard'); }

        leave();
        if (push) { window.history.pushState({ managerShell: true }, '', url); }
        return applyPage(doc).then(function () {
          document.dispatchEvent(new CustomEvent('manager:enter'));
          if (OB.managerRealtime) { OB.managerRealtime.refreshBadges(); }
          progress('done');
        });
      })
      .catch(function () {
        progress('done');
        hardLoad(url);
      });
  }

  // ---------- sự kiện ----------
  document.addEventListener('click', function (event) {
    var link = event.target.closest ? event.target.closest('a[data-shell-link]') : null;
    if (!link) { return; }
    if (event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) { return; }
    if (link.target && link.target !== '_self') { return; }

    var url = new URL(link.href, window.location.href);
    if (url.origin !== window.location.origin) { return; }

    // Đang ở trang tải lại cả trang (danh mục, voucher): để trình duyệt điều hướng bình thường.
    if (main() && main().querySelector('[data-full-reload]')) { return; }

    event.preventDefault();
    var target = url.pathname + url.search;
    if (target === window.location.pathname + window.location.search) { return; }
    navigate(target, true);
  });

  window.addEventListener('popstate', function () {
    navigate(window.location.pathname + window.location.search, false);
  });

  var refresh = document.getElementById('mgRefresh');
  if (refresh) {
    refresh.addEventListener('click', function () {
      if (main() && main().querySelector('[data-full-reload]')) { window.location.reload(); return; }
      if (!OB.managerRealtime) { return; }
      refresh.classList.add('is-spinning');
      OB.managerRealtime.refreshAll();
      window.setTimeout(function () { refresh.classList.remove('is-spinning'); }, 700);
    });
  }

  OB.managerShell = {
    navigate: function (url) { return navigate(url, true); },
    // Trang gọi sau mỗi lần tải dữ liệu xong để ghi dòng "Cập nhật lúc".
    touch: function () {
      var node = document.getElementById('mgUpdated');
      if (!node) { return; }
      var now = new Date();
      node.textContent = 'Cập nhật lúc ' + String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');
    },
    // Đổi query ?tab= mà không thêm/tải gì (dùng cho thẻ con).
    setQuery: function (name, value) {
      var url = new URL(window.location.href);
      url.searchParams.set(name, value);
      window.history.replaceState(window.history.state, '', url.pathname + url.search);
    }
  };

  // Lần tải đầu: chạy script của trang. Script thường chạy khi chưa đăng nhập sẽ tự thoát (auth.js chuyển về /login).
  var first = main();
  if (first) {
    window.history.replaceState({ managerShell: true }, '', window.location.pathname + window.location.search);
    activateScripts(first).then(function () { document.dispatchEvent(new CustomEvent('manager:enter')); });
  }
})();
