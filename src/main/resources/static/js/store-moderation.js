(function () {
  'use strict';

  var client = window.OldBook;
  if (!client || !document.getElementById('moderationContent')) { return; }
  var user = client.auth.getUser();
  if (!user) { client.auth.redirectToLogin(); return; }
  if (user.vaiTro !== 'QUAN_LY' && user.vaiTro !== 'QUAN_TRI_VIEN') {
    showError(document.getElementById('moderationAccessError'), 'Chỉ quản lý hoặc quản trị viên mới có quyền kiểm duyệt.');
    return;
  }
  if (!window.bootstrap || !window.bootstrap.Modal) {
    showError(document.getElementById('moderationAccessError'), 'Không tải được giao diện kiểm duyệt. Vui lòng tải lại trang.');
    return;
  }

  var base = '/api/store/moderation/';
  var lists = {};
  var current = null;
  var detailVersion = 0;
  var detailBusy = false;
  var actionBusy = false;
  var modalElement = document.getElementById('reviewDetailModal');
  var modal = new window.bootstrap.Modal(modalElement);
  var detailBody = document.getElementById('reviewDetailBody');
  var detailError = document.getElementById('reviewDetailError');
  var rejectForm = document.getElementById('reviewRejectForm');
  var reasonInput = document.getElementById('reviewRejectReason');
  var approveButton = document.getElementById('reviewApproveButton');
  var rejectButton = document.getElementById('reviewRejectButton');
  var retryButton = document.getElementById('reviewDetailRetry');
  var esc = client.escapeHtml;
  var statusLabels = { CHO_DUYET: 'Chờ duyệt', DA_DUYET: 'Đã duyệt', TU_CHOI: 'Từ chối' };
  var saleLabels = { DANG_BAN: 'Đang bán', NGUNG_BAN: 'Ngừng bán' };

  function showError(element, message) {
    element.textContent = message || '';
    element.classList.toggle('d-none', !message);
  }

  function handleError(element, error) {
    if (client.auth.handleAuthError(error, user.vaiTro)) { return; }
    showError(element, error && error.message ? error.message : 'Không tải được dữ liệu. Vui lòng thử lại.');
  }

  function value(text) {
    return text === null || text === undefined || text === '' ? 'Chưa cung cấp' : String(text);
  }

  function dateText(date) {
    if (!date) { return 'Chưa cung cấp'; }
    var parsed = new Date(date);
    return Number.isNaN(parsed.getTime()) ? String(date) : parsed.toLocaleString('vi-VN');
  }

  function money(amount) {
    return amount === null || amount === undefined ? 'Chưa cung cấp' : Number(amount).toLocaleString('vi-VN') + ' đ';
  }

  function badge(status) {
    var color = status === 'DA_DUYET' ? 'success' : status === 'TU_CHOI' ? 'danger' : 'warning text-dark';
    return '<span class="badge bg-' + color + '">' + esc(statusLabels[status] || value(status)) + '</span>';
  }

  function field(label, text) {
    return '<dt class="col-sm-4">' + esc(label) + '</dt><dd class="col-sm-8 text-break" style="white-space: pre-wrap">' + esc(value(text)) + '</dd>';
  }

  function safeImageUrl(raw) {
    if (!raw) { return null; }
    try {
      var url = new URL(raw, window.location.origin);
      return (url.protocol === 'http:' || url.protocol === 'https:') && !url.username && !url.password ? url.href : null;
    } catch (error) { return null; }
  }

  function updateControls() {
    Object.keys(lists).forEach(function (kind) {
      var list = lists[kind];
      Array.prototype.forEach.call(list.form.elements, function (element) { element.disabled = list.loading || actionBusy; });
      list.prev.disabled = list.loading || actionBusy || list.page <= 0;
      list.next.disabled = list.loading || actionBusy || list.page + 1 >= list.totalPages;
    });
    Array.prototype.forEach.call(document.querySelectorAll('[data-review-id]'), function (button) {
      button.disabled = detailBusy || actionBusy;
    });
    var pending = !!current && !!current.data && current.data.trangThaiDuyet === 'CHO_DUYET';
    approveButton.classList.toggle('d-none', !pending);
    rejectButton.classList.toggle('d-none', !pending);
    rejectForm.classList.toggle('d-none', !pending);
    approveButton.disabled = actionBusy || detailBusy || !pending;
    rejectButton.disabled = actionBusy || detailBusy || !pending;
    reasonInput.disabled = actionBusy;
    retryButton.disabled = detailBusy || actionBusy;
    Array.prototype.forEach.call(modalElement.querySelectorAll('[data-bs-dismiss="modal"]'), function (button) {
      button.disabled = actionBusy;
    });
    document.getElementById('reviewActionLoading').classList.toggle('d-none', !actionBusy);
  }

  function filterValues(list) {
    return {
      trangThaiDuyet: list.form.elements.trangThaiDuyet.value,
      tuKhoa: list.form.elements.tuKhoa.value.trim(),
      maCH: list.form.elements.maCH.value,
      size: list.form.elements.size.value
    };
  }

  function renderRows(list, items) {
    list.rows.innerHTML = items.map(function (item) {
      var id = list.kind === 'store' ? item.maCH : item.maSach;
      var cells = list.kind === 'store'
        ? '<td>' + esc(value(item.tenCuaHang)) + '</td><td class="text-break">' + esc(value(item.diaChiCH)) + '</td><td>' + badge(item.trangThaiDuyet) + '</td><td>' + esc(dateText(item.ngayTao)) + '</td>'
        : '<td><div>' + esc(value(item.tenSach)) + '</div><small class="text-muted">' + esc(value(item.tacGia)) + '</small></td><td>' + esc(value(item.tenCuaHang)) + '<div class="small text-muted">Mã: ' + esc(item.maCH) + '</div></td><td class="text-nowrap">' + esc(money(item.giaBanCu)) + '</td><td>' + badge(item.trangThaiDuyet) + '</td>';
      return '<tr><th scope="row">' + esc(id) + '</th>' + cells + '<td><button type="button" class="btn btn-outline-brown btn-sm text-nowrap" data-review-kind="' + list.kind + '" data-review-id="' + esc(id) + '">Xem chi tiết</button></td></tr>';
    }).join('');
    list.empty.classList.toggle('d-none', items.length !== 0);
  }

  function loadList(list) {
    list.loading = true;
    list.rows.innerHTML = '';
    list.empty.classList.add('d-none');
    list.summary.textContent = '';
    showError(list.error, '');
    list.spinner.classList.remove('d-none');
    updateControls();
    var query = new URLSearchParams();
    query.set('page', list.page);
    Object.keys(list.filters).forEach(function (key) {
      if (list.filters[key] !== '') { query.set(key, list.filters[key]); }
    });
    return client.request('GET', base + list.path + '?' + query.toString()).then(function (data) {
      if (!data || !Array.isArray(data.content)) { throw new Error('Dữ liệu danh sách không hợp lệ. Vui lòng thử lại.'); }
      list.totalPages = data.totalPages;
      if (list.page > 0 && list.page >= data.totalPages) {
        list.page = Math.max(0, data.totalPages - 1);
        return loadList(list);
      }
      list.page = data.page;
      renderRows(list, data.content);
      list.summary.textContent = data.totalElements + ' mục · Trang ' + (data.totalPages ? data.page + 1 : 0) + ' / ' + data.totalPages;
    }).catch(function (error) {
      list.totalPages = 0;
      handleError(list.error, error);
    }).finally(function () {
      list.loading = false;
      list.spinner.classList.add('d-none');
      updateControls();
    });
  }

  function setupList(kind, prefix, path) {
    var list = {
      kind: kind, path: path, page: 0, totalPages: 0, loading: false,
      form: document.getElementById(prefix + 'Filter'),
      rows: document.getElementById(prefix + 'Rows'),
      error: document.getElementById(prefix + 'Error'),
      empty: document.getElementById(prefix + 'Empty'),
      spinner: document.getElementById(prefix + 'Loading'),
      summary: document.getElementById(prefix + 'Summary'),
      prev: document.getElementById(prefix + 'Prev'),
      next: document.getElementById(prefix + 'Next')
    };
    list.filters = filterValues(list);
    lists[kind] = list;
    list.form.addEventListener('submit', function (event) {
      event.preventDefault();
      if (list.loading || actionBusy || !list.form.reportValidity()) { return; }
      list.filters = filterValues(list);
      list.page = 0;
      loadList(list);
    });
    list.prev.addEventListener('click', function () {
      if (list.loading || actionBusy || list.page <= 0) { return; }
      list.page -= 1;
      loadList(list);
    });
    list.next.addEventListener('click', function () {
      if (list.loading || actionBusy || list.page + 1 >= list.totalPages) { return; }
      list.page += 1;
      loadList(list);
    });
  }

  function renderDetail(item, kind) {
    var html = '<dl class="row mb-0">';
    if (kind === 'store') {
      html += field('Mã cửa hàng', item.maCH) + field('Tên cửa hàng', item.tenCuaHang)
        + field('Mô tả', item.moTa) + field('Địa chỉ', item.diaChiCH) + field('Số điện thoại', item.soDienThoaiCH);
    } else {
      var image = safeImageUrl(item.hinhAnhUrl);
      if (image) {
        html = '<img class="img-thumbnail mb-3" src="' + esc(image) + '" alt="Ảnh bìa sách" referrerpolicy="no-referrer" style="max-height: 220px; max-width: 100%; object-fit: contain"><dl class="row mb-0">';
      }
      html += field('Mã sách', item.maSach) + field('Tên sách', item.tenSach) + field('Tác giả', item.tacGia)
        + field('Mã cửa hàng', item.maCH) + field('Tên cửa hàng', item.tenCuaHang)
        + field('Mã danh mục', item.maDM) + field('Danh mục', item.tenDanhMuc)
        + field('Nhà xuất bản', item.nhaXuatBan) + field('Năm xuất bản', item.namXuatBan)
        + field('Giá bìa', money(item.giaBia)) + field('Giá bán', money(item.giaBanCu))
        + field('Độ mới', item.doMoiPercent === null || item.doMoiPercent === undefined ? null : item.doMoiPercent + '%')
        + field('Tình trạng vật lý', item.tinhTrangVatLy) + field('Mô tả chi tiết', item.moTaChiTiet)
        + field('Số lượng tồn', item.soLuongTon) + field('Trạng thái bán', saleLabels[item.trangThaiBan] || item.trangThaiBan)
        + field('Đường dẫn ảnh', item.hinhAnhUrl);
    }
    html += field('Trạng thái duyệt', statusLabels[item.trangThaiDuyet] || item.trangThaiDuyet)
      + field('Ngày tạo', dateText(item.ngayTao)) + field('Lý do từ chối', item.lyDoTuChoi || 'Không có') + '</dl>';
    detailBody.innerHTML = html;
  }

  function loadDetail() {
    if (!current || detailBusy || actionBusy) { return; }
    var selection = current;
    var version = ++detailVersion;
    detailBusy = true;
    current.data = null;
    detailBody.innerHTML = '';
    showError(detailError, '');
    retryButton.classList.add('d-none');
    document.getElementById('reviewDetailLoading').classList.remove('d-none');
    updateControls();
    client.request('GET', base + lists[selection.kind].path + '/' + selection.id).then(function (data) {
      if (version !== detailVersion) { return; }
      if (!data || (selection.kind === 'store' ? data.maCH : data.maSach) !== selection.id) {
        throw new Error('Dữ liệu chi tiết không hợp lệ. Vui lòng thử lại.');
      }
      current.data = data;
      renderDetail(data, selection.kind);
    }).catch(function (error) {
      if (version !== detailVersion) { return; }
      handleError(detailError, error);
      retryButton.classList.remove('d-none');
    }).finally(function () {
      if (version !== detailVersion) { return; }
      detailBusy = false;
      document.getElementById('reviewDetailLoading').classList.add('d-none');
      updateControls();
    });
  }

  function moderate(reject) {
    if (actionBusy || detailBusy || !current || !current.data || current.data.trangThaiDuyet !== 'CHO_DUYET') { return; }
    var reason = reasonInput.value.trim();
    if (reject && (!reason || reasonInput.value.length > 500)) {
      reasonInput.setCustomValidity(reason ? 'Lý do từ chối tối đa 500 ký tự.' : 'Vui lòng nhập lý do từ chối.');
      reasonInput.reportValidity();
      return;
    }
    reasonInput.setCustomValidity('');
    var selection = current;
    actionBusy = true;
    showError(detailError, '');
    updateControls();
    client.request('POST', base + lists[selection.kind].path + '/' + selection.id + (reject ? '/reject' : '/approve'), reject ? { lyDo: reason } : undefined)
      .then(function () {
        actionBusy = false;
        updateControls();
        modal.hide();
        client.toast(reject ? 'Đã từ chối và lưu lý do.' : 'Phê duyệt thành công.', 'success');
        return loadList(lists[selection.kind]);
      }).catch(function (error) {
        handleError(detailError, error);
        retryButton.classList.remove('d-none');
      }).finally(function () {
        actionBusy = false;
        updateControls();
      });
  }

  document.getElementById('moderationContent').classList.remove('d-none');
  setupList('store', 'storeReview', 'stores');
  setupList('book', 'bookReview', 'books');
  document.getElementById('moderationContent').addEventListener('click', function (event) {
    var button = event.target.closest('[data-review-id]');
    if (!button || detailBusy || actionBusy) { return; }
    var kind = button.getAttribute('data-review-kind');
    var id = Number(button.getAttribute('data-review-id'));
    if (!lists[kind] || !Number.isInteger(id) || id <= 0) { return; }
    current = { kind: kind, id: id, data: null };
    reasonInput.value = '';
    reasonInput.setCustomValidity('');
    document.getElementById('reviewRejectCount').textContent = '0';
    document.getElementById('reviewDetailTitle').textContent = kind === 'store' ? 'Thông tin cửa hàng #' + id : 'Thông tin sách #' + id;
    modal.show();
    loadDetail();
  });
  reasonInput.addEventListener('input', function () {
    reasonInput.setCustomValidity('');
    document.getElementById('reviewRejectCount').textContent = reasonInput.value.length;
  });
  rejectForm.addEventListener('submit', function (event) { event.preventDefault(); moderate(true); });
  approveButton.addEventListener('click', function () { moderate(false); });
  retryButton.addEventListener('click', loadDetail);
  modalElement.addEventListener('hide.bs.modal', function (event) {
    if (actionBusy) { event.preventDefault(); return; }
    detailVersion += 1;
    detailBusy = false;
    current = null;
    updateControls();
  });
  loadList(lists.store);
  loadList(lists.book);
  if (client.notifications) {
    client.notifications.refreshOn(['GIAN_HANG_CHO_DUYET', 'SACH_CHO_DUYET'], function () {
      loadList(lists.store);
      loadList(lists.book);
    }, function () { return !actionBusy && !lists.store.loading && !lists.book.loading; });
  }
})();
