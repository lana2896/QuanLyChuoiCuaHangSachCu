(function () {
  'use strict';

  // Trang không cần đăng nhập: hãng được xác định bởi mã trên URL (/shipping/{maCode}),
  // server đưa mã vào data-ma-code. Mọi API đều đi qua /api/shipping/{maCode}/...
  var page = document.querySelector('[data-shipping-page]');
  var MA_CODE = page ? page.getAttribute('data-ma-code') : null;
  var API = '/api/shipping/' + encodeURIComponent(MA_CODE || '');
  var REFRESH_MS = 30000;

  var shipments = [];
  var pending = [];
  var pendingView = { page: 0, size: 10, tracking: '', order: '' };
  var shipmentView = { page: 0, size: 10, filter: '', tracking: '', order: '' };

  // Tìm theo mã vận đơn: không phân biệt hoa thường, khớp một phần (ví dụ "ghn0001").
  function matchTracking(x, keyword) {
    if (!keyword) return true;
    var code = (x.maVanDon || (x.maVanChuyen ? 'VC-' + x.maVanChuyen : '')).toLowerCase();
    return code.indexOf(keyword.toLowerCase()) >= 0;
  }

  // Tìm theo mã đơn hàng: nhận "12", "#12", "DHCH12" hoặc "#DHCH12"; khớp mã đơn của cửa hàng (DHCH) hoặc mã đơn tổng.
  function matchOrder(x, keyword) {
    if (!keyword) return true;
    var text = keyword.trim().toUpperCase().replace(/^#/, '');
    var onlyStoreOrder = text.indexOf('DHCH') === 0;
    var digits = text.replace(/^DHCH/, '').replace(/^#/, '');
    if (!/^\d+$/.test(digits)) return false;
    if (String(x.maDHCH) === digits) return true;
    return !onlyStoreOrder && String(x.maDH) === digits;
  }

  function applySearch(list, view) {
    return list.filter(function (x) { return matchTracking(x, view.tracking) && matchOrder(x, view.order); });
  }

  // Không gửi token đăng nhập (nếu trình duyệt đang lưu) vì trang này không dùng tài khoản.
  function api(method, path, body) {
    return OldBook.request(method, API + path, body, { token: null });
  }

  function esc(v) { return OldBook.escapeHtml(v == null ? '' : v); }
  function money(v) { return OldBook.formatMoney(v); }
  function date(v) { return v ? new Date(v).toLocaleString('vi-VN') : '-'; }

  function showAlert(message) {
    var a = document.getElementById('accessAlert');
    a.classList.remove('d-none');
    a.textContent = message;
  }

  // Tên hiển thị khớp TrangThaiVanChuyen / TrangThaiDonHang ở server.
  // Bước chuyển hợp lệ KHÔNG giữ ở đây: server trả về trong trangThaiTiepTheo của từng vận đơn.
  var LABELS = {
    CHO_LAY_HANG: 'Chờ lấy hàng',
    DANG_LAY_HANG: 'Đang lấy hàng',
    DA_LAY_HANG: 'Đã lấy hàng',
    DANG_GIAO: 'Đang giao',
    DA_GIAO: 'Đã giao',
    GIAO_THAT_BAI: 'Giao thất bại',
    DA_HOAN_TRA: 'Đã hoàn trả',
    DA_HUY: 'Đã hủy'
  };
  var ORDER_LABELS = { DA_XAC_NHAN: 'Đã xác nhận', DANG_LAY_HANG: 'Đang lấy hàng', DA_LAY_HANG: 'Đã lấy hàng',
    DANG_GIAO: 'Đang giao', DA_GIAO: 'Đã giao', DA_HOAN_TRA: 'Đã hoàn trả', DA_HUY: 'Đã hủy' };
  var PAYMENT_LABELS = { CHO_THANH_TOAN: 'Thu hộ (COD)', DA_THANH_TOAN: 'Đã thanh toán',
    THANH_TOAN_MOT_PHAN: 'Thanh toán một phần', KHONG_THANH_TOAN: 'Không thanh toán' };
  var DEFAULT_MAX_ATTEMPTS = 3;

  function maxAttempts(x) { return (x && x.soLanGiaoToiDa) || DEFAULT_MAX_ATTEMPTS; }
  function failCount(x) { return (x && x.soLanGiaoThatBai) || 0; }

  // Gợi ý theo trạng thái được chọn, tính theo số lần giao thất bại của vận đơn đang mở.
  function hintFor(status, x) {
    var max = maxAttempts(x);
    var fails = failCount(x);
    if (status === 'DA_GIAO') {
      return 'Xác nhận đã giao và thu tiền COD. Hệ thống sẽ chốt doanh thu cho cửa hàng.';
    }
    if (status === 'GIAO_THAT_BAI') {
      var lan = fails + 1;
      return lan >= max
        ? 'Đây là lần giao thất bại thứ ' + lan + '/' + max + ': hệ thống sẽ TỰ ĐỘNG HOÀN TRẢ hàng về cửa hàng (cộng lại tồn kho, không thu tiền khách).'
        : 'Ghi nhận giao thất bại lần ' + lan + '/' + max + '. Sau đó chọn "Giao lại" để giao tiếp; thất bại lần thứ ' + max + ' sẽ tự động hoàn trả.';
    }
    if (status === 'DANG_GIAO' && x && x.trangThaiVanChuyen === 'GIAO_THAT_BAI') {
      return 'Giao lại lần ' + (fails + 1) + '/' + max + ' (đã thất bại ' + fails + ' lần).';
    }
    return '';
  }

  // Tên lựa chọn trong hộp thoại: DANG_GIAO sau khi giao thất bại hiển thị là "Giao lại".
  function optionLabel(status, x) {
    if (status === 'DANG_GIAO' && x && x.trangThaiVanChuyen === 'GIAO_THAT_BAI') {
      return 'Giao lại (lần ' + (failCount(x) + 1) + '/' + maxAttempts(x) + ')';
    }
    return LABELS[status] || status;
  }
  var IN_TRANSIT = ['DANG_LAY_HANG', 'DA_LAY_HANG', 'DANG_GIAO', 'GIAO_THAT_BAI'];

  function statusBadge(status) {
    var cls = {
      DA_GIAO: 'text-bg-success',
      DA_HOAN_TRA: 'text-bg-warning',
      GIAO_THAT_BAI: 'text-bg-danger',
      DANG_GIAO: 'text-bg-primary',
      DANG_LAY_HANG: 'text-bg-info',
      DA_LAY_HANG: 'text-bg-info',
      DA_HUY: 'text-bg-dark'
    }[status] || 'text-bg-secondary';
    return '<span class="badge ' + cls + '">' + esc(LABELS[status] || status || 'Chưa cập nhật') + '</span>';
  }

  function loadPending() {
    return api('GET', '/requests/pending').then(function (data) {
      pending = data || [];
      renderPending();
      updateStats();
    });
  }

  function loadShipments() {
    return api('GET', '/requests').then(function (data) {
      shipments = data || [];
      renderShipments();
      updateStats();
    });
  }

  function loadAll(silent) {
    return Promise.all([loadPending(), loadShipments()])
      .catch(function (e) { if (!silent) OldBook.toast(e.message, 'danger'); });
  }

  // Đơn được cửa hàng xác nhận sẽ tự xuất hiện: làm mới định kỳ khi tab đang mở và không có hộp thoại nào đang mở.
  function startAutoRefresh() {
    window.setInterval(function () {
      if (document.hidden) return;
      if (document.querySelector('.modal.show')) return;
      loadAll(true);
    }, REFRESH_MS);
    document.addEventListener('visibilitychange', function () {
      if (!document.hidden && !document.querySelector('.modal.show')) loadAll(true);
    });
  }

  function updateStats() {
    function count(list) { return shipments.filter(function (x) { return list.indexOf(x.trangThaiVanChuyen) >= 0; }).length; }
    document.getElementById('statPending').textContent = pending.length;
    document.getElementById('statShipping').textContent = count(IN_TRANSIT);
    document.getElementById('statDelivered').textContent = count(['DA_GIAO']);
    document.getElementById('statReturned').textContent = count(['DA_HOAN_TRA']);
  }

  function paginate(list, view, prefix, unit) {
    var pages = Math.max(1, Math.ceil(list.length / view.size));
    if (view.page >= pages) { view.page = pages - 1; }
    var start = view.page * view.size;
    var end = Math.min(start + view.size, list.length);
    document.getElementById(prefix + 'Count').textContent = list.length ? (start + 1) + '–' + end + ' / ' + list.length + ' ' + unit : '0 ' + unit;
    document.getElementById(prefix + 'PageLabel').textContent = 'Trang ' + (view.page + 1) + ' / ' + pages;
    document.getElementById(prefix + 'Previous').disabled = view.page === 0;
    document.getElementById(prefix + 'Next').disabled = view.page >= pages - 1;
    return list.slice(start, end);
  }

  function renderPending() {
    var body = document.getElementById('pendingBody');
    var rows = applySearch(pending, pendingView);
    var pageRows = paginate(rows, pendingView, 'pending', 'yêu cầu');
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">' +
        (pending.length ? 'Không tìm thấy yêu cầu khớp mã đã nhập.' : 'Không có yêu cầu chờ tiếp nhận.') + '</td></tr>';
      return;
    }
    body.innerHTML = pageRows.map(function (x) {
      return '<tr>' +
        '<td><strong>#DHCH' + esc(x.maDHCH) + '</strong><div class="small text-muted">Đơn #' + esc(x.maDH) + '</div></td>' +
        '<td class="small">' + esc(x.maVanDon || '-') + '</td>' +
        '<td><span class="badge text-bg-light border">' + esc(ORDER_LABELS[x.trangThaiDonHang] || x.trangThaiDonHang) + '</span></td>' +
        '<td>' + esc(PAYMENT_LABELS[x.trangThaiThanhToan] || x.trangThaiThanhToan || '-') + '</td>' +
        '<td>' + money(x.tongTien) + '</td>' +
        '<td class="small">' + date(x.ngayCapNhat) + '</td>' +
        '<td class="text-end"><button class="btn btn-accent btn-sm" onclick="Shipping.openAccept(' + x.maDHCH + ')"><i class="bi bi-box-arrow-in-right me-1"></i>Tiếp nhận</button></td>' +
      '</tr>';
    }).join('');
  }

  function renderShipments() {
    var body = document.getElementById('shipmentBody');
    // Đơn CHO_LAY_HANG nằm ở tab Tiếp nhận yêu cầu; đơn DA_HUY không hiển thị với đơn vị vận chuyển.
    var rows = shipments.filter(function (x) { return x.trangThaiVanChuyen !== 'CHO_LAY_HANG' && x.trangThaiVanChuyen !== 'DA_HUY'; });
    var total = rows.length;
    if (shipmentView.filter) {
      rows = rows.filter(function (x) { return x.trangThaiVanChuyen === shipmentView.filter; });
    }
    var searching = !!(shipmentView.tracking || shipmentView.order);
    rows = applySearch(rows, shipmentView);
    var pageRows = paginate(rows, shipmentView, 'shipment', 'vận đơn');
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">' + (!total ? 'Chưa có vận đơn.' : (searching ? 'Không tìm thấy vận đơn khớp mã đã nhập.' : 'Không có vận đơn ở trạng thái này.')) + '</td></tr>';
      return;
    }
    body.innerHTML = pageRows.map(function (x) {
      var canUpdate = (x.trangThaiTiepTheo || []).length > 0;
      return '<tr>' +
        '<td><strong>' + esc(x.maVanDon || ('VC-' + x.maVanChuyen)) + '</strong></td>' +
        '<td>#DHCH' + esc(x.maDHCH) + '<div class="small text-muted">Đơn #' + esc(x.maDH) + '</div></td>' +
        '<td>' + statusBadge(x.trangThaiVanChuyen) + '</td>' +
        '<td>' + esc(failCount(x) + '/' + maxAttempts(x)) + '</td>' +
        '<td class="small">' + date(x.ngayCapNhat) + '</td>' +
        '<td class="text-end">' + (canUpdate
          ? '<button class="btn btn-outline-secondary btn-sm" onclick="Shipping.openStatus(' + x.maVanChuyen + ')"><i class="bi bi-pencil-square me-1"></i>Cập nhật</button>'
          : '<span class="small text-muted">Đã kết thúc</span>') + '</td>' +
      '</tr>';
    }).join('');
  }

  function openAccept(orderId) {
    var x = pending.find(function (p) { return p.maDHCH === orderId; });
    if (!x) return;
    document.getElementById('acceptOrderId').value = orderId;
    document.getElementById('acceptOrderLabel').value = '#DHCH' + orderId + ' / Đơn #' + x.maDH;
    document.getElementById('acceptTracking').value = x.maVanDon || '';
    document.getElementById('acceptNote').value = '';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('acceptModal')).show();
  }

  function accept() {
    var body = {
      maDHCH: Number(document.getElementById('acceptOrderId').value),
      ghiChuGiaoHang: document.getElementById('acceptNote').value || null
    };
    if (!body.maDHCH) return;
    api('POST', '/requests', body).then(function () {
      bootstrap.Modal.getInstance(document.getElementById('acceptModal')).hide();
      OldBook.toast('Đã tiếp nhận #DHCH' + body.maDHCH + ', đơn chuyển sang Đang lấy hàng.', 'success');
      loadAll();
    }).catch(function (e) { OldBook.toast(e.message, 'danger'); });
  }

  function currentShipment() {
    var id = Number(document.getElementById('statusId').value);
    return shipments.find(function (s) { return s.maVanChuyen === id; });
  }

  function onStatusChange() {
    var v = document.getElementById('statusValue').value;
    var hint = document.getElementById('statusHint');
    var text = hintFor(v, currentShipment());
    hint.textContent = text;
    hint.classList.toggle('d-none', !text);
  }

  function openStatus(id) {
    var x = shipments.find(function (s) { return s.maVanChuyen === id; });
    if (!x) return;
    var allowed = x.trangThaiTiepTheo || [];
    if (!allowed.length) {
      OldBook.toast('Vận đơn đã kết thúc, không thể cập nhật thêm.', 'warning');
      return;
    }
    document.getElementById('statusId').value = id;
    document.getElementById('statusValue').innerHTML = allowed.map(function (st) {
      return '<option value="' + st + '">' + esc(optionLabel(st, x)) + '</option>';
    }).join('');
    document.getElementById('statusNote').value = '';
    onStatusChange();
    bootstrap.Modal.getOrCreateInstance(document.getElementById('statusModal')).show();
  }

  function updateStatus() {
    var status = document.getElementById('statusValue').value;
    var body = { trangThai: status, ghiChu: document.getElementById('statusNote').value.trim() || null };
    var id = document.getElementById('statusId').value;
    api('PATCH', '/requests/' + encodeURIComponent(id) + '/status', body).then(function (res) {
      bootstrap.Modal.getInstance(document.getElementById('statusModal')).hide();
      // Server có thể tự chuyển tiếp (ví dụ giao thất bại hết lượt -> hoàn trả), nên báo theo kết quả thật.
      var now = res && res.trangThaiVanChuyen ? res.trangThaiVanChuyen : status;
      if (now === 'DA_HOAN_TRA' && status === 'GIAO_THAT_BAI') {
        OldBook.toast('Giao thất bại lần ' + failCount(res) + '/' + maxAttempts(res) + ': đơn đã tự động chuyển hoàn trả về cửa hàng.', 'warning');
      } else if (now === 'GIAO_THAT_BAI') {
        OldBook.toast('Đã ghi nhận giao thất bại lần ' + failCount(res) + '/' + maxAttempts(res) + '. Hãy cập nhật "Giao lại" khi đi giao tiếp.', 'warning');
      } else {
        OldBook.toast('Đã cập nhật: ' + (LABELS[now] || now) + '.', 'success');
      }
      loadAll();
    }).catch(function (e) { OldBook.toast(e.message, 'danger'); });
  }

  // Ô tìm mã vận đơn / mã đơn hàng: lọc ngay trên danh sách đã tải, gõ xong 250ms mới lọc.
  function bindSearch(prefix, view, render) {
    var timer = null;
    function update() {
      view.tracking = document.getElementById(prefix + 'SearchTracking').value.trim();
      view.order = document.getElementById(prefix + 'SearchOrder').value.trim();
      view.page = 0;
      render();
    }
    ['SearchTracking', 'SearchOrder'].forEach(function (suffix) {
      document.getElementById(prefix + suffix).addEventListener('input', function () {
        window.clearTimeout(timer);
        timer = window.setTimeout(update, 250);
      });
    });
    document.getElementById(prefix + 'SearchClear').addEventListener('click', function () {
      document.getElementById(prefix + 'SearchTracking').value = '';
      document.getElementById(prefix + 'SearchOrder').value = '';
      update();
    });
  }

  function bindPaging(prefix, view, render) {
    document.getElementById(prefix + 'PageSize').addEventListener('change', function () {
      view.size = parseInt(this.value, 10) || 10;
      view.page = 0;
      render();
    });
    document.getElementById(prefix + 'Previous').addEventListener('click', function () { view.page -= 1; render(); });
    document.getElementById(prefix + 'Next').addEventListener('click', function () { view.page += 1; render(); });
  }

  window.Shipping = {
    loadAll: loadAll,
    loadShipments: loadShipments,
    openAccept: openAccept,
    onStatusChange: onStatusChange,
    accept: accept,
    openStatus: openStatus,
    updateStatus: updateStatus
  };

  document.addEventListener('DOMContentLoaded', function () {
    if (!MA_CODE) {
      showAlert('Liên kết thiếu mã đơn vị vận chuyển.');
      return;
    }
    bindPaging('pending', pendingView, renderPending);
    bindPaging('shipment', shipmentView, renderShipments);
    document.getElementById('shipmentFilter').addEventListener('change', function () {
      shipmentView.filter = this.value;
      shipmentView.page = 0;
      renderShipments();
    });
    bindSearch('pending', pendingView, renderPending);
    bindSearch('shipment', shipmentView, renderShipments);
    loadAll();
    startAutoRefresh();
  });
})();
