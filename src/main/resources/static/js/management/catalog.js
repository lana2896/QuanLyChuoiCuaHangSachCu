(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('catalogPage')) { return; }

  var Auth = OB.auth;
  var ROLES = ['QUAN_LY', 'QUAN_TRI_VIEN'];
  var CAT_URL = '/api/management/categories';
  var TITLE_URL = '/api/management/book-titles';
  var ON = 'DANG_BAN';
  var OFF = 'NGUNG_BAN';
  var PAGE_SIZE = 20;

  var categories = [];
  var titles = [];
  var titlePage = 0;
  var titleTotalPages = 0;
  var titleTotal = 0;
  var titlesLoaded = false;
  var editingCategory = null;
  var editingTitle = null;
  var busy = false;

  function $(id) { return document.getElementById(id); }
  function show(id) { $(id).classList.remove('d-none'); }
  function hide(id) { $(id).classList.add('d-none'); }
  function esc(v) { return OB.escapeHtml(v); }
  function val(id) { return $(id).value.trim(); }
  function opt(id) { return val(id) || null; }
  function num(id) { var v = val(id); return v === '' ? null : Number(v); }
  function modal(id) { return window.bootstrap.Modal.getOrCreateInstance($(id), { backdrop: 'static' }); }
  function showError(id, msg) { $(id).textContent = msg; show(id); }
  function badge(status) {
    return '<span class="badge ' + (status === ON ? 'carrier-badge-active' : 'carrier-badge-paused') + '">' +
      (status === ON ? 'Đang bán' : 'Ngừng bán') + '</span>';
  }

  function handleError(err, errorId) {
    if (Auth.handleAuthError(err)) { return; }
    if (err.status === 403) { hide('catalogContent'); show('catalogDenied'); return; }
    showError(errorId || 'catalogError', err.message);
  }

  function setBusy(form, value) {
    busy = value;
    var submit = form.querySelector('[type="submit"]');
    submit.disabled = value;
    form.setAttribute('aria-busy', String(value));
  }

  function submitForm(form, errorId, modalId, action, message, done) {
    if (busy || !form.reportValidity()) { return; }
    hide(errorId);
    setBusy(form, true);
    action().then(function () {
      modal(modalId).hide();
      OB.toast(message, 'success');
      return done();
    }).catch(function (err) {
      if (Auth.handleAuthError(err)) { modal(modalId).hide(); return; }
      showError(errorId, err.message);
    }).then(function () { setBusy(form, false); });
  }

  function categoryOptions(selected, onlyActive) {
    return categories.filter(function (c) {
      return !onlyActive || c.trangThaiBan === ON || c.maDM === selected;
    }).map(function (c) {
      return '<option value="' + c.maDM + '"' + (c.maDM === selected ? ' selected' : '') + '>' + esc(c.tenDanhMuc) + '</option>';
    }).join('');
  }

  function fillCategoryFilter() {
    var current = $('titleCategoryFilter').value;
    $('titleCategoryFilter').innerHTML = '<option value="">Tất cả</option>' + categoryOptions(null, false);
    $('titleCategoryFilter').value = current;
  }

  function renderCategories() {
    hide('categoriesLoading');
    var keyword = val('categoryKeyword').toLowerCase();
    var status = $('categoryStatusFilter').value;
    var list = categories.filter(function (c) {
      return (!status || c.trangThaiBan === status) &&
        (!keyword || c.tenDanhMuc.toLowerCase().indexOf(keyword) >= 0);
    });
    if (!list.length) { hide('categoriesResults'); show('categoriesEmpty'); return; }
    hide('categoriesEmpty');
    $('categoryRows').innerHTML = list.map(function (c) {
      var on = c.trangThaiBan === ON;
      return '<tr data-id="' + c.maDM + '">' +
        '<td><span class="carrier-code">' + c.maDM + '</span></td>' +
        '<td class="fw-semibold text-break">' + esc(c.tenDanhMuc) + '</td>' +
        '<td class="small text-muted text-break">' + (c.moTa ? esc(c.moTa) : '—') + '</td>' +
        '<td>' + badge(c.trangThaiBan) + '</td>' +
        '<td class="text-end"><div class="d-flex flex-wrap justify-content-end gap-1">' +
          '<button type="button" class="btn btn-outline-secondary btn-sm" data-action="edit">Cập nhật</button>' +
          '<button type="button" class="btn ' + (on ? 'btn-outline-danger' : 'btn-accent') + ' btn-sm" data-action="toggle">' + (on ? 'Ngừng bán' : 'Mở bán') + '</button>' +
        '</div></td></tr>';
    }).join('');
    show('categoriesResults');
  }

  function loadCategories() {
    return OB.request('GET', CAT_URL).then(function (data) {
      categories = Array.isArray(data) ? data : [];
      renderCategories();
      fillCategoryFilter();
    }).catch(function (err) { hide('categoriesLoading'); handleError(err); });
  }

  function renderTitles() {
    hide('titlesLoading');
    if (!titles.length) { hide('titlesResults'); show('titlesEmpty'); return; }
    hide('titlesEmpty');
    $('titleRows').innerHTML = titles.map(function (t) {
      var on = t.trangThaiBan === ON;
      var meta = [t.tacGia, t.nhaXuatBan, t.namXuatBan].filter(function (x) { return x; }).join(' · ');
      return '<tr data-id="' + t.maTuaSach + '">' +
        '<td><span class="carrier-code">' + t.maTuaSach + '</span></td>' +
        '<td><div class="fw-semibold text-break">' + esc(t.tenSach) + '</div><div class="small text-muted text-break">' + (meta ? esc(meta) : '—') + '</div></td>' +
        '<td>' + esc(t.tenDanhMuc) + '</td>' +
        '<td>' + esc(OB.formatMoney(t.giaGoc)) + '</td>' +
        '<td>' + badge(t.trangThaiBan) + '</td>' +
        '<td class="text-end"><div class="d-flex flex-wrap justify-content-end gap-1">' +
          '<button type="button" class="btn btn-outline-secondary btn-sm" data-action="edit">Cập nhật</button>' +
          '<button type="button" class="btn ' + (on ? 'btn-outline-danger' : 'btn-accent') + ' btn-sm" data-action="toggle">' + (on ? 'Ngừng bán' : 'Mở bán') + '</button>' +
        '</div></td></tr>';
    }).join('');
    var start = titlePage * PAGE_SIZE;
    $('titlesCount').textContent = (start + 1) + '–' + (start + titles.length) + ' / ' + titleTotal + ' tựa sách';
    $('titlesPageLabel').textContent = 'Trang ' + (titlePage + 1) + ' / ' + Math.max(1, titleTotalPages);
    $('titlesPrevious').disabled = titlePage === 0;
    $('titlesNext').disabled = titlePage + 1 >= titleTotalPages;
    show('titlesResults');
  }

  function loadTitles(resetPage) {
    if (resetPage) { titlePage = 0; }
    hide('titlesResults'); hide('titlesEmpty'); show('titlesLoading');
    var params = new URLSearchParams();
    if (val('titleKeyword')) { params.set('tuKhoa', val('titleKeyword')); }
    if ($('titleCategoryFilter').value) { params.set('maDM', $('titleCategoryFilter').value); }
    if ($('titleStatusFilter').value) { params.set('trangThaiBan', $('titleStatusFilter').value); }
    params.set('page', titlePage);
    params.set('size', PAGE_SIZE);
    return OB.request('GET', TITLE_URL + '?' + params.toString()).then(function (data) {
      data = data || {};
      titles = data.content || [];
      titleTotal = data.totalElements || 0;
      titleTotalPages = data.totalPages || 0;
      titlesLoaded = true;
      renderTitles();
    }).catch(function (err) { hide('titlesLoading'); handleError(err); });
  }

  function openCategory(category) {
    editingCategory = category;
    hide('categoryFormError');
    $('categoryModalTitle').textContent = category ? 'Cập nhật danh mục' : 'Thêm danh mục';
    $('categoryName').value = category ? category.tenDanhMuc : '';
    $('categoryDesc').value = category && category.moTa ? category.moTa : '';
    modal('categoryModal').show();
  }

  function openTitle(title) {
    editingTitle = title;
    hide('titleFormError');
    $('titleModalTitle').textContent = title ? 'Cập nhật tựa sách' : 'Thêm tựa sách';
    $('titleCategory').innerHTML = categoryOptions(title ? title.maDM : null, true);
    if (!title) { $('titleCategory').selectedIndex = 0; }
    $('titleName').value = title ? title.tenSach : '';
    $('titleAuthor').value = title && title.tacGia || '';
    $('titlePublisher').value = title && title.nhaXuatBan || '';
    $('titleYear').value = title && title.namXuatBan || '';
    $('titlePrice').value = title ? title.giaGoc : '';
    $('titleImage').value = title && title.hinhAnhUrl || '';
    $('titleDesc').value = title && title.moTaChiTiet || '';
    modal('titleModal').show();
  }

  function toggle(url, current, reload) {
    if (busy) { return; }
    busy = true;
    var next = current === ON ? OFF : ON;
    OB.request('PATCH', url + '/sale-status', { trangThaiBan: next }).then(function () {
      OB.toast(next === ON ? 'Đã mở bán.' : 'Đã ngừng bán.', 'success');
      return reload();
    }).catch(function (err) { handleError(err); }).then(function () { busy = false; });
  }

  function bind() {
    $('categoryFilters').addEventListener('submit', function (e) { e.preventDefault(); });
    $('categoryKeyword').addEventListener('input', renderCategories);
    $('categoryStatusFilter').addEventListener('change', renderCategories);
    $('createCategoryButton').addEventListener('click', function () { openCategory(null); });
    $('createTitleButton').addEventListener('click', function () { openTitle(null); });

    $('categoryRows').addEventListener('click', function (e) {
      var button = e.target.closest('[data-action]');
      if (!button) { return; }
      var id = Number(button.closest('tr').getAttribute('data-id'));
      var category = categories.filter(function (c) { return c.maDM === id; })[0];
      if (!category) { return; }
      if (button.getAttribute('data-action') === 'edit') { openCategory(category); }
      else { toggle(CAT_URL + '/' + id, category.trangThaiBan, loadCategories); }
    });

    $('titleRows').addEventListener('click', function (e) {
      var button = e.target.closest('[data-action]');
      if (!button) { return; }
      var id = Number(button.closest('tr').getAttribute('data-id'));
      var title = titles.filter(function (t) { return t.maTuaSach === id; })[0];
      if (!title) { return; }
      if (button.getAttribute('data-action') === 'edit') { openTitle(title); }
      else { toggle(TITLE_URL + '/' + id, title.trangThaiBan, function () { return loadTitles(false); }); }
    });

    $('categoryForm').addEventListener('submit', function (e) {
      e.preventDefault();
      var body = { tenDanhMuc: val('categoryName'), moTa: opt('categoryDesc') };
      if (!body.tenDanhMuc) { showError('categoryFormError', 'Vui lòng nhập tên danh mục.'); return; }
      var editing = editingCategory;
      submitForm(this, 'categoryFormError', 'categoryModal', function () {
        return editing ? OB.request('PUT', CAT_URL + '/' + editing.maDM, body) : OB.request('POST', CAT_URL, body);
      }, editing ? 'Đã cập nhật danh mục.' : 'Đã thêm danh mục.', function () {
        return loadCategories().then(function () { if (titlesLoaded) { return loadTitles(false); } });
      });
    });

    $('titleForm').addEventListener('submit', function (e) {
      e.preventDefault();
      var body = {
        maDM: num('titleCategory'),
        tenSach: val('titleName'),
        tacGia: opt('titleAuthor'),
        nhaXuatBan: opt('titlePublisher'),
        namXuatBan: num('titleYear'),
        giaGoc: num('titlePrice'),
        moTaChiTiet: opt('titleDesc'),
        hinhAnhUrl: opt('titleImage')
      };
      if (!body.tenSach) { showError('titleFormError', 'Vui lòng nhập tên sách.'); return; }
      var editing = editingTitle;
      submitForm(this, 'titleFormError', 'titleModal', function () {
        return editing ? OB.request('PUT', TITLE_URL + '/' + editing.maTuaSach, body) : OB.request('POST', TITLE_URL, body);
      }, editing ? 'Đã cập nhật tựa sách.' : 'Đã thêm tựa sách.', function () { return loadTitles(false); });
    });

    $('titleFilters').addEventListener('submit', function (e) { e.preventDefault(); loadTitles(true); });
    $('resetTitleFilters').addEventListener('click', function () { $('titleFilters').reset(); loadTitles(true); });
    $('titlesPrevious').addEventListener('click', function () { if (titlePage > 0) { titlePage--; loadTitles(false); } });
    $('titlesNext').addEventListener('click', function () { if (titlePage + 1 < titleTotalPages) { titlePage++; loadTitles(false); } });
    $('tabTitles').addEventListener('shown.bs.tab', function () { if (!titlesLoaded) { loadTitles(true); } });
  }

  function init() {
    if (!Auth.isLoggedIn()) { Auth.redirectToLogin(); return; }
    var user = Auth.getUser();
    if (!user || ROLES.indexOf(user.vaiTro) < 0) { hide('catalogContent'); show('catalogDenied'); return; }
    show('catalogContent');
    if (!window.bootstrap || !window.bootstrap.Modal) {
      showError('catalogError', 'Không tải được giao diện hộp thoại. Vui lòng tải lại trang.');
      return;
    }
    bind();
    loadCategories();
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); }
  else { init(); }
})();
