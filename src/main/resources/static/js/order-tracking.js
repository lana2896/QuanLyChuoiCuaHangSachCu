/* Hiển thị trạng thái & hành trình đơn hàng, dùng chung cho trang khách hàng và trang cửa hàng. */
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || OB.tracking) { return; }
  var esc = OB.escapeHtml;

  // 7 trạng thái theo thứ tự luồng xử lý. Từ "Đang lấy hàng" trở đi do đơn vị vận chuyển cập nhật.
  var STATUSES = [
    { key: 'CHO_XU_LY', label: 'Chờ xử lý' },
    { key: 'DA_XAC_NHAN', label: 'Đã xác nhận' },
    { key: 'DANG_LAY_HANG', label: 'Đang lấy hàng' },
    { key: 'DA_LAY_HANG', label: 'Đã lấy hàng' },
    { key: 'DANG_GIAO', label: 'Đang giao' },
    { key: 'DA_GIAO', label: 'Đã giao' },
    { key: 'DA_HOAN_TRA', label: 'Đã hoàn trả' }
  ];

  var EXTRA_TEXT = {
    DA_HUY: 'Đã hủy',
    CHO_THANH_TOAN: 'Chờ thanh toán',
    DA_THANH_TOAN: 'Đã thanh toán',
    THANH_TOAN_MOT_PHAN: 'Thanh toán một phần',
    KHONG_THANH_TOAN: 'Không thu tiền',
    CHO_LAY_HANG: 'Chờ lấy hàng',
    GIAO_THAT_BAI: 'Giao thất bại'
  };

  var TEXT = {};
  STATUSES.forEach(function (s) { TEXT[s.key] = s.label; });
  Object.keys(EXTRA_TEXT).forEach(function (k) { TEXT[k] = EXTRA_TEXT[k]; });

  var TABS = [{ key: 'ALL', label: 'Tất cả' }]
    .concat(STATUSES.map(function (s) { return { key: s.key, label: s.label }; }))
    .concat([{ key: 'DA_HUY', label: 'Đã hủy' }]);

  var FINAL = { DA_GIAO: true, DA_HOAN_TRA: true, DA_HUY: true };

  function text(v) { return TEXT[v] || v || 'Không xác định'; }

  function pill(v) {
    var cls = '';
    if (v === 'DA_GIAO' || v === 'DA_THANH_TOAN') { cls = ' is-done'; }
    else if (v === 'DA_HUY' || v === 'DA_HOAN_TRA' || v === 'KHONG_THANH_TOAN' || v === 'GIAO_THAT_BAI') { cls = ' is-cancel'; }
    return '<span class="order-pill' + cls + '">' + esc(text(v)) + '</span>';
  }

  function isFinal(status) { return !!FINAL[status]; }

  function dateTime(v) { return v ? new Date(v).toLocaleString('vi-VN') : ''; }

  function itemCover(item) {
    var url = typeof item.hinhAnhUrl === 'string' ? item.hinhAnhUrl.trim() : '';
    var safe = /^https?:\/\//i.test(url) || (url.charAt(0) === '/' && url.charAt(1) !== '/');
    return '<div class="mini-cover c' + ((item.maSach || 0) % 6) + '">' +
      (safe ? '<img class="order-cover-image" src="' + esc(url) + '" alt="' + esc(item.tenSach || '') +
        '" loading="lazy" referrerpolicy="no-referrer">' : '') + '</div>';
  }

  function bindItemCovers(container) {
    // Image errors do not bubble; capture them even after the list is re-rendered.
    container.addEventListener('error', function (e) {
      var image = e.target;
      if (image && image.classList && image.classList.contains('order-cover-image')) {
        image.classList.add('d-none');
      }
    }, true);
  }

  /* Thanh tiến trình: 5 bước chung rồi "Đã giao" hoặc "Đã hoàn trả" tùy kết quả. */
  function progress(status) {
    if (status === 'DA_HUY') { return ''; }
    var returned = status === 'DA_HOAN_TRA';
    var flow = STATUSES.slice(0, 5).concat([returned ? STATUSES[6] : STATUSES[5]]);
    var idx = -1;
    flow.forEach(function (s, i) { if (s.key === status) { idx = i; } });
    var finished = status === 'DA_GIAO' || returned;

    return '<div class="order-progress">' + flow.map(function (st, i) {
      var cls = (i < idx || finished) ? 'done' : (i === idx ? 'current' : '');
      if (returned && i === flow.length - 1) { cls += ' returned'; }
      return '<div class="op-step ' + cls + '">' + esc(st.label) + '</div>';
    }).join('') + '</div>';
  }

  /* Dòng thông tin vận chuyển ngắn gọn. */
  function shipInfo(store) {
    var parts = [];
    var vc = store.vanChuyen;
    if (store.tenDonViVanChuyen) {
      parts.push('<i class="bi bi-truck me-1"></i>' + esc(store.tenDonViVanChuyen) +
        (store.phiShipCH != null ? ' · Phí ' + OB.formatMoney(store.phiShipCH) : ''));
    }
    if (vc && vc.maVanDon) { parts.push('Mã vận đơn: <code>' + esc(vc.maVanDon) + '</code>'); }
    if (store.ghiChu) { parts.push('<i class="bi bi-chat-left-text me-1"></i>Ghi chú: ' + esc(store.ghiChu)); }

    var html = parts.length ? '<div class="order-ship-info">' + parts.join(' · ') + '</div>' : '';

    if (vc && vc.trangThaiVanChuyen === 'GIAO_THAT_BAI' && store.trangThaiDonHang === 'DANG_GIAO') {
      html += '<div class="order-ship-warn"><i class="bi bi-exclamation-triangle me-1"></i>Giao hàng không thành công' +
        (vc.soLanGiaoThatBai ? ' (lần ' + vc.soLanGiaoThatBai + ')' : '') + '. Đơn vị vận chuyển đang xử lý tiếp.</div>';
    }
    if (vc && vc.ghiChuGiaoHang && !isFinal(store.trangThaiDonHang)) {
      html += '<div class="order-ship-info">Ghi chú giao hàng: ' + esc(vc.ghiChuGiaoHang) + '</div>';
    }
    if (store.trangThaiDonHang === 'CHO_XU_LY') {
      html += '<div class="order-ship-info">Đang chờ cửa hàng xác nhận, sau đó đơn sẽ được chuyển cho đơn vị vận chuyển.</div>';
    }
    return html;
  }

  /* Hành trình chi tiết do đơn vị vận chuyển báo về (mới nhất ở trên). */
  var openTimelines = {};
  function timeline(store) {
    var shipEvents = (store.vanChuyen && store.vanChuyen.lichSu) || [];
    var events = [];

    if (store.ngayTao) {
      events.push({ thoiGian: store.ngayTao, ten: 'Chờ xử lý', ghiChu: 'Đơn hàng đã được tạo, chờ cửa hàng xác nhận' });
    }
    if (store.ngayXacNhan) {
      events.push({ thoiGian: store.ngayXacNhan, ten: 'Đã xác nhận', ghiChu: 'Chủ cửa hàng đã xác nhận đơn' });
    }
	var carrierAccepted = false;
	shipEvents.forEach(function (e) {
	  // CHO_LAY_HANG chỉ là lúc cửa hàng bàn giao đơn, đơn vị vận chuyển chưa tiếp nhận -> không hiển thị.
	  if (e.trangThai === 'CHO_LAY_HANG') { return; }

	  if (!carrierAccepted) {
	    carrierAccepted = true;
	    events.push({
	      thoiGian: e.thoiGian,
	      ten: 'Đã tiếp nhận',
	      ghiChu: 'Đơn vị vận chuyển ' + (store.tenDonViVanChuyen || '') + ' đã tiếp nhận đơn'
	    });
	  }
	  events.push({ thoiGian: e.thoiGian, ten: e.tenTrangThai || text(e.trangThai), ghiChu: e.trangThai === 'DANG_LAY_HANG' ? '' : e.ghiChu });
	});
    if (!events.length) { return ''; }

    var items = events.slice().reverse().map(function (e) {
		return '<li><span class="tl-time">' + esc(dateTime(e.thoiGian)) + '</span>' +
		  '<span class="tl-note">' + esc(e.ghiChu || e.ten) + '</span></li>';
    }).join('');
    return '<details class="order-timeline" data-key="' + esc(store.maDHCH) + '"' +
      (openTimelines[store.maDHCH] ? ' open' : '') + '><summary>Hành trình đơn hàng (' + events.length + ')</summary>' +
      '<ul>' + items + '</ul></details>';
  }

  /* Ghi nhớ hộp hành trình đang mở để không bị đóng lại khi tự làm mới. */
  function bindTimelineMemory(container) {
    container.addEventListener('toggle', function (e) {
      var d = e.target;
      if (d && d.classList && d.classList.contains('order-timeline')) {
        openTimelines[d.getAttribute('data-key')] = d.open;
      }
    }, true);
  }

  function renderTabs(container, current) {
    container.innerHTML = TABS.map(function (t) {
      return '<button type="button" class="order-tab' + (t.key === current ? ' active' : '') +
        '" data-tab="' + t.key + '">' + esc(t.label) + '</button>';
    }).join('');
  }

  /* Tự làm mới định kỳ khi còn đơn đang xử lý và tab đang hiển thị. */
  function autoRefresh(refresh, hasActive, intervalMs) {
    setInterval(function () {
      if (!document.hidden && hasActive()) { refresh(); }
    }, intervalMs || 30000);
  }

  OB.tracking = {
    STATUSES: STATUSES,
    TABS: TABS,
    text: text,
    pill: pill,
    isFinal: isFinal,
    dateTime: dateTime,
    itemCover: itemCover,
    bindItemCovers: bindItemCovers,
    progress: progress,
    shipInfo: shipInfo,
    timeline: timeline,
    bindTimelineMemory: bindTimelineMemory,
    renderTabs: renderTabs,
    autoRefresh: autoRefresh
  };
})();
