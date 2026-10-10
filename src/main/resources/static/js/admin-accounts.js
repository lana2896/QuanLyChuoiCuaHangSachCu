(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('accountsPage')) { return; }

  var Auth = OB.auth;
  var ROLE = 'QUAN_TRI_VIEN';
  var BASE = '/api/admin/accounts';
  var statusLabels = { HOAT_DONG: 'Hoạt động', CHO_XAC_THUC: 'Chờ xác thực', BI_KHOA: 'Bị khóa' };
  var roleClasses = {
    KHACH_HANG: 'badge-role-khach-hang',
    CHU_CUA_HANG: 'badge-role-chu-cua-hang',
    QUAN_LY: 'badge-role-quan-ly',
    QUAN_TRI_VIEN: 'badge-role-quan-tri-vien'
  };
  var accounts = [];
  var page = 0;
  var selectedRole = null;
  var selectedStatus = null;
  var loading = false;
  var busy = false;
  var allowModalClose = false;
  var filters = { tuKhoa: '', vaiTro: '', trangThai: '' };

  function $(id) { return document.getElementById(id); }
  function show(id) { $(id).classList.remove('d-none'); }
  function hide(id) { $(id).classList.add('d-none'); }
  function text(id, value) { $(id).textContent = value === null || value === undefined || value === '' ? '—' : String(value); }
  function modal(id) { return window.bootstrap.Modal.getOrCreateInstance($(id), { backdrop: 'static' }); }
  function closeModal(id) {
    allowModalClose = true;
    modal(id).hide();
    allowModalClose = false;
  }
  function pageSize() { return parseInt($('accountsPageSize').value, 10) || 10; }
  function pageCount() { return Math.max(1, Math.ceil(accounts.length / pageSize())); }
  function isSelf(account) {
    var current = Auth.getUser();
    return !!current && String(current.maTK) === String(account.maTK);
  }
  function error(id, message) { $(id).textContent = message; show(id); }

  function syncControls() {
    var controls = document.querySelectorAll('#accountsPage [data-admin-control]');
    for (var i = 0; i < controls.length; i++) {
      controls[i].disabled = busy || loading || controls[i].hasAttribute('data-always-disabled');
    }
    var forms = document.querySelectorAll('#createAccountModal button, #createAccountModal input, #createAccountModal select, #accountRoleModal button, #accountRoleModal select, #accountStatusModal button, #accountStatusModal textarea');
    for (var j = 0; j < forms.length; j++) { forms[j].disabled = busy; }
    $('accountsPrevious').disabled = busy || loading || page === 0;
    $('accountsNext').disabled = busy || loading || page + 1 >= pageCount();
  }

  function denied() { hide('accountsContent'); show('accountsDenied'); }

  function handleAuthError(err) {
    if (Auth.handleAuthError(err, ROLE)) { return true; }
    if (err.status === 403) { denied(); return true; }
    return false;
  }

  function rowHtml(account) {
    var esc = OB.escapeHtml;
    var id = esc(account.maTK);
    var locked = account.trangThai === 'BI_KHOA';
    var statusClass = locked ? 'text-bg-secondary' : (account.trangThai === 'HOAT_DONG' ? 'text-bg-success' : 'text-bg-warning');
    var cannotLock = !locked && isSelf(account);
    return '<tr data-account-id="' + id + '">' +
      '<td>' + id + '</td>' +
      '<td><div class="fw-semibold text-break">' + esc(account.hoTen) + '</div><div class="small text-muted text-break">' + esc(account.email) + '</div></td>' +
      '<td>' + esc(account.soDienThoai || '—') + '</td>' +
      '<td><span class="badge ' + (roleClasses[account.vaiTro] || 'text-bg-secondary') + '">' + esc(OB.roleLabels[account.vaiTro] || account.vaiTro) + '</span></td>' +
      '<td><span class="badge ' + statusClass + '">' + esc(statusLabels[account.trangThai] || account.trangThai) + '</span></td>' +
      '<td class="text-end"><div class="d-flex flex-wrap justify-content-end gap-1">' +
        '<button type="button" class="btn btn-outline-secondary btn-sm" data-account-action="detail" data-admin-control>Xem</button>' +
        '<button type="button" class="btn btn-outline-secondary btn-sm" data-account-action="role" data-admin-control>Đổi vai trò</button>' +
        '<button type="button" class="btn ' + (locked ? 'btn-outline-brown' : 'btn-outline-danger') + ' btn-sm" data-account-action="status" data-admin-control' +
          (cannotLock ? ' data-always-disabled disabled title="Bạn không thể tự khóa tài khoản"' : '') + '>' + (locked ? 'Mở khóa' : 'Khóa') + '</button>' +
      '</div></td></tr>';
  }

  function render() {
    page = Math.min(page, pageCount() - 1);
    hide('accountsLoading');
    if (!accounts.length) { hide('accountsResults'); show('accountsEmpty'); syncControls(); return; }
    hide('accountsEmpty');
    var start = page * pageSize();
    $('accountsRows').innerHTML = accounts.slice(start, start + pageSize()).map(rowHtml).join('');
    text('accountsCount', (start + 1) + '–' + Math.min(start + pageSize(), accounts.length) + ' / ' + accounts.length + ' tài khoản');
    text('accountsPageLabel', 'Trang ' + (page + 1) + ' / ' + pageCount());
    show('accountsResults');
    syncControls();
  }

  function loadAccounts(resetPage) {
    if (loading) { return Promise.resolve(); }
    if (resetPage) { page = 0; }
    hide('accountsError'); hide('accountsEmpty'); hide('accountsResults'); show('accountsLoading');
    loading = true;
    syncControls();
    var params = new URLSearchParams();
    Object.keys(filters).forEach(function (key) { if (filters[key]) { params.set(key, filters[key]); } });
    var query = params.toString();
    return OB.request('GET', BASE + (query ? '?' + query : '')).then(function (data) {
      accounts = Array.isArray(data) ? data : [];
      render();
    }).catch(function (err) {
      hide('accountsLoading');
      if (!handleAuthError(err)) { text('accountsErrorText', err.message); show('accountsError'); }
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
      if (afterSuccess && afterSuccess(data) === false) { return; }
      return loadAccounts(false);
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

  function showDetail(account) {
    text('detailAccountId', account.maTK); text('detailUserId', account.maND);
    text('detailName', account.hoTen); text('detailEmail', account.email); text('detailPhone', account.soDienThoai);
    text('detailRole', OB.roleLabels[account.vaiTro] || account.vaiTro);
    text('detailStatus', statusLabels[account.trangThai] || account.trangThai);
    var created = account.ngayTao ? new Date(account.ngayTao) : null;
    text('detailCreated', created && !isNaN(created.getTime()) ? created.toLocaleString('vi-VN') : account.ngayTao);
    modal('accountDetailModal').show();
  }

  function showRole(account) {
    selectedRole = account;
    hide('accountRoleError');
    text('accountRoleTarget', account.maTK + ' — ' + account.hoTen);
    $('accountNewRole').value = account.vaiTro;
    $('accountRoleSelfNotice').classList.toggle('d-none', !isSelf(account));
    modal('accountRoleModal').show();
  }

  function showStatus(account) {
    if (account.trangThai !== 'BI_KHOA' && isSelf(account)) { return; }
    selectedStatus = account;
    var locked = account.trangThai === 'BI_KHOA';
    hide('accountStatusError');
    text('accountStatusTarget', account.maTK + ' — ' + account.hoTen);
    text('accountStatusTitle', locked ? 'Mở khóa tài khoản' : 'Khóa tài khoản');
    text('accountStatusSubmit', locked ? 'Mở khóa tài khoản' : 'Khóa tài khoản');
    text('accountStatusDescription', locked ? 'Tài khoản sẽ được phép đăng nhập lại.' : 'Tài khoản sẽ bị khóa và các phiên đăng nhập hiện tại sẽ hết hiệu lực.');
    $('accountLockReasonGroup').classList.toggle('d-none', locked);
    $('accountLockReason').value = '';
    $('accountLockReason').required = !locked;
    $('accountStatusSubmit').className = 'btn ' + (locked ? 'btn-accent' : 'btn-outline-danger');
    modal('accountStatusModal').show();
  }

  function init() {
    if (!Auth.isLoggedIn()) { Auth.redirectToLogin(); return; }
    if (!Auth.hasRole(ROLE)) { denied(); return; }
    show('accountsContent');
    if (!window.bootstrap || !window.bootstrap.Modal) {
      hide('accountsLoading');
      text('accountsErrorText', 'Không tải được giao diện hộp thoại. Vui lòng tải lại trang.');
      show('accountsError');
      busy = true; syncControls(); busy = false;
      $('retryAccounts').disabled = false;
      $('retryAccounts').addEventListener('click', function () { window.location.reload(); });
      return;
    }
    ['createAccountModal', 'accountRoleModal', 'accountStatusModal'].forEach(function (id) {
      $(id).addEventListener('hide.bs.modal', function (event) {
        if (busy && !allowModalClose) { event.preventDefault(); }
      });
    });
    $('accountsFilters').addEventListener('submit', function (event) {
      event.preventDefault();
      if (busy || loading) { return; }
      filters = { tuKhoa: $('accountKeyword').value.trim(), vaiTro: $('accountRoleFilter').value, trangThai: $('accountStatusFilter').value };
      loadAccounts(true);
    });
    $('resetAccountFilters').addEventListener('click', function () {
      if (busy || loading) { return; }
      $('accountsFilters').reset(); filters = { tuKhoa: '', vaiTro: '', trangThai: '' }; loadAccounts(true);
    });
    $('retryAccounts').addEventListener('click', function () { if (!busy && !loading) { loadAccounts(false); } });
    $('accountsPageSize').addEventListener('change', function () { if (!busy && !loading) { page = 0; render(); } });
    $('accountsPrevious').addEventListener('click', function () { if (!busy && !loading && page > 0) { page--; render(); } });
    $('accountsNext').addEventListener('click', function () { if (!busy && !loading && page + 1 < pageCount()) { page++; render(); } });
    $('accountsRows').addEventListener('click', function (event) {
      var button = event.target.closest('[data-account-action]');
      if (!button || button.disabled || busy || loading) { return; }
      var id = button.closest('tr').getAttribute('data-account-id');
      var account = accounts.find(function (value) { return String(value.maTK) === id; });
      if (!account) { return; }
      var action = button.getAttribute('data-account-action');
      if (action === 'detail') { showDetail(account); }
      if (action === 'role') { showRole(account); }
      if (action === 'status') { showStatus(account); }
    });
    $('createAccountButton').addEventListener('click', function () {
      if (busy || loading) { return; }
      $('createAccountForm').reset(); hide('createAccountError'); modal('createAccountModal').show();
    });
    $('createAccountForm').addEventListener('submit', function (event) {
      event.preventDefault();
      var body = { hoTen: $('createAccountName').value.trim(), email: $('createAccountEmail').value.trim(), soDienThoai: $('createAccountPhone').value.trim() || null, matKhau: $('createAccountPassword').value, vaiTro: $('createAccountRole').value };
      if (!body.hoTen) { error('createAccountError', 'Vui lòng nhập họ tên.'); return; }
      mutate(this, 'createAccountError', 'createAccountModal', function () { return OB.request('POST', BASE, body); }, 'Đã tạo tài khoản.', function () { $('createAccountForm').reset(); });
    });
    $('accountRoleForm').addEventListener('submit', function (event) {
      event.preventDefault();
      if (!selectedRole) { return; }
      var account = selectedRole;
      var role = $('accountNewRole').value;
      if (role === account.vaiTro) { error('accountRoleError', 'Vui lòng chọn vai trò khác vai trò hiện tại.'); return; }
      mutate(this, 'accountRoleError', 'accountRoleModal', function () {
        return OB.request('PUT', BASE + '/' + encodeURIComponent(account.maTK) + '/role', { vaiTro: role });
      }, 'Đã cập nhật vai trò.', function () {
        if (isSelf(account)) { Auth.clear(); Auth.redirectToLogin('/'); return false; }
      });
    });
    $('accountStatusForm').addEventListener('submit', function (event) {
      event.preventDefault();
      if (!selectedStatus) { return; }
      var account = selectedStatus;
      var unlock = account.trangThai === 'BI_KHOA';
      var reason = $('accountLockReason').value.trim();
      if (!unlock && !reason) { error('accountStatusError', 'Vui lòng nhập lý do khóa tài khoản.'); return; }
      mutate(this, 'accountStatusError', 'accountStatusModal', function () {
        return OB.request('PUT', BASE + '/' + encodeURIComponent(account.maTK) + (unlock ? '/unlock' : '/lock'), unlock ? undefined : { lyDo: reason });
      }, unlock ? 'Đã mở khóa tài khoản.' : 'Đã khóa tài khoản.');
    });
    loadAccounts(true);
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); }
  else { init(); }
})();
