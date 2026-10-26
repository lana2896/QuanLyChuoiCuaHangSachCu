// Trang Tổng quan của cổng Quản lý: 5 thẻ KPI bấm được, hàng đợi 5 việc mới nhất, dòng tiền tháng, đối tác vận chuyển.
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('ovContent')) { return; }

  var Auth = OB.auth;
  var requestId = 0;

  function $(id) { return document.getElementById(id); }
  function esc(value) { return OB.escapeHtml(value === null || value === undefined ? '' : value); }
  function money(value) { return OB.formatMoney(value); }

  function dateTime(value) {
    var m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(String(value || ''));
    return m ? m[4] + ':' + m[5] + ' ' + m[3] + '/' + m[2] : '';
  }

  function kpi(href, label, value, note, alert) {
    return '<a class="mg-kpi' + (alert ? ' is-alert' : '') + '" href="' + esc(href) + '" data-shell-link>'
      + '<span class="mg-kpi-label">' + esc(label) + '</span>'
      + '<span class="mg-kpi-value d-block">' + esc(value) + '</span>'
      + '<span class="mg-kpi-note">' + esc(note) + '</span></a>';
  }

  function render(d) {
    var choDuyet = d.choDuyetCuaHang + d.choDuyetSach;
    $('ovKpis').innerHTML =
      kpi('/store-moderation', 'Hồ sơ chờ duyệt', choDuyet, d.choDuyetCuaHang + ' cửa hàng · ' + d.choDuyetSach + ' sách', false)
      + kpi('/management/payments?tab=refunds', 'Hoàn tiền chờ xử lý', d.hoanTienCho, d.hoanTienChoKhachKhai + ' chờ khách khai tài khoản', false)
      + kpi('/management/payments?tab=payouts', 'Cửa hàng chờ chi', d.cuaHangChoChi, money(d.tongTienChoChi) + ' · ' + d.khoanChoChi + ' đơn', false)
      + kpi('/management/commissions', 'Hoa hồng tháng này', money(d.hoaHongThangNay), 'Đơn giao thành công, tính theo ngày giao', false)
      + kpi('/management/payments?tab=transactions', 'Giao dịch lệch', d.giaoDichLech, 'Trong 30 ngày gần đây', d.giaoDichLech > 0);

    var queue = d.hangDoi || [];
    $('ovQueue').innerHTML = queue.map(function (v) {
      return '<li><div><div class="mg-queue-title">' + esc(v.tieuDe) + '</div>'
        + '<div class="mg-queue-sub">' + esc(v.moTa) + ' · ' + esc(dateTime(v.thoiGian)) + '</div></div>'
        + '<a class="btn btn-sm btn-accent flex-shrink-0" href="' + esc(v.duongDan) + '" data-shell-link>' + esc(v.nut) + '</a></li>';
    }).join('');
    $('ovQueueEmpty').classList.toggle('d-none', queue.length > 0);

    var f = d.dongTien || {};
    $('ovFlow').innerHTML = [
      ['Khách đã trả vào', f.khachDaTra], ['Đã hoàn cho khách', f.daHoan],
      ['Đã chi cửa hàng', f.daChiCuaHang], ['Hoa hồng', f.hoaHong]
    ].map(function (r) {
      return '<dt class="col-7 fw-normal text-muted">' + esc(r[0]) + '</dt><dd class="col-5 text-end mb-1 fw-semibold">' + esc(money(r[1])) + '</dd>';
    }).join('');

    var carriers = d.vanChuyen || [];
    $('ovCarriers').innerHTML = carriers.map(function (c) {
      var rate = c.tongDon ? Math.round(c.giaoThanhCong * 1000 / c.tongDon) / 10 : 0;
      return '<li><div><div class="mg-queue-title">' + esc(c.tenDonVi) + '</div>'
        + '<div class="mg-queue-sub">' + c.tongDon + ' đơn kết thúc · ' + c.hoanTra + ' hoàn trả</div></div>'
        + '<span class="mg-pill ' + (rate >= 90 ? 'is-ok' : rate >= 70 ? 'is-wait' : 'is-bad') + '">' + rate + '% thành công</span></li>';
    }).join('');
    $('ovCarriersEmpty').classList.toggle('d-none', carriers.length > 0);

    if (OB.managerShell) { OB.managerShell.touch(); }
  }

  function load() {
    var id = ++requestId;
    $('ovError').classList.add('d-none');
    OB.request('GET', '/api/management/overview').then(function (data) {
      if (id === requestId && data) { render(data); }
    }, function (err) {
      if (id !== requestId) { return; }
      if (Auth.handleAuthError && Auth.handleAuthError(err)) { return; }
      $('ovErrorText').textContent = err.message || 'Không tải được số liệu.';
      $('ovError').classList.remove('d-none');
    });
  }

  function init() {
    if (!Auth.isLoggedIn()) { return; }
    var user = Auth.getUser();
    if (!user || user.vaiTro !== 'QUAN_LY') { $('ovDenied').classList.remove('d-none'); return; }
    $('ovContent').classList.remove('d-none');
    $('ovRetry').addEventListener('click', load);
    load();
    if (OB.managerRealtime) { OB.managerRealtime.on('overview', load); }
  }

  init();
})();
