// Trang Thống kê vận hành của Quản lý: xem để hành động. Không có nút xuất CSV (xuất là việc của Admin).
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('stContent')) { return; }

  var Auth = OB.auth;
  var BASE = '/api/management/stats';
  var range = '30d';
  var requestId = 0;
  var busy = false;

  var BEN_HUY = { KHACH_HANG: 'Khách hàng', CHU_CUA_HANG: 'Cửa hàng', HE_THONG: 'Hệ thống', KHAC: 'Không rõ' };

  function $(id) { return document.getElementById(id); }
  function esc(value) { return OB.escapeHtml(value === null || value === undefined ? '' : value); }
  function money(value) { return OB.formatMoney(value || 0); }
  function pct(value) { return (value === null || value === undefined ? '—' : String(value) + '%'); }
  function hours(value) { return value === null || value === undefined ? '—' : value + ' giờ'; }
  function pad(n) { return (n < 10 ? '0' : '') + n; }
  function iso(date) { return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()); }
  function vn(isoDate) {
    var p = String(isoDate || '').split('-');
    return p.length === 3 ? p[2] + '/' + p[1] + '/' + p[0] : '';
  }

  // Ngày bắt đầu và kết thúc (yyyy-MM-dd) theo lựa chọn kỳ.
  function periodOf(name) {
    var today = new Date();
    var from = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    if (name === '7d') { from.setDate(from.getDate() - 6); }
    else if (name === '30d') { from.setDate(from.getDate() - 29); }
    else if (name === 'month') { from.setDate(1); }
    else if (name === 'quarter') { from = new Date(today.getFullYear(), Math.floor(today.getMonth() / 3) * 3, 1); }
    else { return { tu: $('stFrom').value, den: $('stTo').value }; }
    return { tu: iso(from), den: iso(today) };
  }

  function kpi(label, value, note, alert) {
    return '<div class="mg-kpi' + (alert ? ' is-alert' : '') + '"><span class="mg-kpi-label">' + esc(label) + '</span>'
      + '<span class="mg-kpi-value d-block">' + esc(value) + '</span><span class="mg-kpi-note">' + esc(note || '') + '</span></div>';
  }

  function render(d) {
    $('stPeriod').textContent = vn(d.tuNgay) + ' – ' + vn(d.denNgay);
    var f = d.dongTien;
    $('stFlow').innerHTML = kpi('Khách đã trả vào', money(f.khachDaTra), 'Giao dịch khớp và chờ hoàn')
      + kpi('Đã hoàn khách', money(f.daHoan)) + kpi('Đã chi cửa hàng', money(f.daChiCuaHang))
      + kpi('Hoa hồng', money(f.hoaHong), 'Đơn giao thành công, tính theo ngày giao')
      + kpi('Sàn đang giữ', money(f.sanDangGiu), 'Sau khi trừ phí ship ' + money(f.phiShipDonDaGiao));
    $('stMismatch').classList.toggle('d-none', !f.giaoDichLech);
    $('stMismatchText').textContent = f.giaoDichLech + ' giao dịch ngân hàng bị lệch trong kỳ, cần xử lý.';

    $('stStores').innerHTML = d.cuaHang.map(function (c) {
      return '<tr><td><strong>' + esc(c.tenCuaHang) + '</strong><br><span class="small text-muted">' + c.tongDon + ' đơn đặt</span></td>'
        + '<td class="text-end">' + esc(money(c.doanhThu)) + '</td><td class="text-end">' + c.soDonHoanTat + '</td>'
        + '<td class="text-end">' + c.soDonHuyBoiShop + ' <span class="small text-muted">(' + esc(pct(c.tyLeHuyBoiShop)) + ')</span></td>'
        + '<td class="text-end">' + esc(hours(c.gioXacNhanTrungBinh)) + '</td>'
        + '<td class="text-end">' + (c.diemDanhGia === null ? '—' : esc(c.diemDanhGia) + ' ★ <span class="small text-muted">(' + c.soDanhGia + ')</span>') + '</td></tr>';
    }).join('');
    $('stStoresEmpty').classList.toggle('d-none', d.cuaHang.length > 0);

    // Gom đơn quá hạn theo cửa hàng để mỗi cửa hàng chỉ có một nút nhắc.
    var byStore = {};
    d.donQuaHan.forEach(function (o) {
      (byStore[o.maCH] = byStore[o.maCH] || { ten: o.tenCuaHang, don: [] }).don.push(o);
    });
    var late = Object.keys(byStore).map(function (maCH) {
      var g = byStore[maCH];
      var oldest = g.don.reduce(function (m, o) { return Math.max(m, o.gioChoXacNhan); }, 0);
      return '<li><div><div class="mg-queue-title">' + esc(g.ten) + '</div><div class="mg-queue-sub">'
        + g.don.length + ' đơn · lâu nhất ' + oldest + ' giờ</div></div>'
        + '<button type="button" class="btn btn-sm btn-accent flex-shrink-0" data-remind="' + maCH + '">Gửi nhắc</button></li>';
    });
    $('stLate').innerHTML = late.join('');
    $('stLateEmpty').classList.toggle('d-none', late.length > 0);

    $('stCarriers').innerHTML = d.vanChuyen.map(function (v) {
      return '<tr><td><strong>' + esc(v.tenDonVi) + '</strong></td><td class="text-end">' + v.soDon + '</td>'
        + '<td class="text-end">' + v.giaoThanhCong + ' <span class="small text-muted">(' + esc(pct(v.tyLeThanhCong)) + ')</span></td>'
        + '<td class="text-end">' + v.hoanTra + ' <span class="small text-muted">(' + esc(pct(v.tyLeHoan)) + ')</span></td>'
        + '<td class="text-end">' + esc(hours(v.gioGiaoTrungBinh)) + '</td></tr>';
    }).join('');
    $('stCarriersEmpty').classList.toggle('d-none', d.vanChuyen.length > 0);

    var h = d.huyHoan;
    $('stCancelBy').innerHTML = h.huyTheoBen.length ? h.huyTheoBen.map(function (b) {
      return '<li><span>' + esc(BEN_HUY[b.ben] || b.ben) + '</span><strong>' + b.soDon + ' đơn</strong></li>';
    }).join('') : '<li class="text-muted">Không có đơn hủy trong kỳ.</li>';
    $('stRefundWeek').innerHTML = h.hoanTheoTuan.length ? h.hoanTheoTuan.map(function (w) {
      return '<li><span>Tuần từ ' + esc(vn(w.tuNgay)) + '</span><strong>' + esc(money(w.soTien)) + ' · ' + w.soKhoan + ' khoản</strong></li>';
    }).join('') : '<li class="text-muted">Chưa hoàn khoản nào trong kỳ.</li>';
    $('stRefundTime').textContent = 'Thời gian xử lý hoàn trung bình: ' + hours(h.gioXuLyHoanTrungBinh);

    $('stCategories').innerHTML = d.danhMuc.map(function (c) {
      return '<tr><td><strong>' + esc(c.tenDanhMuc) + '</strong></td><td class="text-end">' + esc(money(c.doanhThu)) + '</td>'
        + '<td class="text-end">' + esc(money(c.hoaHong)) + '</td><td class="text-end">' + esc(c.tyLeDangApDung) + '%</td></tr>';
    }).join('');

    var k = d.kiemDuyet;
    $('stModeration').innerHTML = '<dt class="col-7 fw-normal text-muted">Cửa hàng chờ duyệt</dt><dd class="col-5 text-end">' + k.cuaHangCho + '</dd>'
      + '<dt class="col-7 fw-normal text-muted">Sách chờ duyệt</dt><dd class="col-5 text-end">' + k.sachCho + '</dd>'
      + '<dt class="col-7 fw-normal text-muted">Tỉ lệ từ chối</dt><dd class="col-5 text-end">' + esc(pct(k.tyLeTuChoi)) + '</dd>';

    if (OB.managerShell) { OB.managerShell.touch(); }
  }

  function load() {
    var p = periodOf(range);
    if (range === 'custom' && (!p.tu || !p.den)) { return; }
    var id = ++requestId;
    $('stError').classList.add('d-none');
    OB.request('GET', BASE + '?tuNgay=' + encodeURIComponent(p.tu) + '&denNgay=' + encodeURIComponent(p.den)).then(function (data) {
      if (id === requestId && data) { render(data); }
    }, function (err) {
      if (id !== requestId || (Auth.handleAuthError && Auth.handleAuthError(err))) { return; }
      $('stErrorText').textContent = err.message || 'Không tải được thống kê.';
      $('stError').classList.remove('d-none');
    });
  }

  function remind(maCH) {
    if (busy) { return; }
    busy = true;
    OB.request('POST', BASE + '/stores/' + encodeURIComponent(maCH) + '/remind-confirm').then(function () {
      OB.toast('Đã gửi thông báo nhắc cửa hàng xác nhận đơn', 'success');
    }, function (err) {
      OB.toast(err.message, 'danger');
    }).then(function () { busy = false; });
  }

  function init() {
    if (!Auth.isLoggedIn()) { return; }
    var user = Auth.getUser();
    if (!user || user.vaiTro !== 'QUAN_LY') { $('stDenied').classList.remove('d-none'); return; }
    $('stContent').classList.remove('d-none');

    var today = new Date();
    $('stTo').value = iso(today);
    var start = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 29);
    $('stFrom').value = iso(start);

    document.querySelectorAll('[data-range]').forEach(function (button) {
      button.addEventListener('click', function () {
        range = button.getAttribute('data-range');
        document.querySelectorAll('[data-range]').forEach(function (b) { b.classList.toggle('active', b === button); });
        $('stCustom').classList.toggle('d-none', range !== 'custom');
        if (range !== 'custom') { load(); }
      });
    });
    $('stApply').addEventListener('click', load);
    $('stRetry').addEventListener('click', load);
    $('stLate').addEventListener('click', function (event) {
      var button = event.target.closest('[data-remind]');
      if (button) { remind(button.getAttribute('data-remind')); }
    });

    load();
    if (OB.managerRealtime) { OB.managerRealtime.on('overview', load); }
  }

  init();
})();
