/*
 * Cập nhật màn hình Quản lý theo thời gian thực, dùng lại kênh /ws/notifications của notifications.js.
 *
 * File này giữ MỘT listener duy nhất trên kênh đẩy rồi tự chia tin cho trang đang mở, vì notifications.js
 * không có hàm gỡ listener: nếu mỗi trang tự đăng ký thì chuyển trang nhiều lần sẽ cộng dồn listener.
 *
 * Trang đăng ký phần mình quan tâm: OB.managerRealtime.on('payouts', tải_lại). Khi chuyển mục (manager:leave)
 * mọi đăng ký bị xóa. Các tin đến trong 1 giây được gom thành một lần gọi cho mỗi phần.
 *
 * Phần (phan): overview, moderation, payouts, refunds, transactions, commissions.
 */
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || OB.managerRealtime) { return; }

  var DEBOUNCE_MS = 1000;
  var ALL = ['overview', 'moderation', 'payouts', 'refunds', 'transactions', 'commissions'];

  // loại thông báo (thong_bao.loai) -> các phần cần tải lại
  var BY_NOTIFICATION = {
    GIAN_HANG_CHO_DUYET: ['moderation', 'overview'],
    SACH_CHO_DUYET: ['moderation', 'overview'],
    CAN_HOAN_TIEN: ['refunds', 'overview'],
    CAN_CHI_CUA_HANG: ['payouts', 'overview'],
    GIAO_DICH_LECH: ['transactions', 'overview']
  };

  var handlers = {};
  var pending = {};
  var timer = null;

  function push(phans) {
    phans.forEach(function (phan) { pending[phan] = true; });
    if (!timer) { timer = window.setTimeout(flush, DEBOUNCE_MS); }
  }

  function run(phan) {
    (handlers[phan] || []).slice().forEach(function (fn) {
      try { fn(); } catch (e) { }
    });
  }

  function flush() {
    timer = null;
    var phans = Object.keys(pending);
    pending = {};
    if (!phans.length) { return; }
    refreshBadges();
    phans.forEach(run);
  }

  // Huy hiệu ở sidebar: số việc chờ ở Kiểm duyệt và Thanh toán.
  function refreshBadges() {
    if (!OB.auth.isLoggedIn()) { return Promise.resolve(); }
    return OB.request('GET', '/api/management/overview/badges').then(function (data) {
      setBadge('moderation', data && data.kiemDuyet);
      setBadge('payments', data && data.thanhToan);
    }, function () { });
  }

  function setBadge(name, count) {
    var nodes = document.querySelectorAll('[data-badge="' + name + '"]');
    for (var i = 0; i < nodes.length; i++) {
      var n = Number(count) || 0;
      nodes[i].textContent = n > 99 ? '99+' : String(n);
      nodes[i].classList.toggle('d-none', n === 0);
    }
  }

  function onMessage(tin) {
    if (!tin) { return; }
    if (tin.resync) { push(ALL); return; }
    if (tin.type === 'DU_LIEU_THAY_DOI') {
      var phans = BY_NOTIFICATION[tin.loai];
      if (phans) { push(phans); }
      return;
    }
    if (tin.type === 'VAN_HANH_THAY_DOI' && tin.phan) {
      push([tin.phan, 'overview']);
    }
  }

  var api = {
    // Đăng ký tải lại cho một phần. Tự bị xóa khi chuyển mục.
    on: function (phan, fn) {
      (handlers[phan] = handlers[phan] || []).push(fn);
    },
    // Nút làm mới trên thanh trên: tải lại mọi phần đang đăng ký.
    refreshAll: function () {
      refreshBadges();
      Object.keys(handlers).forEach(run);
    },
    refreshBadges: refreshBadges
  };
  OB.managerRealtime = api;

  document.addEventListener('manager:leave', function () {
    handlers = {};
    pending = {};
    if (timer) { window.clearTimeout(timer); timer = null; }
  });

  if (OB.notifications) {
    OB.notifications.onChange(onMessage);
    refreshBadges();
  }
})();
