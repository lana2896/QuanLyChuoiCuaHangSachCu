(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('carriersPage')) { return; }

  var Auth = OB.auth;
  var ROLES = ['QUAN_LY', 'QUAN_TRI_VIEN'];
  var BASE = '/api/management/shipping-carriers';
  var ACTIVE = 'HOAT_DONG';
  var PAUSED = 'NGUNG_HOAT_DONG';
  var MODALS = ['carrierFormModal', 'carrierDetailModal', 'carrierLinkModal', 'carrierStatusModal'];
  var statusLabels = { HOAT_DONG: 'Đang hợp tác', NGUNG_HOAT_DONG: 'Tạm dừng' };

  var carriers = [];
  var page = 0;
  var loading = false;
  var busy = false;
  var allowModalClose = false;
  var editing = null;
  var selectedStatus = null;
  var selectedDetail = null;
  var filters = { tuKhoa: '', trangThai: '' };
  var modalOpen = {};
  var afterHide = {};

  function $(id) { return document.getElementById(id); }
  function show(id) { $(id).classList.remove('d-none'); }
  function hide(id) { $(id).classList.add('d-none'); }
  function esc(value) { return OB.escapeHtml(value); }
  function blank(value) { return value === null || value === undefined || value === ''; }
  function text(id, value) { $(id).textContent = blank(value) ? '—' : String(value); }
  function trimmed(id) { return $(id).value.trim(); }
  function optional(id) { return trimmed(id) || null; }
  function numberValue(id) { var v = trimmed(id); return v === '' ? null : Number(v); }
  function modal(id) { return window.bootstrap.Modal.getOrCreateInstance($(id), { backdrop: 'static' }); }
  function closeModal(id) {
    allowModalClose = true;
    modal(id).hide();
    allowModalClose = false;
  }
  function whenHidden(id, callback) {
    if (!modalOpen[id]) { callback(); return; }
    (afterHide[id] = afterHide[id] || []).push(callback);
  }
  function switchModal(fromId, callback) {
    whenHidden(fromId, callback);
    modal(fromId).hide();
  }
  function error(id, message) { $(id).textContent = message; show(id); }
  function pageSize() { return parseInt($('carriersPageSize').value, 10) || 10; }
  function pageCount() { return Math.max(1, Math.ceil(carriers.length / pageSize())); }
  function isActive(carrier) { return carrier.trangThai === ACTIVE; }
  function find(code) {
    return carriers.find(function (c) { return c.maCode === code; }) || null;
  }
  function hasAccess() {
    var user = Auth.getUser();
    return !!user && ROLES.indexOf(user.vaiTro) >= 0;
  }
  function absolute(url, path) {
    if (url) { return url; }
    return path ? window.location.origin + path : '';
  }

  function formatDate(value) {
    if (blank(value)) { return null; }
    var match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value));
    if (match) { return match[3] + '/' + match[2] + '/' + match[1]; }
    var date = new Date(value);
    return isNaN(date.getTime()) ? String(value) : date.toLocaleString('vi-VN');
  }

  function statusBadge(carrier) {
    return '<span class="badge ' + (isActive(carrier) ? 'carrier-badge-active' : 'carrier-badge-paused') + '">' +
      esc(statusLabels[carrier.trangThai] || carrier.trangThai) + '</span>';
  }

  function syncControls() {
    var controls = document.querySelectorAll('#carriersPage [data-carrier-control]');
    for (var i = 0; i < controls.length; i++) { controls[i].disabled = busy || loading; }
    var forms = document.querySelectorAll('#carrierFormModal button, #carrierFormModal input, #carrierFormModal textarea, #carrierStatusModal button, #carrierStatusModal textarea');
    for (var j = 0; j < forms.length; j++) {
      forms[j].disabled = busy || (forms[j].id === 'carrierCode' && !!editing);
    }
    $('carriersPrevious').disabled = busy || loading || page === 0;
    $('carriersNext').disabled = busy || loading || page + 1 >= pageCount();
  }

  function denied() { hide('carriersContent'); show('carriersDenied'); }

  function handleAuthError(err) {
    if (Auth.handleAuthError(err)) { return true; }
    if (err.status === 403) { denied(); return true; }
    return false;
  }

  function renderStats(stats) {
    stats = stats || {};
    text('statTotal', stats.tongDoiTac || 0);
    text('statActive', stats.dangHopTac || 0);
    text('statPaused', stats.tamDung || 0);
    text('statShipping', stats.vanDonDangXuLy || 0);
  }

  function rowHtml(carrier) {
    var code = esc(carrier.maCode);
    var active = isActive(carrier);
    var contact = [];
    if (carrier.soDienThoai) { contact.push('<div><i class="bi bi-telephone me-1 text-muted" aria-hidden="true"></i>' + esc(carrier.soDienThoai) + '</div>'); }
    if (carrier.email) { contact.push('<div class="text-break"><i class="bi bi-envelope me-1 text-muted" aria-hidden="true"></i>' + esc(carrier.email) + '</div>'); }
    return '<tr data-carrier-code="' + code + '">' +
      '<td><span class="carrier-code">' + code + '</span></td>' +
      '<td><div class="fw-semibold text-break">' + esc(carrier.tenDonVi) + '</div>' +
        '<div class="small text-muted text-break">' + (carrier.nguoiLienHe ? esc(carrier.nguoiLienHe) : 'Chưa có người liên hệ') + '</div></td>' +
      '<td class="small">' + (contact.length ? contact.join('') : '<span class="text-muted">—</span>') + '</td>' +
      '<td><div>' + esc(OB.formatMoney(carrier.phiVanChuyen)) + '</div><div class="small text-muted">' + esc(carrier.thoiGianGiaoDuKien) + ' ngày</div></td>' +
      '<td><div>' + esc(carrier.soVanDonDangXuLy || 0) + ' đang xử lý</div><div class="small text-muted">' + esc(carrier.soVanDon || 0) + ' tổng</div></td>' +
      '<td>' + statusBadge(carrier) + '</td>' +
      '<td class="text-end"><div class="d-flex flex-wrap justify-content-end gap-1">' +
        '<button type="button" class="btn btn-outline-secondary btn-sm" data-carrier-action="detail" data-carrier-control>Xem</button>' +
        '<button type="button" class="btn btn-outline-secondary btn-sm" data-carrier-action="edit" data-carrier-control>Cập nhật</button>' +
        '<button type="button" class="btn btn-outline-brown btn-sm" data-carrier-action="link" data-carrier-control title="Đường link cho đối tác"><i class="bi bi-link-45deg" aria-hidden="true"></i> Link</button>' +
        '<button type="button" class="btn ' + (active ? 'btn-outline-danger' : 'btn-accent') + ' btn-sm" data-carrier-action="status" data-carrier-control>' +
          (active ? 'Tạm dừng' : 'Kích hoạt') + '</button>' +
      '</div></td></tr>';
  }

  function render() {
    page = Math.min(page, pageCount() - 1);
    hide('carriersLoading');
    if (!carriers.length) { hide('carriersResults'); show('carriersEmpty'); syncControls(); return; }
    hide('carriersEmpty');
    var start = page * pageSize();
    $('carriersRows').innerHTML = carriers.slice(start, start + pageSize()).map(rowHtml).join('');
    text('carriersCount', (start + 1) + '–' + Math.min(start + pageSize(), carriers.length) + ' / ' + carriers.length + ' đơn vị');
    text('carriersPageLabel', 'Trang ' + (page + 1) + ' / ' + pageCount());
    show('carriersResults');
    syncControls();
  }

  function loadCarriers(resetPage) {
    if (loading) { return Promise.resolve(); }
    if (resetPage) { page = 0; }
    hide('carriersError'); hide('carriersEmpty'); hide('carriersResults'); show('carriersLoading');
    loading = true;
    syncControls();
    var params = new URLSearchParams();
    Object.keys(filters).forEach(function (key) { if (filters[key]) { params.set(key, filters[key]); } });
    var query = params.toString();
    return OB.request('GET', BASE + (query ? '?' + query : '')).then(function (data) {
      data = data || {};
      carriers = Array.isArray(data.danhSach) ? data.danhSach : [];
      renderStats(data.thongKe);
      render();
    }).catch(function (err) {
      hide('carriersLoading');
      if (!handleAuthError(err)) { text('carriersErrorText', err.message); show('carriersError'); }
    }).then(function () { loading = false; syncControls(); });
  }

  function mutate(form, errorId, modalId, action, message, afterSuccess) {
    if (busy || loading || !form.reportValidity()) { return; }
    hide(errorId);
    var submit = form.querySelector('[type="submit"]');
    var submitLabel = submit.textContent;
    submit.textContent = 'Đang xử lý...';
    form.setAttribute('aria-busy', 'true');
    busy = true;
    syncControls();
    action().then(function (data) {
      closeModal(modalId);
      OB.toast(message, 'success');
      busy = false;
      return loadCarriers(false).then(function () {
        if (afterSuccess) { whenHidden(modalId, function () { afterSuccess(data); }); }
      });
    }).catch(function (err) {
      if (handleAuthError(err)) { closeModal(modalId); return; }
      error(errorId, err.message);
    }).then(function () {
      busy = false;
      submit.textContent = submitLabel;
      form.setAttribute('aria-busy', 'false');
      syncControls();
    });
  }

  function fillForm(carrier) {
    var c = carrier || {};
    $('carrierCode').value = c.maCode || '';
    $('carrierName').value = c.tenDonVi || '';
    $('carrierFee').value = blank(c.phiVanChuyen) ? '' : c.phiVanChuyen;
    $('carrierDays').value = blank(c.thoiGianGiaoDuKien) ? '' : c.thoiGianGiaoDuKien;
    $('carrierContact').value = c.nguoiLienHe || '';
    $('carrierPhone').value = c.soDienThoai || '';
    $('carrierEmail').value = c.email || '';
    $('carrierWebsite').value = c.website || '';
    $('carrierAddress').value = c.diaChi || '';
    $('carrierNote').value = c.ghiChu || '';
    $('carrierIntegration').value = c.thongTinTichHop || '';
  }

  function openForm(carrier) {
    editing = carrier || null;
    hide('carrierFormError');
    fillForm(editing);
    $('carrierCode').required = !editing;
    text('carrierFormTitle', editing ? 'Cập nhật đơn vị ' + editing.maCode : 'Thêm đơn vị vận chuyển');
    text('carrierCodeHelp', editing ? 'Mã đã gắn với đường link của đối tác nên không thể thay đổi.' : 'Dùng trong đường link, không đổi được sau khi tạo.');
    text('carrierFormSubmit', editing ? 'Lưu thay đổi' : 'Thêm đơn vị');
    syncControls();
    modal('carrierFormModal').show();
  }

  function formBody() {
    return {
      tenDonVi: trimmed('carrierName'),
      phiVanChuyen: numberValue('carrierFee'),
      thoiGianGiaoDuKien: numberValue('carrierDays'),
      nguoiLienHe: optional('carrierContact'),
      soDienThoai: optional('carrierPhone'),
      email: optional('carrierEmail'),
      website: optional('carrierWebsite'),
      diaChi: optional('carrierAddress'),
      ghiChu: optional('carrierNote'),
      thongTinTichHop: optional('carrierIntegration')
    };
  }

  function showDetail(carrier) {
    selectedDetail = carrier;
    text('detailCode', carrier.maCode);
    text('detailName', carrier.tenDonVi);
    text('detailFee', OB.formatMoney(carrier.phiVanChuyen));
    text('detailDays', blank(carrier.thoiGianGiaoDuKien) ? null : carrier.thoiGianGiaoDuKien + ' ngày');
    $('detailStatus').innerHTML = statusBadge(carrier);
    text('detailPauseReason', isActive(carrier) ? null : carrier.lyDoTamDung);
    text('detailStatusAt', formatDate(carrier.ngayCapNhatTrangThai));
    text('detailCreated', formatDate(carrier.ngayTao));
    text('detailUpdated', formatDate(carrier.ngayCapNhat));
    text('detailContact', carrier.nguoiLienHe);
    text('detailPhone', carrier.soDienThoai);
    text('detailEmail', carrier.email);
    text('detailAddress', carrier.diaChi);
    if (carrier.website && /^https?:\/\//i.test(carrier.website)) {
      $('detailWebsite').innerHTML = '<a href="' + esc(carrier.website) + '" target="_blank" rel="noopener noreferrer">' + esc(carrier.website) + '</a>';
    } else {
      text('detailWebsite', carrier.website);
    }
    text('detailShipTotal', carrier.soVanDon || 0);
    text('detailShipActive', carrier.soVanDonDangXuLy || 0);
    text('detailShipDone', carrier.soVanDonDaGiao || 0);
    text('detailNote', carrier.ghiChu);
    modal('carrierDetailModal').show();
  }

  function mailtoHref(carrier, pageUrl, apiUrl) {
    var subject = 'OldBook - Đường link làm việc cho ' + carrier.tenDonVi;
    var body = 'Xin chào ' + (carrier.nguoiLienHe || carrier.tenDonVi) + ',\n\n' +
      'OldBook Marketplace gửi quý đối tác đường link để tiếp nhận và cập nhật trạng thái đơn hàng:\n' +
      pageUrl + '\n\n' +
      'Địa chỉ API dành cho hệ thống của quý đối tác:\n' + apiUrl + '\n\n' +
      'Mã đơn vị: ' + carrier.maCode + '\n' +
      'Vui lòng không chia sẻ đường link này cho bên thứ ba.\n\nTrân trọng,\nOldBook Marketplace';
    return 'mailto:' + encodeURIComponent(carrier.email) + '?subject=' + encodeURIComponent(subject) + '&body=' + encodeURIComponent(body);
  }

  function showLink(carrier) {
    var pageUrl = absolute(carrier.duongDanTrangDayDu, carrier.duongDanTrang);
    var apiUrl = absolute(carrier.duongDanApiDayDu, carrier.duongDanApi);
    var active = isActive(carrier);
    text('linkCarrierName', carrier.tenDonVi + ' (' + carrier.maCode + ')');
    $('linkCarrierStatus').className = 'badge ms-1 ' + (active ? 'carrier-badge-active' : 'carrier-badge-paused');
    text('linkCarrierStatus', statusLabels[carrier.trangThai] || carrier.trangThai);
    $('linkPausedWarning').classList.toggle('d-none', active);
    $('linkPageUrl').value = pageUrl;
    $('linkApiUrl').value = apiUrl;
    $('linkPageOpen').setAttribute('href', pageUrl || '#');
    var mail = $('linkMailButton');
    if (carrier.email) {
      mail.setAttribute('href', mailtoHref(carrier, pageUrl, apiUrl));
      mail.classList.remove('disabled');
      mail.removeAttribute('aria-disabled');
      text('linkMailInfo', 'Gửi tới ' + carrier.email);
    } else {
      mail.setAttribute('href', '#');
      mail.classList.add('disabled');
      mail.setAttribute('aria-disabled', 'true');
      text('linkMailInfo', 'Đối tác chưa có email liên hệ. Cập nhật email để gửi link.');
    }
    modal('carrierLinkModal').show();
  }

  function copyValue(inputId) {
    var input = $(inputId);
    var value = input.value;
    if (!value) { return; }
    var done = function () { OB.toast('Đã sao chép đường link.', 'success'); };
    var fallback = function () {
      try {
        input.focus();
        if (input.select) { input.select(); }
        if (document.execCommand && document.execCommand('copy')) { done(); return; }
      } catch (e) { }
      OB.toast('Không sao chép tự động được, vui lòng chọn và sao chép thủ công.', 'warning');
    };
    var nav = window.navigator;
    if (nav && nav.clipboard && nav.clipboard.writeText) {
      nav.clipboard.writeText(value).then(done, fallback);
    } else {
      fallback();
    }
  }

  function updateReasonCount() { $('carrierPauseReasonCount').textContent = String($('carrierPauseReason').value.length); }

  function showStatus(carrier) {
    selectedStatus = carrier;
    var pausing = isActive(carrier);
    hide('carrierStatusError');
    text('carrierStatusTarget', carrier.maCode + ' — ' + carrier.tenDonVi);
    text('carrierStatusTitle', pausing ? 'Tạm dừng hợp tác' : 'Kích hoạt hợp tác');
    text('carrierStatusSubmit', pausing ? 'Tạm dừng hợp tác' : 'Kích hoạt lại');
    $('carrierStatusSubmit').className = 'btn ' + (pausing ? 'btn-outline-danger' : 'btn-accent');
    text('carrierStatusDescription', pausing
      ? 'Khách hàng sẽ không chọn được đơn vị này khi đặt hàng và đường link của đối tác sẽ tạm khóa.'
      : 'Đơn vị sẽ xuất hiện lại khi khách đặt hàng và đường link của đối tác hoạt động trở lại.');
    var activeShipments = carrier.soVanDonDangXuLy || 0;
    if (pausing && activeShipments > 0) {
      text('carrierStatusWarning', 'Đối tác đang có ' + activeShipments + ' vận đơn chưa hoàn tất. Trong thời gian tạm dừng, đối tác không cập nhật được các vận đơn này.');
      show('carrierStatusWarning');
    } else {
      hide('carrierStatusWarning');
    }
    $('carrierPauseReasonGroup').classList.toggle('d-none', !pausing);
    $('carrierPauseReason').value = '';
    $('carrierPauseReason').required = pausing;
    updateReasonCount();
    modal('carrierStatusModal').show();
  }

  function bindModalTracking() {
    MODALS.forEach(function (id) {
      var element = $(id);
      var opened = function () { modalOpen[id] = true; };
      element.addEventListener('show.bs.modal', opened);
      element.addEventListener('shown.bs.modal', opened);
      element.addEventListener('hidden.bs.modal', function () {
        modalOpen[id] = false;
        var queue = afterHide[id] || [];
        afterHide[id] = [];
        queue.forEach(function (callback) { callback(); });
      });
    });
    ['carrierFormModal', 'carrierStatusModal'].forEach(function (id) {
      $(id).addEventListener('hide.bs.modal', function (event) {
        if (busy && !allowModalClose) { event.preventDefault(); }
      });
    });
  }

  function bindEvents() {
    bindModalTracking();

    $('carriersFilters').addEventListener('submit', function (event) {
      event.preventDefault();
      if (busy || loading) { return; }
      filters = { tuKhoa: trimmed('carrierKeyword'), trangThai: $('carrierStatusFilter').value };
      loadCarriers(true);
    });
    $('resetCarrierFilters').addEventListener('click', function () {
      if (busy || loading) { return; }
      $('carriersFilters').reset();
      filters = { tuKhoa: '', trangThai: '' };
      loadCarriers(true);
    });
    $('retryCarriers').addEventListener('click', function () { if (!busy && !loading) { loadCarriers(false); } });
    $('carriersPageSize').addEventListener('change', function () { if (!busy && !loading) { page = 0; render(); } });
    $('carriersPrevious').addEventListener('click', function () { if (!busy && !loading && page > 0) { page--; render(); } });
    $('carriersNext').addEventListener('click', function () { if (!busy && !loading && page + 1 < pageCount()) { page++; render(); } });

    $('carriersRows').addEventListener('click', function (event) {
      var button = event.target.closest('[data-carrier-action]');
      if (!button || button.disabled || busy || loading) { return; }
      var carrier = find(button.closest('tr').getAttribute('data-carrier-code'));
      if (!carrier) { return; }
      var action = button.getAttribute('data-carrier-action');
      if (action === 'detail') { showDetail(carrier); }
      if (action === 'edit') { openForm(carrier); }
      if (action === 'link') { showLink(carrier); }
      if (action === 'status') { showStatus(carrier); }
    });

    $('createCarrierButton').addEventListener('click', function () {
      if (busy || loading) { return; }
      openForm(null);
    });

    $('carrierCode').addEventListener('input', function () {
      var upper = this.value.toUpperCase();
      if (upper !== this.value) { this.value = upper; }
    });

    $('carrierForm').addEventListener('submit', function (event) {
      event.preventDefault();
      var body = formBody();
      if (!body.tenDonVi) { error('carrierFormError', 'Vui lòng nhập tên đơn vị vận chuyển.'); return; }
      if (editing) {
        var code = editing.maCode;
        mutate(this, 'carrierFormError', 'carrierFormModal', function () {
          return OB.request('PUT', BASE + '/' + encodeURIComponent(code), body);
        }, 'Đã cập nhật thông tin đơn vị vận chuyển.');
        return;
      }
      body.maCode = trimmed('carrierCode').toUpperCase();
      mutate(this, 'carrierFormError', 'carrierFormModal', function () {
        return OB.request('POST', BASE, body);
      }, 'Đã thêm đơn vị vận chuyển. Hãy gửi đường link cho đối tác.', function (created) {
        if (created && created.maCode) { showLink(find(created.maCode) || created); }
      });
    });

    $('carrierStatusForm').addEventListener('submit', function (event) {
      event.preventDefault();
      if (!selectedStatus) { return; }
      var carrier = selectedStatus;
      var pausing = isActive(carrier);
      var reason = trimmed('carrierPauseReason');
      if (pausing && !reason) { error('carrierStatusError', 'Vui lòng nhập lý do tạm dừng hợp tác.'); return; }
      var body = pausing ? { trangThai: PAUSED, lyDo: reason } : { trangThai: ACTIVE };
      mutate(this, 'carrierStatusError', 'carrierStatusModal', function () {
        return OB.request('PATCH', BASE + '/' + encodeURIComponent(carrier.maCode) + '/status', body);
      }, pausing ? 'Đã tạm dừng hợp tác với ' + carrier.tenDonVi + '.' : 'Đã kích hoạt lại ' + carrier.tenDonVi + '.');
    });

    $('carrierPauseReason').addEventListener('input', updateReasonCount);

    var copyButtons = document.querySelectorAll('#carrierLinkModal [data-copy-target]');
    for (var i = 0; i < copyButtons.length; i++) {
      copyButtons[i].addEventListener('click', function () { copyValue(this.getAttribute('data-copy-target')); });
    }

    $('linkMailButton').addEventListener('click', function (event) {
      if (this.classList.contains('disabled')) { event.preventDefault(); }
    });

    $('detailLinkButton').addEventListener('click', function () {
      if (!selectedDetail) { return; }
      var carrier = selectedDetail;
      switchModal('carrierDetailModal', function () { showLink(carrier); });
    });
    $('detailEditButton').addEventListener('click', function () {
      if (!selectedDetail || busy || loading) { return; }
      var carrier = selectedDetail;
      switchModal('carrierDetailModal', function () { openForm(carrier); });
    });
  }

  function init() {
    if (!Auth.isLoggedIn()) { Auth.redirectToLogin(); return; }
    if (!hasAccess()) { denied(); return; }
    show('carriersContent');
    if (!window.bootstrap || !window.bootstrap.Modal) {
      hide('carriersLoading');
      text('carriersErrorText', 'Không tải được giao diện hộp thoại. Vui lòng tải lại trang.');
      show('carriersError');
      $('retryCarriers').addEventListener('click', function () { window.location.reload(); });
      return;
    }
    bindEvents();
    loadCarriers(true);
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); }
  else { init(); }
})();
