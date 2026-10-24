/*
 * Thông báo của tài khoản đang đăng nhập (chuông trên header).
 *
 * REST /api/notifications là nguồn dữ liệu chính: danh sách, số chưa đọc, đánh dấu đã đọc.
 * WebSocket /ws/notifications chỉ làm một việc: báo "vừa có thông báo mới" để chuông cập nhật ngay.
 * Không mở được socket thì chuông vẫn đúng sau mỗi lần tải trang; file này không hỏi máy chủ theo chu kỳ.
 *
 * Trang nào có khối [data-notify] thì được gắn giao diện chuông. Trang khác (ví dụ admin, có chuông riêng)
 * dùng OB.notifications.onNew(...) để nghe thông báo mới.
 */
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || OB.notifications) { return; }

  var Auth = OB.auth;
  var API = '/api/notifications';
  var PAGE_SIZE = 10;
  var KEEP_ALIVE_MS = 120000;
  var MAX_RECONNECTS = 3;

  // Mức độ luôn đi kèm biểu tượng, không chỉ phân biệt bằng màu.
  var LEVELS = {
    NGHIEM_TRONG: { css: 'is-critical', icon: 'bi-shield-exclamation', label: 'Nghiêm trọng' },
    QUAN_TRONG: { css: 'is-important', icon: 'bi-exclamation-circle', label: 'Quan trọng' },
    THONG_TIN: { css: '', icon: 'bi-info-circle', label: '' }
  };

  var unread = 0;
  var newListeners = [];
  var unreadListeners = [];

  function setUnread(count) {
    unread = Math.max(0, Number(count) || 0);
    unreadListeners.forEach(function (listener) { listener(unread); });
  }

  // Chỉ nhận đường dẫn trong chính trang web này, không nhận địa chỉ ngoài như "//evil.example".
  function internalPath(path) {
    var internal = typeof path === 'string'
      && path.charAt(0) === '/'
      && path.charAt(1) !== '/'
      && path.charAt(1) !== '\\';
    return internal ? path : '';
  }

  var Notifications = {

    getUnread: function () { return unread; },

    refreshUnread: function () {
      return OB.request('GET', API + '/chua-doc').then(function (data) {
        setUnread(data && data.soLuong);
      }, function () { });
    },

    list: function (page) {
      return OB.request('GET', API + '?page=' + (page || 0) + '&size=' + PAGE_SIZE);
    },

    markRead: function (maThongBao) {
      return OB.request('PUT', API + '/' + encodeURIComponent(maThongBao) + '/read').then(function () {
        setUnread(unread - 1);
      });
    },

    markAllRead: function () {
      return OB.request('PUT', API + '/read').then(function () { setUnread(0); });
    },

    // listener(thongBao) được gọi mỗi khi máy chủ đẩy một thông báo mới qua socket.
    onNew: function (listener) { newListeners.push(listener); },

    // listener(soChuaDoc) được gọi mỗi khi số chưa đọc thay đổi.
    onUnreadChange: function (listener) { unreadListeners.push(listener); },

    internalPath: internalPath
  };

  OB.notifications = Notifications;

  // ---------------------------------------------------------
  // Kênh đẩy
  // ---------------------------------------------------------

  var socket = null;
  var keepAlive = null;
  var reconnects = 0;
  var leaving = false;

  function handleMessage(event) {
    var message;
    try { message = JSON.parse(event.data); } catch (e) { return; }
    if (!message || message.type !== 'THONG_BAO_MOI') { return; }

    setUnread(message.chuaDoc === undefined || message.chuaDoc === null ? unread + 1 : message.chuaDoc);
    newListeners.forEach(function (listener) { listener(message.thongBao || {}); });
  }

  function closeSocket() {
    if (keepAlive) { window.clearInterval(keepAlive); keepAlive = null; }
    if (socket) {
      socket.onclose = null;
      try { socket.close(); } catch (e) { }
      socket = null;
    }
  }

  // JWT không được nằm trên URL, nên trước khi mở socket phải xin một vé dùng một lần qua REST.
  function connect() {
    if (socket || leaving || !window.WebSocket || !Auth.isLoggedIn()) { return; }

    OB.request('POST', API + '/ticket').then(function (data) {
      if (!data || !data.ticket || socket || leaving) { return; }

      // http://máy-chủ thành ws://máy-chủ, https thành wss.
      socket = new window.WebSocket(
        window.location.origin.replace(/^http/, 'ws') + '/ws/notifications?ticket=' + encodeURIComponent(data.ticket));

      socket.onopen = function () {
        reconnects = 0;
        // Giữ kết nối sống qua các proxy cắt kết nối rảnh.
        keepAlive = window.setInterval(function () {
          if (socket && socket.readyState === 1) { socket.send('ping'); }
        }, KEEP_ALIVE_MS);
      };
      socket.onmessage = handleMessage;
      socket.onclose = function () {
        closeSocket();
        // Thử lại vài lần rồi thôi: máy chủ có thể đang hết lượt kết nối, chuông vẫn đúng khi tải lại trang.
        if (!leaving && reconnects < MAX_RECONNECTS && Auth.isLoggedIn()) {
          reconnects++;
          window.setTimeout(connect, 5000 * reconnects);
        }
      };
    }, function () { });
  }

  window.addEventListener('pagehide', function () {
    leaving = true;
    closeSocket();
  });

  // Mở kênh đẩy nếu chưa mở. Trang tự gọi khi tải xong; để công khai cho trường hợp cần mở lại.
  Notifications.connect = connect;

  // ---------------------------------------------------------
  // Giao diện chuông (header khách hàng, cửa hàng, quản lý)
  // ---------------------------------------------------------

  // Hiện giờ ngày như máy chủ ghi, giống các trang khác. Không đổi sang "5 phút trước":
  // máy chủ trả giờ không kèm múi giờ, nên phép so với đồng hồ trình duyệt sẽ lệch khi hai bên khác múi giờ.
  function timeLabel(value) {
    var date = new Date(value);
    if (isNaN(date.getTime())) { return ''; }
    return date.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) { node.className = className; }
    if (text !== undefined) { node.textContent = text; }
    return node;
  }

  function initBell(root) {
    var bell = root.querySelector('[data-notify-bell]');
    var badge = root.querySelector('[data-notify-badge]');
    var listBox = root.querySelector('[data-notify-list]');
    var readAll = root.querySelector('[data-notify-read-all]');
    var more = root.querySelector('[data-notify-more]');
    var nextPage = 0;
    var loading = false;

    function note(message) {
      return el('p', 'notify-note', message);
    }

    // Tiêu đề và nội dung có thể chứa tên người dùng, tên cửa hàng: luôn gán bằng textContent.
    function itemNode(item) {
      var level = LEVELS[item.mucDo] || LEVELS.THONG_TIN;
      var node = el('button', 'notify-item ' + level.css + (item.daDoc ? '' : ' is-unread'));
      node.type = 'button';

      var icon = el('i', 'notify-item-icon bi ' + level.icon);
      icon.setAttribute('aria-hidden', 'true');

      var main = el('span', 'notify-item-main');
      main.appendChild(el('span', 'notify-item-title', item.tieuDe || 'Thông báo'));
      if (item.noiDung) { main.appendChild(el('span', 'notify-item-body', item.noiDung)); }

      var meta = el('span', 'notify-item-meta');
      if (level.label) { meta.appendChild(el('span', '', level.label)); }
      meta.appendChild(el('span', '', timeLabel(item.ngayTao)));
      if (!item.daDoc) {
        var unreadMark = el('span', 'notify-item-unread', 'Chưa đọc');
        unreadMark.setAttribute('data-notify-unread', '');
        meta.appendChild(unreadMark);
      }
      main.appendChild(meta);

      node.appendChild(icon);
      node.appendChild(main);
      node.addEventListener('click', function () { openItem(item, node); });
      return node;
    }

    // Bấm vào một thông báo: đánh dấu đã đọc rồi mở trang cần xem.
    function openItem(item, node) {
      var target = internalPath(item.duongDan);
      var go = function () { if (target) { Auth.navigate(target); } };

      if (item.daDoc) { go(); return; }

      Notifications.markRead(item.maThongBao).then(function () {
        item.daDoc = true;
        node.classList.remove('is-unread');
        var mark = node.querySelector('[data-notify-unread]');
        if (mark) { mark.parentNode.removeChild(mark); }
      }, function () { }).then(go);
    }

    // reset = true: tải lại từ trang đầu (mỗi lần mở chuông); false: nối thêm trang kế tiếp.
    function load(reset) {
      if (loading) { return; }
      loading = true;

      var page = reset ? 0 : nextPage;
      if (reset) {
        listBox.textContent = '';
        listBox.appendChild(note('Đang tải thông báo...'));
      }

      Notifications.list(page).then(function (data) {
        var items = (data && data.content) || [];
        if (reset) { listBox.textContent = ''; }
        if (reset && !items.length) { listBox.appendChild(note('Bạn chưa có thông báo nào.')); }
        items.forEach(function (item) { listBox.appendChild(itemNode(item)); });

        nextPage = page + 1;
        more.classList.toggle('d-none', !data || nextPage >= data.totalPages);
      }).catch(function (err) {
        if (Auth.handleAuthError(err)) { return; }
        if (reset) {
          listBox.textContent = '';
          listBox.appendChild(note('Không tải được thông báo. Hãy mở lại chuông để thử lại.'));
        }
      }).then(function () { loading = false; });
    }

    Notifications.onUnreadChange(function (count) {
      badge.textContent = count > 99 ? '99+' : String(count);
      badge.classList.toggle('d-none', count === 0);
      readAll.disabled = count === 0;
      bell.setAttribute('aria-label', count ? 'Thông báo, ' + count + ' chưa đọc' : 'Thông báo');
    });

    Notifications.onNew(function (item) {
      var target = internalPath(item.duongDan);
      OB.toast(item.tieuDe || 'Bạn có thông báo mới', 'info',
        target ? { duration: 6000, link: { text: 'Xem', href: target } } : { duration: 6000 });
      if (bell.getAttribute('aria-expanded') === 'true') { load(true); }
    });

    root.addEventListener('show.bs.dropdown', function () { load(true); });
    more.addEventListener('click', function () { load(false); });
    readAll.addEventListener('click', function () {
      Notifications.markAllRead().then(function () { load(true); }, function (err) {
        if (!Auth.handleAuthError(err)) { OB.toast(err.message, 'danger'); }
      });
    });

    Notifications.refreshUnread();
  }

  function init() {
    if (!Auth.isLoggedIn()) { return; }

    var root = document.querySelector('[data-notify]');
    if (root) { initBell(root); }

    connect();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
