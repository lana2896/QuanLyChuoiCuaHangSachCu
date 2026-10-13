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
    DA_HOAN_TRA: 'Đã hoàn trả'
  };
  var ORDER_LABELS = { DA_XAC_NHAN: 'Đã xác nhận', DANG_LAY_HANG: 'Đang lấy hàng', DA_LAY_HANG: 'Đã lấy hàng',
    DANG_GIAO: 'Đang giao', DA_GIAO: 'Đã giao', DA_HOAN_TRA: 'Đã hoàn trả', DA_HUY: 'Đã hủy' };
  var PAYMENT_LABELS = { CHO_THANH_TOAN: 'Thu hộ (COD)', DA_THANH_TOAN: 'Đã thanh toán',
    THANH_TOAN_MOT_PHAN: 'Thanh toán một phần', KHONG_THANH_TOAN: 'Không thanh toán' };
  var HINTS = {
    DA_GIAO: 'Xác nhận đã giao và thu tiền COD. Hệ thống sẽ chốt doanh thu cho cửa hàng.',
    GIAO_THAT_BAI: 'Hết số lần giao cho phép thì đơn tự chuyển sang Đã hoàn trả.',
    DA_HOAN_TRA: 'Hàng trả về cửa hàng, tồn kho được cộng lại, không thu tiền khách.'
  };
  var IN_TRANSIT = ['DANG_LAY_HANG', 'DA_LAY_HANG', 'DANG_GIAO', 'GIAO_THAT_BAI'];

  function statusBadge(status) {
    var cls = {
      DA_GIAO: 'text-bg-success',
      DA_HOAN_TRA: 'text-bg-warning',
      GIAO_THAT_BAI: 'text-bg-danger',
      DANG_GIAO: 'text-bg-primary',
      DANG_LAY_HANG: 'text-bg-info',
      DA_LAY_HANG: 'text-bg-info'
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

  function renderPending() {
    var body = document.getElementById('pendingBody');
    if (!pending.length) {
      body.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Không có yêu cầu chờ tiếp nhận.</td></tr>';
      return;
    }
    body.innerHTML = pending.map(function (x) {
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
    // Đơn CHO_LAY_HANG nằm ở tab Tiếp nhận yêu cầu.
    var rows = shipments.filter(function (x) { return x.trangThaiVanChuyen !== 'CHO_LAY_HANG'; });
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Chưa có vận đơn.</td></tr>';
      return;
    }
    body.innerHTML = rows.map(function (x) {
      var canUpdate = (x.trangThaiTiepTheo || []).length > 0;
      return '<tr>' +
        '<td><strong>' + esc(x.maVanDon || ('VC-' + x.maVanChuyen)) + '</strong></td>' +
        '<td>#DHCH' + esc(x.maDHCH) + '<div class="small text-muted">Đơn #' + esc(x.maDH) + '</div></td>' +
        '<td>' + statusBadge(x.trangThaiVanChuyen) + '</td>' +
        '<td>' + esc(x.soLanGiaoThatBai || 0) + '</td>' +
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

  function onStatusChange() {
    var v = document.getElementById('statusValue').value;
    var hint = document.getElementById('statusHint');
    hint.textContent = HINTS[v] || '';
    hint.classList.toggle('d-none', !HINTS[v]);
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
      return '<option value="' + st + '">' + esc(LABELS[st]) + '</option>';
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
      OldBook.toast('Đã cập nhật: ' + (LABELS[now] || now) + '.', 'success');
      loadAll();
    }).catch(function (e) { OldBook.toast(e.message, 'danger'); });
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
    loadAll();
    startAutoRefresh();
  });
})();
