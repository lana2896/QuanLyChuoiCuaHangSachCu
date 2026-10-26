// Trang quét mã VietQR. Trang chỉ hiển thị và hỏi trạng thái khi máy chủ báo phiên đổi (WebSocket);
// việc xác nhận tiền về do máy chủ quyết định.
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('payPage')) { return; }

  var Auth = OB.auth;
  var Banks = window.OldBookBanks;
  var API = '/api/payment/sessions/';
  var CHANGE_TYPE = 'PHIEN_THANH_TOAN_DOI';
  var SELECTED_KEY = 'ob_checkout_items';

  var maPhien = new URLSearchParams(window.location.search).get('maPhien');
  var remaining = 0;
  var countdownTimer = null;
  var finished = false;

  function $(id) { return document.getElementById(id); }
  function show(id) { $(id).classList.remove('d-none'); }
  function hide(id) { $(id).classList.add('d-none'); }

  function clock(seconds) {
    var m = Math.floor(seconds / 60);
    var s = seconds % 60;
    return (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
  }

  function stopTimers() {
    if (countdownTimer) { window.clearInterval(countdownTimer); countdownTimer = null; }
  }

  function message(icon, title, text, primary) {
    hide('payLoading');
    hide('payContent');
    $('payMessageIcon').className = 'bi ' + icon + ' fs-1';
    $('payMessageTitle').textContent = title;
    $('payMessageText').textContent = text;
    if (primary) {
      $('payMessagePrimary').textContent = primary.label;
      $('payMessagePrimary').setAttribute('href', primary.href);
    }
    show('payMessage');
  }

  function renderWaiting(session) {
    hide('payLoading');
    hide('payMessage');
    $('payQr').setAttribute('src', session.qrUrl || '');
    $('payBank').textContent = Banks.name(session.maNganHang);
    $('payAccount').textContent = session.soTaiKhoan || '';
    $('payAccountName').textContent = session.tenTaiKhoan || '';
    $('payAmount').textContent = OB.formatMoney(session.soTien);
    $('payAmount').setAttribute('data-raw', String(Math.round(Number(session.soTien) || 0)));
    $('payCode').textContent = session.maThanhToan || '';
    $('payCountdown').textContent = clock(remaining);
    show('payContent');
  }

  function apply(session) {
    if (finished) { return; }

    if (session.trangThai === 'DA_THANH_TOAN' && session.maDH) {
      finished = true;
      stopTimers();
      // Các dòng đã mua không còn trong giỏ, nên bỏ luôn lựa chọn đã lưu của trang thanh toán.
      try { window.sessionStorage.removeItem(SELECTED_KEY); } catch (e) { }
      window.location.href = '/order-success?maDH=' + encodeURIComponent(session.maDH);
      return;
    }

    if (session.trangThai === 'CAN_HOAN_TIEN') {
      finished = true;
      stopTimers();
      message('bi-arrow-counterclockwise', 'Đã nhận tiền nhưng chưa tạo được đơn hàng',
        'Số tiền, giá sách hoặc giỏ hàng không còn khớp với mã thanh toán. Sàn sẽ hoàn lại toàn bộ số tiền bạn đã chuyển.',
        { label: 'Nhập tài khoản nhận hoàn tiền', href: '/payment/refunds' });
      return;
    }

    if (session.trangThai === 'DA_HUY') {
      message('bi-x-circle', 'Bạn đã hủy lần thanh toán này',
        'Sách vẫn nằm trong giỏ hàng. Nếu bạn đã chuyển khoản trước khi hủy, đơn hàng sẽ được tạo khi tiền về hoặc tiền sẽ được hoàn lại.');
      stopTimers();
      return;
    }

    remaining = Math.max(0, Number(session.conLaiGiay) || 0);
    if (session.trangThai === 'HET_HAN' || remaining === 0) {
      message('bi-hourglass-bottom', 'Mã thanh toán đã hết hạn',
        'Sách không còn được giữ cho bạn. Nếu bạn đã chuyển khoản, đừng chuyển lại: đơn hàng sẽ được tạo khi tiền về, hoặc tiền sẽ được hoàn lại.');
      stopTimers();
      return;
    }

    renderWaiting(session);
  }

  function poll() {
    if (finished) { return; }
    OB.request('GET', API + encodeURIComponent(maPhien)).then(apply, function () {
      // Mất mạng thoáng qua thì lần hỏi sau thử lại; không đổi màn hình vì một lần lỗi.
    });
  }

  function tick() {
    if (finished || remaining <= 0) { return; }
    remaining -= 1;
    $('payCountdown').textContent = clock(remaining);
    if (remaining === 0) { poll(); }
  }

  function copy(button) {
    var target = $(button.getAttribute('data-copy'));
    var value = button.hasAttribute('data-copy-digits') ? target.getAttribute('data-raw') : target.textContent;
    var clipboard = window.navigator && window.navigator.clipboard;
    if (!clipboard || !value) { return; }
    clipboard.writeText(value).then(function () {
      OB.toast('Đã chép ' + value, 'success');
    }, function () { });
  }

  function cancel() {
    OB.confirm('Hủy lần thanh toán này? Sách vẫn nằm trong giỏ hàng nhưng không còn được giữ cho bạn.', {
      confirmText: 'Hủy thanh toán'
    }).then(function (ok) {
      if (!ok) { return; }
      $('payCancel').disabled = true;
      OB.request('DELETE', API + encodeURIComponent(maPhien)).then(apply, function (err) {
        $('payCancel').disabled = false;
        OB.toast(err.message, 'error');
      });
    });
  }

  function init() {
    // Chưa đăng nhập: auth.js đang chuyển về trang đăng nhập (body có data-page-requires-login).
    if (!Auth.isLoggedIn()) { return; }
    if (!maPhien) {
      message('bi-question-circle', 'Không tìm thấy mã thanh toán', 'Hãy quay lại giỏ hàng và bấm thanh toán lại.');
      return;
    }

    document.querySelectorAll('[data-copy]').forEach(function (button) {
      button.addEventListener('click', function () { copy(button); });
    });
    $('payCancel').addEventListener('click', cancel);

    // Tiền chuyển sát giờ có thể về muộn và đơn vẫn được tạo, nên trang vẫn nghe tin sau khi mã hết hạn.
    // Nối lại sau khi rớt kết nối (resync) cũng hỏi lại, vì có thể đã lỡ tin.
    if (OB.notifications && OB.notifications.onChange) {
      OB.notifications.onChange(function (tin) {
        if (tin.resync || (tin.type === CHANGE_TYPE && String(tin.maPhien) === String(maPhien))) { poll(); }
      });
    }

    OB.request('GET', API + encodeURIComponent(maPhien)).then(function (session) {
      apply(session);
      if (finished) { return; }
      countdownTimer = window.setInterval(tick, 1000);
    }, function (err) {
      message('bi-exclamation-triangle', 'Không tải được mã thanh toán', err.message);
    });
  }

  init();
})();
