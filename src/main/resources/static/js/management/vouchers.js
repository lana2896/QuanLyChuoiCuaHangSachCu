(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('vouchersPage')) { return; }

  var Auth = OB.auth;
  var PAGE = document.getElementById('vouchersPage');
  var ROLES = (PAGE.getAttribute('data-roles') || 'QUAN_LY,QUAN_TRI_VIEN').split(',');
  var BASE = PAGE.getAttribute('data-api-base') || '/api/management/vouchers';
  var LABEL = PAGE.getAttribute('data-voucher-label') || 'Voucher Sàn';
  var PERCENT = 'THEO_PHAN_TRAM';
  var ACTIVE = 'HOAT_DONG';
  var PAUSED = 'TAM_DUNG';
  var statusLabels = {
    DANG_AP_DUNG: 'Đang áp dụng',
    TAM_DUNG: 'Tạm dừng',
    CHUA_BAT_DAU: 'Chưa bắt đầu',
    HET_HAN: 'Hết hạn',
    HET_LUOT: 'Hết lượt'
  };

  var vouchers = [];
  var page = 0;
  var loading = false;
  var busy = false;
  var allowModalClose = false;
  var editing = null;
  var statusTarget = null;
  var deleteTarget = null;
  var filters = { tuKhoa: '', trangThai: '' };

  function $(id) { return document.getElementById(id); }
  function show(id) { $(id).classList.remove('d-none'); }
  function hide(id) { $(id).classList.add('d-none'); }
  function esc(value) { return OB.escapeHtml(value); }
  function blank(value) { return value === null || value === undefined || value === ''; }
  function text(id, value) { $(id).textContent = blank(value) ? '—' : String(value); }
  function trimmed(id) { return $(id).value.trim(); }
  function numberValue(id) { var v = trimmed(id); return v === '' ? null : Number(v); }
  function modal(id) { return window.bootstrap.Modal.getOrCreateInstance($(id), { backdrop: 'static' }); }
  function closeModal(id) {
    allowModalClose = true;
    modal(id).hide();
    allowModalClose = false;
  }
  function error(id, message) { $(id).textContent = message; show(id); }
  function pageSize() { return parseInt($('vouchersPageSize').value, 10) || 10; }
  function pageCount() { return Math.max(1, Math.ceil(vouchers.length / pageSize())); }
  function find(id) {
    return vouchers.find(function (v) { return String(v.maKM) === String(id); }) || null;
  }
  function hasAccess() {
    var user = Auth.getUser();
    return !!user && ROLES.indexOf(user.vaiTro) >= 0;
  }

  function formatDate(value) {
    if (blank(value)) { return '—'; }
    var date = new Date(value);
    return isNaN(date.getTime()) ? String(value) : date.toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
  }

  function toInputDate(value) {
    return blank(value) ? '' : String(value).slice(0, 16);
  }

  function toApiDate(value) {
    return value.length === 16 ? value + ':00' : value;
  }

  function discountText(voucher) {
    if (voucher.loaiGiamGia === PERCENT) {
      var text = Number(voucher.giaTriGiam) + '%';
      if (voucher.giaTriGiamToiDa) { text += ' (tối đa ' + OB.formatMoney(voucher.giaTriGiamToiDa) + ')'; }
      return text;
    }
    return OB.formatMoney(voucher.giaTriGiam);
  }

  function statusBadge(voucher) {
    var key = voucher.trangThaiHienThi;
    var label = esc(statusLabels[key] || key);
    if (key === 'DANG_AP_DUNG') { return '<span class="badge carrier-badge-active">' + label + '</span>'; }
    if (key === 'TAM_DUNG') { return '<span class="badge carrier-badge-paused">' + label + '</span>'; }
    return '<span class="badge" style="background:var(--status-neutral-bg);color:var(--status-neutral-text)">' + label + '</span>';
  }

  function syncControls() {
    var controls = document.querySelectorAll('#vouchersPage [data-voucher-control]');
    for (var i = 0; i < controls.length; i++) {
      controls[i].disabled = busy || loading || controls[i].hasAttribute('data-voucher-locked');
    }
    var forms = document.querySelectorAll('#voucherFormModal button, #voucherFormModal input, #voucherFormModal select, #voucherStatusModal button, #voucherDeleteModal button');
    for (var j = 0; j < forms.length; j++) {
      forms[j].disabled = busy || (forms[j].id === 'voucherCode' && !!editing);
    }
    $('vouchersPrevious').disabled = busy || loading || page === 0;
    $('vouchersNext').disabled = busy || loading || page + 1 >= pageCount();
  }

  function denied() { hide('vouchersContent'); show('vouchersDenied'); }

  function handleAuthError(err) {
    if (Auth.handleAuthError(err)) { return true; }
    if (err.status === 403) { denied(); return true; }
    return false;
  }

  function renderStats(stats) {
    stats = stats || {};
    text('statTotal', stats.tong || 0);
    text('statActive', stats.dangApDung || 0);
    text('statPaused', stats.tamDung || 0);
    text('statExpired', stats.hetHieuLuc || 0);
  }

  function rowHtml(voucher) {
    var active = voucher.trangThai === ACTIVE;
    return '<tr data-voucher-id="' + esc(voucher.maKM) + '">' +
      '<td><span class="carrier-code">' + esc(voucher.code) + '</span>' +
        '<div class="small text-muted text-break mt-1">' + esc(voucher.tenKhuyenMai) + '</div></td>' +
      '<td>' + esc(discountText(voucher)) + '</td>' +
      '<td>' + esc(OB.formatMoney(voucher.giaTriDonToiThieu)) + '</td>' +
      '<td class="small"><div>' + esc(formatDate(voucher.ngayBatDau)) + '</div><div class="text-muted">đến ' + esc(formatDate(voucher.ngayKetThuc)) + '</div></td>' +
      '<td><div>' + esc(voucher.daSuDung) + ' / ' + esc(voucher.soLuong) + '</div><div class="small text-muted">Còn ' + esc(voucher.conLai) + ' lượt</div></td>' +
      '<td>' + statusBadge(voucher) + '</td>' +
      '<td class="text-end"><div class="d-flex flex-wrap justify-content-end gap-1">' +
        '<button type="button" class="btn btn-outline-secondary btn-sm" data-voucher-action="edit" data-voucher-control>Chỉnh sửa</button>' +
        '<button type="button" class="btn ' + (active ? 'btn-outline-danger' : 'btn-accent') + ' btn-sm" data-voucher-action="status" data-voucher-control>' + (active ? 'Tạm dừng' : 'Kích hoạt') + '</button>' +
        '<button type="button" class="btn btn-outline-danger btn-sm" data-voucher-action="delete" data-voucher-control' +
          (voucher.coTheXoa ? '' : ' data-voucher-locked title="Voucher đã được sử dụng nên không thể xóa"') + '><i class="bi bi-trash" aria-hidden="true"></i></button>' +
      '</div></td></tr>';
  }

  function render() {
    page = Math.min(page, pageCount() - 1);
    hide('vouchersLoading');
    if (!vouchers.length) { hide('vouchersResults'); show('vouchersEmpty'); syncControls(); return; }
    hide('vouchersEmpty');
    var start = page * pageSize();
    $('vouchersRows').innerHTML = vouchers.slice(start, start + pageSize()).map(rowHtml).join('');
    text('vouchersCount', (start + 1) + '–' + Math.min(start + pageSize(), vouchers.length) + ' / ' + vouchers.length + ' voucher');
    text('vouchersPageLabel', 'Trang ' + (page + 1) + ' / ' + pageCount());
    show('vouchersResults');
    syncControls();
  }

  function loadVouchers(resetPage) {
    if (loading) { return Promise.resolve(); }
    if (resetPage) { page = 0; }
    hide('vouchersError'); hide('vouchersEmpty'); hide('vouchersResults'); show('vouchersLoading');
    loading = true;
    syncControls();
    var params = new URLSearchParams();
    Object.keys(filters).forEach(function (key) { if (filters[key]) { params.set(key, filters[key]); } });
    var query = params.toString();
    return OB.request('GET', BASE + (query ? '?' + query : '')).then(function (data) {
      data = data || {};
      vouchers = Array.isArray(data.danhSach) ? data.danhSach : [];
      renderStats(data.thongKe);
      render();
    }).catch(function (err) {
      hide('vouchersLoading');
      if (!handleAuthError(err)) { text('vouchersErrorText', err.message); show('vouchersError'); }
    }).then(function () { loading = false; syncControls(); });
  }

  function mutate(form, errorId, modalId, action, message) {
    if (busy || loading || !form.reportValidity()) { return; }
    hide(errorId);
    var submit = form.querySelector('[type="submit"]');
    var submitLabel = submit.textContent;
    submit.textContent = 'Đang xử lý...';
    form.setAttribute('aria-busy', 'true');
    busy = true;
    syncControls();
    action().then(function () {
      closeModal(modalId);
      OB.toast(message, 'success');
      busy = false;
      return loadVouchers(false);
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

  function applyDiscountType() {
    var percent = $('voucherDiscountType').value === PERCENT;
    var value = $('voucherValue');
    text('voucherValueLabel', percent ? 'Giá trị giảm (%)' : 'Giá trị giảm (đ)');
    value.min = percent ? '1' : '1000';
    value.max = percent ? '100' : '1000000000';
    value.step = percent ? '1' : '1000';
    $('voucherMaxGroup').classList.toggle('d-none', !percent);
  }

  function fillForm(voucher) {
    var v = voucher || {};
    $('voucherCode').value = v.code || '';
    $('voucherName').value = v.tenKhuyenMai || '';
    $('voucherDiscountType').value = v.loaiGiamGia || PERCENT;
    applyDiscountType();
    $('voucherValue').value = blank(v.giaTriGiam) ? '' : Number(v.giaTriGiam);
    $('voucherMax').value = blank(v.giaTriGiamToiDa) ? '' : Number(v.giaTriGiamToiDa);
    $('voucherMinOrder').value = blank(v.giaTriDonToiThieu) ? '0' : Number(v.giaTriDonToiThieu);
    $('voucherQuantity').value = blank(v.soLuong) ? '' : v.soLuong;
    $('voucherStart').value = toInputDate(v.ngayBatDau);
    $('voucherEnd').value = toInputDate(v.ngayKetThuc);
  }

  function openForm(voucher) {
    editing = voucher || null;
    hide('voucherFormError');
    fillForm(editing);
    $('voucherCode').required = !editing;
    var used = editing ? Number(editing.daSuDung || 0) : 0;
    $('voucherQuantity').min = String(Math.max(1, used));
    $('voucherQuantityHelp').classList.toggle('d-none', !used);
    $('voucherQuantityHelp').textContent = used ? 'Đã có ' + used + ' lượt sử dụng, số lượng không được nhỏ hơn mức này.' : '';
    text('voucherFormTitle', editing ? 'Chỉnh sửa voucher ' + editing.code : 'Tạo ' + LABEL + ' mới');
    text('voucherCodeHelp', editing ? 'Mã code đã được sử dụng nên không thể thay đổi.' : 'Mã duy nhất trên toàn hệ thống, không đổi được sau khi tạo.');
    text('voucherFormSubmit', editing ? 'Lưu thay đổi' : 'Lưu chương trình');
    syncControls();
    modal('voucherFormModal').show();
  }

  function formBody() {
    var percent = $('voucherDiscountType').value === PERCENT;
    var max = numberValue('voucherMax');
    return {
      tenKhuyenMai: trimmed('voucherName'),
      loaiGiamGia: $('voucherDiscountType').value,
      giaTriGiam: numberValue('voucherValue'),
      giaTriGiamToiDa: percent && max ? max : null,
      giaTriDonToiThieu: numberValue('voucherMinOrder') || 0,
      soLuong: numberValue('voucherQuantity'),
      ngayBatDau: toApiDate($('voucherStart').value),
      ngayKetThuc: toApiDate($('voucherEnd').value)
    };
  }

  function showStatus(voucher) {
    statusTarget = voucher;
    var pausing = voucher.trangThai === ACTIVE;
    hide('voucherStatusError');
    text('voucherStatusTarget', voucher.code + ' — ' + voucher.tenKhuyenMai);
    text('voucherStatusTitle', pausing ? 'Tạm dừng voucher' : 'Kích hoạt voucher');
    text('voucherStatusSubmit', pausing ? 'Tạm dừng' : 'Kích hoạt');
    $('voucherStatusSubmit').className = 'btn ' + (pausing ? 'btn-outline-danger' : 'btn-accent');
    text('voucherStatusDescription', pausing
      ? 'Khách hàng sẽ không thể áp dụng mã này khi đặt hàng cho tới khi voucher được kích hoạt lại.'
      : 'Khách hàng có thể áp dụng lại mã này nếu voucher còn trong thời gian hiệu lực và còn lượt sử dụng.');
    syncControls();
    modal('voucherStatusModal').show();
  }

  function showDelete(voucher) {
    deleteTarget = voucher;
    hide('voucherDeleteError');
    text('voucherDeleteTarget', voucher.code + ' — ' + voucher.tenKhuyenMai);
    syncControls();
    modal('voucherDeleteModal').show();
  }

  function bindEvents() {
    ['voucherFormModal', 'voucherStatusModal', 'voucherDeleteModal'].forEach(function (id) {
      $(id).addEventListener('hide.bs.modal', function (event) {
        if (busy && !allowModalClose) { event.preventDefault(); }
      });
    });

    $('vouchersFilters').addEventListener('submit', function (event) {
      event.preventDefault();
      if (busy || loading) { return; }
      filters = { tuKhoa: trimmed('voucherKeyword'), trangThai: $('voucherStatusFilter').value };
      loadVouchers(true);
    });
    $('resetVoucherFilters').addEventListener('click', function () {
      if (busy || loading) { return; }
      $('vouchersFilters').reset();
      filters = { tuKhoa: '', trangThai: '' };
      loadVouchers(true);
    });
    $('retryVouchers').addEventListener('click', function () { if (!busy && !loading) { loadVouchers(false); } });
    $('vouchersPageSize').addEventListener('change', function () { if (!busy && !loading) { page = 0; render(); } });
    $('vouchersPrevious').addEventListener('click', function () { if (!busy && !loading && page > 0) { page--; render(); } });
    $('vouchersNext').addEventListener('click', function () { if (!busy && !loading && page + 1 < pageCount()) { page++; render(); } });

    $('vouchersRows').addEventListener('click', function (event) {
      var button = event.target.closest('[data-voucher-action]');
      if (!button || button.disabled || busy || loading) { return; }
      var voucher = find(button.closest('tr').getAttribute('data-voucher-id'));
      if (!voucher) { return; }
      var action = button.getAttribute('data-voucher-action');
      if (action === 'edit') { openForm(voucher); }
      if (action === 'status') { showStatus(voucher); }
      if (action === 'delete') { showDelete(voucher); }
    });

    $('createVoucherButton').addEventListener('click', function () {
      if (busy || loading) { return; }
      openForm(null);
    });

    $('voucherCode').addEventListener('input', function () {
      var upper = this.value.toUpperCase();
      if (upper !== this.value) { this.value = upper; }
    });
    $('voucherDiscountType').addEventListener('change', applyDiscountType);

    $('voucherForm').addEventListener('submit', function (event) {
      event.preventDefault();
      var body = formBody();
      if (!body.tenKhuyenMai) { error('voucherFormError', 'Vui lòng nhập tên chương trình.'); return; }
      if (new Date(body.ngayKetThuc) <= new Date(body.ngayBatDau)) {
        error('voucherFormError', 'Thời gian kết thúc phải sau thời gian bắt đầu');
        return;
      }
      if (editing) {
        var id = editing.maKM;
        mutate(this, 'voucherFormError', 'voucherFormModal', function () {
          return OB.request('PUT', BASE + '/' + encodeURIComponent(id), body);
        }, 'Đã cập nhật voucher.');
        return;
      }
      body.code = trimmed('voucherCode').toUpperCase();
      mutate(this, 'voucherFormError', 'voucherFormModal', function () {
        return OB.request('POST', BASE, body);
      }, 'Tạo ' + LABEL + ' thành công');
    });

    $('voucherStatusForm').addEventListener('submit', function (event) {
      event.preventDefault();
      if (!statusTarget) { return; }
      var voucher = statusTarget;
      var pausing = voucher.trangThai === ACTIVE;
      mutate(this, 'voucherStatusError', 'voucherStatusModal', function () {
        return OB.request('PATCH', BASE + '/' + encodeURIComponent(voucher.maKM) + '/status',
          { trangThai: pausing ? PAUSED : ACTIVE });
      }, pausing ? 'Đã tạm dừng voucher ' + voucher.code + '.' : 'Đã kích hoạt voucher ' + voucher.code + '.');
    });

    $('voucherDeleteForm').addEventListener('submit', function (event) {
      event.preventDefault();
      if (!deleteTarget) { return; }
      var voucher = deleteTarget;
      mutate(this, 'voucherDeleteError', 'voucherDeleteModal', function () {
        return OB.request('DELETE', BASE + '/' + encodeURIComponent(voucher.maKM));
      }, 'Đã xóa voucher ' + voucher.code + '.');
    });
  }

  function init() {
    if (!Auth.isLoggedIn()) { Auth.redirectToLogin(); return; }
    if (!hasAccess()) { denied(); return; }
    show('vouchersContent');
    if (!window.bootstrap || !window.bootstrap.Modal) {
      hide('vouchersLoading');
      text('vouchersErrorText', 'Không tải được giao diện hộp thoại. Vui lòng tải lại trang.');
      show('vouchersError');
      $('retryVouchers').addEventListener('click', function () { window.location.reload(); });
      return;
    }
    bindEvents();
    applyDiscountType();
    loadVouchers(true);
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); }
  else { init(); }
})();
