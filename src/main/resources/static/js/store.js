(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('sellerPage')) { return; }

  var ROLE = 'CHU_CUA_HANG';
  var BASE = '/api/store/me/books';
  var esc = OB.escapeHtml;
  var store = null;
  var storeLoading = false;
  var writing = false;
  var page = 0;
  var totalPages = 0;
  var books = [];
  var storeSequence = 0;
  var booksSequence = 0;
  var detailSequence = 0;
  var categoriesPromise = null;
  var editingBookId = null;
  var stockBookId = null;
  var bookReady = false;
  var bookReadOnly = false;
  var closingAfterSave = false;
  var images = [];
  var selectedTitle = null;
  var titleResults = [];
  var titleTimer = null;
  var titleSequence = 0;
  var MAX_IMAGES = 10;
  var IMAGE_MAX_SIZE = 10 * 1024 * 1024;
  var bookModal;
  var stockModal;
  // Ô chọn Tỉnh/Thành phố và Phường/Xã của form thông tin cửa hàng (js/location-picker.js).
  var storeLocation = OB.locations.attach(document.getElementById('storeProvinceInput'), document.getElementById('storeWardInput'));

  function $(id) { return document.getElementById(id); }
  function visible(id, show) { $(id).classList.toggle('d-none', !show); }
  function error(id, message) {
    $(id).textContent = message || '';
    visible(id === 'booksErrorText' ? 'booksError' : id, !!message);
  }
  function message(err) { return (err && err.message) || 'Không thực hiện được yêu cầu. Vui lòng thử lại.'; }
  function authError(err) { return OB.auth.handleAuthError(err, ROLE); }
  function approved() { return !!store && store.trangThaiDuyet === 'DA_DUYET' && !storeLoading; }
  function canWrite() { return approved() && !writing; }
  function canRead() { return !!store && !storeLoading && !writing; }
  function nullableText(id) { return $(id).value.trim() || null; }
  function nullableNumber(id) { return $(id).value === '' ? null : Number($(id).value); }
  function setValue(id, value) { $(id).value = value === null || value === undefined ? '' : value; }
  function closeSavedModal(modal) {
    closingAfterSave = true;
    modal.hide();
    closingAfterSave = false;
  }

  function badge(status) {
    var labels = { CHO_DUYET: 'Chờ duyệt', DA_DUYET: 'Đã duyệt', TU_CHOI: 'Từ chối' };
    var colors = { CHO_DUYET: 'warning', DA_DUYET: 'success', TU_CHOI: 'danger' };
    return '<span class="badge text-bg-' + (colors[status] || 'secondary') + '">' + esc(labels[status] || status || 'Chưa xác định') + '</span>';
  }

  function syncControls() {
    $('saveStore').disabled = writing || storeLoading;
    $('editStore').disabled = writing || storeLoading;
    $('cancelStoreEdit').disabled = writing;
    $('refreshStore').disabled = writing || storeLoading;
    $('addBook').disabled = !canWrite();
    $('saveBook').disabled = bookReadOnly || !canWrite() || !bookReady;
    $('bookFields').disabled = bookReadOnly || writing || !bookReady || !approved();
    $('saveStock').disabled = !canWrite() || stockBookId === null;
    $('stockQuantity').disabled = writing || !approved();
    document.querySelectorAll('#sellerBooksRows [data-book-action]').forEach(function (button) {
      button.disabled = button.dataset.bookAction === 'view' ? !canRead() : !canWrite();
    });
    document.querySelectorAll('#sellerBookModal [data-bs-dismiss], #sellerStockModal [data-bs-dismiss]').forEach(function (button) {
      button.disabled = writing;
    });
  }

  function openStoreForm() {
    if (writing || storeLoading) { return; }
    error('storeFormError', '');
    setValue('storeName', store ? store.tenCuaHang : 'Hệ thống tự đặt khi đăng ký');
    setValue('storePhoneInput', store && store.soDienThoaiCH);
    setValue('storeAddressInput', store && store.diaChiChiTiet);
    storeLocation.setValue(store && store.maTinh, store && store.maXa).catch(function (err) {
      error('storeFormError', message(err));
    });
    setValue('storeDescriptionInput', store && store.moTa);
    $('saveStore').textContent = store ? 'Lưu và gửi duyệt lại' : 'Đăng ký cửa hàng';
    $('storeFormNotice').textContent = store
      ? 'Sửa thông tin sẽ đưa cửa hàng về chờ duyệt. Trong thời gian này bạn vẫn xem được sách, nhưng cần đợi cửa hàng được duyệt để quản lý sách; sách của cửa hàng cũng tạm ngừng hiển thị công khai.'
      : 'Đăng ký cửa hàng để gửi quản lý xét duyệt. Bạn có thể thêm sách sau khi cửa hàng được duyệt.';
    visible('storeDetails', false);
    visible('editStore', false);
    visible('storeForm', true);
    visible('cancelStoreEdit', !!store);
  }

  function renderStore() {
    visible('storePanel', true);
    visible('storeForm', false);
    visible('storeDetails', !!store);
    visible('editStore', !!store);
    visible('sellerBooks', !!store);
    if (!store) {
      $('storeHeading').textContent = 'Đăng ký cửa hàng';
      $('storeStatus').textContent = 'Bạn chưa đăng ký cửa hàng.';
      openStoreForm();
      syncControls();
      return;
    }
    $('storeHeading').textContent = store.tenCuaHang;
    $('storeStatus').innerHTML = badge(store.trangThaiDuyet);
    $('storeAddress').textContent = store.diaChiCH || 'Chưa có địa chỉ';
    $('storePhone').textContent = store.soDienThoaiCH || 'Chưa có số điện thoại';
    $('storeDescription').textContent = store.moTa || 'Chưa có mô tả cửa hàng.';
    $('storeRejection').textContent = 'Lý do từ chối: ' + (store.lyDoTuChoi || 'Chưa có lý do được cung cấp.');
    visible('storeRejection', store.trangThaiDuyet === 'TU_CHOI');
    $('storeApprovalNotice').textContent = store.trangThaiDuyet === 'TU_CHOI'
      ? 'Cửa hàng bị từ chối. Hãy sửa thông tin theo phản hồi và gửi duyệt lại. Bạn vẫn xem được danh sách sách hiện có.'
      : 'Cửa hàng chưa được duyệt. Bạn vẫn xem được danh sách sách; các thao tác thêm, sửa, đổi tồn kho và bật/ngừng bán sẽ mở sau khi được duyệt.';
    visible('storeApprovalNotice', !approved());
    syncControls();
  }

  function loadStore() {
    if (writing && storeLoading) { return Promise.resolve(); }
    var sequence = ++storeSequence;
    ++booksSequence;
    storeLoading = true;
    store = null;
    visible('storeLoading', true);
    visible('storeLoadError', false);
    visible('storePanel', false);
    visible('sellerBooks', false);
    syncControls();
    return OB.request('GET', '/api/store/me').then(function (data) {
      if (sequence !== storeSequence) { return; }
      if (!data || !data.maCH) { throw new Error('Phản hồi cửa hàng không hợp lệ. Vui lòng tải lại.'); }
      store = data;
      storeLoading = false;
      renderStore();
      return loadBooks();
    }).catch(function (err) {
      if (sequence !== storeSequence || authError(err)) { return; }
      storeLoading = false;
      if (err instanceof OB.ApiError && err.status === 400 && err.message === 'Bạn chưa đăng ký cửa hàng') {
        renderStore();
      } else {
        $('storeLoadErrorText').textContent = message(err);
        visible('storeLoadError', true);
      }
    }).finally(function () {
      if (sequence !== storeSequence) { return; }
      storeLoading = false;
      visible('storeLoading', false);
      syncControls();
    });
  }

  function rowHtml(book) {
    var id = esc(book.maSach);
    var stock = book.soLuongTon === null || book.soLuongTon === undefined ? 'Chưa xác định' : book.soLuongTon;
    var selling = book.trangThaiBan === 'DANG_BAN';
    var saleLabel = selling ? 'Đang bán' : (book.trangThaiBan === 'NGUNG_BAN' ? 'Ngừng bán' : 'Chưa xác định');
    var rejection = book.lyDoTuChoi
      ? '<div class="small text-danger mt-1" style="white-space:pre-wrap">Lý do: ' + esc(book.lyDoTuChoi) + '</div>' : '';
    var disabled = canWrite() ? '' : ' disabled';
    return '<tr><td style="min-width:200px;max-width:340px"><div class="fw-semibold text-break">' + esc(book.tenSach) + '</div>' +
      '<div class="small text-muted">' + esc(book.tacGia || 'Chưa có tác giả') + ' · ' + esc(book.tenDanhMuc || 'Chưa có danh mục') + '</div>' +
      (book.doMoiPercent === null || book.doMoiPercent === undefined ? '' : '<div class="small text-muted">Độ mới: ' + esc(book.doMoiPercent) + '%</div>') + '</td>' +
      '<td class="text-nowrap">' + esc(OB.formatMoney(book.giaBanCu)) + '</td><td>' + esc(stock) + '</td>' +
      '<td style="min-width:140px;max-width:280px">' + badge(book.trangThaiDuyet) + rejection + '</td>' +
      '<td><span class="badge text-bg-' + (selling ? 'success' : 'secondary') + '">' + esc(saleLabel) + '</span></td>' +
      '<td class="text-end" style="min-width:210px"><div class="d-flex flex-wrap justify-content-end gap-1">' +
      '<button type="button" class="btn btn-outline-secondary btn-sm" data-book-action="view" data-book-id="' + id + '"' + (canRead() ? '' : ' disabled') + '>Xem</button>' +
      '<button type="button" class="btn btn-outline-secondary btn-sm" data-book-action="edit" data-book-id="' + id + '"' + disabled + '>Sửa nội dung</button>' +
      '<button type="button" class="btn btn-outline-secondary btn-sm" data-book-action="stock" data-book-id="' + id + '"' + disabled + '>Tồn kho</button>' +
      '<button type="button" class="btn btn-outline-' + (selling ? 'danger' : 'secondary') + ' btn-sm" data-book-action="sale" data-book-id="' + id + '"' + disabled + '>' + (selling ? 'Ngừng bán' : 'Bật bán') + '</button>' +
      '</div></td></tr>';
  }

  function loadBooks() {
    if (!store) { return Promise.resolve(); }
    var sequence = ++booksSequence;
    var params = new URLSearchParams({ page: String(page), size: $('bookPageSize').value, sort: $('bookSort').value });
    if ($('bookApprovalFilter').value) { params.set('trangThaiDuyet', $('bookApprovalFilter').value); }
    visible('booksLoading', true);
    visible('booksError', false);
    visible('booksEmpty', false);
    visible('booksTablePanel', false);
    visible('booksPagination', false);
    books = [];
    return OB.request('GET', BASE + '?' + params.toString()).then(function (data) {
      if (sequence !== booksSequence || !store) { return; }
      if (!data || !Array.isArray(data.content)) { throw new Error('Phản hồi danh sách sách không hợp lệ. Vui lòng tải lại.'); }
      var lastPage = Math.max(0, data.totalPages - 1);
      if (data.page > lastPage) {
        page = lastPage;
        return loadBooks();
      }
      books = data.content;
      page = data.page;
      totalPages = data.totalPages;
      $('sellerBooksRows').innerHTML = books.map(rowHtml).join('');
      visible('booksEmpty', books.length === 0);
      visible('booksTablePanel', books.length > 0);
      visible('booksPagination', data.totalElements > 0);
      $('booksPageSummary').textContent = data.totalElements + ' sách · Trang ' + (page + 1) + ' / ' + totalPages;
      $('booksPrevious').disabled = page <= 0;
      $('booksNext').disabled = page + 1 >= totalPages;
      syncControls();
    }).catch(function (err) {
      if (sequence !== booksSequence || authError(err)) { return; }
      $('booksErrorText').textContent = message(err);
      visible('booksError', true);
    }).finally(function () {
      if (sequence === booksSequence) { visible('booksLoading', false); }
    });
  }

  function categories() {
    if (!categoriesPromise) {
      categoriesPromise = OB.request('GET', '/api/books/categories').then(function (data) {
        if (!Array.isArray(data) || data.length === 0) { throw new Error('Chưa có danh mục sách. Vui lòng thử lại sau khi danh mục được bổ sung.'); }
        return data;
      }).catch(function (err) { categoriesPromise = null; throw err; });
    }
    return categoriesPromise;
  }

  function releaseImages() {
    images.forEach(function (item) { if (item.preview) { window.URL.revokeObjectURL(item.preview); } });
    images = [];
  }

  function renderImages() {
    $('bookImageList').innerHTML = images.map(function (item, index) {
      return '<div class="position-relative"><img src="' + esc(item.preview || item.url) + '" class="img-thumbnail" alt="Ảnh sách ' + (index + 1) + '" style="width:110px;height:140px;object-fit:cover">' +
        (bookReadOnly ? '' : '<button type="button" class="btn btn-danger btn-sm position-absolute top-0 end-0 py-0 px-1" data-image-index="' + index + '" aria-label="Xóa ảnh ' + (index + 1) + '">&times;</button>') + '</div>';
    }).join('');
    visible('bookImageList', images.length > 0);
  }

  function resetBookImages(urls) {
    releaseImages();
    images = (urls || []).map(function (url) { return { url: url }; });
    $('bookImageFile').value = '';
    error('bookImageError', '');
    visible('bookImageUploading', false);
    renderImages();
  }

  function imageValidationMessage(file) {
    if (!file || ['image/jpeg', 'image/png', 'image/webp'].indexOf(file.type) === -1) {
      return 'Vui lòng chọn ảnh JPEG, PNG hoặc WebP.';
    }
    if (!file.size) { return 'Ảnh không được để trống.'; }
    if (file.size > IMAGE_MAX_SIZE) { return 'Mỗi ảnh không được vượt quá 10 MB.'; }
    return '';
  }

  function selectBookImages() {
    if (bookReadOnly || !canWrite() || !bookReady) { return; }
    var files = Array.prototype.slice.call($('bookImageFile').files);
    $('bookImageFile').value = '';
    var problem = '';
    files.forEach(function (file) {
      var validation = imageValidationMessage(file);
      if (validation) { problem = validation; return; }
      if (images.length >= MAX_IMAGES) { problem = 'Tối đa ' + MAX_IMAGES + ' ảnh cho mỗi sách.'; return; }
      images.push({ file: file, preview: window.URL.createObjectURL(file) });
    });
    error('bookImageError', problem);
    renderImages();
  }

  function removeBookImage(index) {
    if (bookReadOnly || !canWrite() || !bookReady || !images[index]) { return; }
    var removed = images.splice(index, 1)[0];
    if (removed.preview) { window.URL.revokeObjectURL(removed.preview); }
    error('bookImageError', '');
    renderImages();
  }

  function uploadPendingImages() {
    var pending = images.filter(function (item) { return item.file && !item.url; });
    if (!pending.length) { return Promise.resolve(); }
    visible('bookImageUploading', true);
    return pending.reduce(function (chain, item) {
      return chain.then(function () {
        var data = new window.FormData();
        data.append('file', item.file);
        return OB.request('POST', BASE + '/images', data).then(function (result) {
          if (!result || typeof result.hinhAnhUrl !== 'string' || !/^https:\/\//i.test(result.hinhAnhUrl)) {
            throw new Error('Phản hồi tải ảnh không hợp lệ. Vui lòng thử lại.');
          }
          item.url = result.hinhAnhUrl;
        });
      });
    }, Promise.resolve()).finally(function () { visible('bookImageUploading', false); });
  }

  function showTitleInfo(info) {
    var safeCover = info && info.hinhAnhUrl && /^https?:\/\//i.test(info.hinhAnhUrl);
    if (safeCover) { $('bookInfoCover').setAttribute('src', info.hinhAnhUrl); } else { $('bookInfoCover').removeAttribute('src'); }
    visible('bookInfoCover', !!safeCover);
    if (info) {
      $('bookInfoName').textContent = info.tenSach || '';
      $('bookInfoAuthor').textContent = 'Tác giả: ' + (info.tacGia || 'Chưa có');
      $('bookInfoPublisher').textContent = 'NXB: ' + (info.nhaXuatBan || 'Chưa có') + (info.namXuatBan ? ' · ' + info.namXuatBan : '');
      $('bookInfoCategory').textContent = 'Danh mục: ' + (info.tenDanhMuc || 'Chưa có');
      $('bookInfoPrice').textContent = OB.formatMoney(info.giaGoc);
    }
    visible('bookTitleInfo', !!info);
    updateSalePrice();
  }

  function updateSalePrice() {
    var base = selectedTitle ? Number(selectedTitle.giaGoc) : NaN;
    var percent = Number($('bookCondition').value);
    var valid = !isNaN(base) && $('bookCondition').value !== '' && percent > 0 && percent <= 100;
    $('bookSalePrice').value = valid ? OB.formatMoney(Math.round(base * percent) / 100) : '';
  }

  function renderTitleResults() {
    $('bookTitleResults').innerHTML = titleResults.map(function (item) {
      var active = selectedTitle && selectedTitle.maTuaSach === item.maTuaSach;
      return '<button type="button" class="list-group-item list-group-item-action' + (active ? ' active' : '') + '" data-title-id="' + esc(item.maTuaSach) + '">' +
        '<div class="fw-semibold text-break">' + esc(item.tenSach) + '</div>' +
        '<div class="small' + (active ? '' : ' text-muted') + '">' + esc(item.tacGia || 'Chưa có tác giả') + ' · ' + esc(item.tenDanhMuc) + ' · ' + esc(OB.formatMoney(item.giaGoc)) + '</div></button>';
    }).join('');
  }

  function searchTitles() {
    var sequence = ++titleSequence;
    var params = new URLSearchParams({ page: '0', size: '20' });
    var keyword = $('bookTitleSearch').value.trim();
    if (keyword) { params.set('tuKhoa', keyword); }
    if ($('bookCategory').value) { params.set('maDM', $('bookCategory').value); }
    $('bookTitleResults').innerHTML = '<div class="list-group-item text-muted small">Đang tìm tựa sách...</div>';
    return OB.request('GET', BASE + '/titles?' + params.toString()).then(function (data) {
      if (sequence !== titleSequence) { return; }
      if (!data || !Array.isArray(data.content)) { throw new Error('Phản hồi danh sách tựa sách không hợp lệ.'); }
      titleResults = data.content;
      if (!titleResults.length) {
        $('bookTitleResults').innerHTML = '<div class="list-group-item text-muted small">Không tìm thấy tựa sách phù hợp. Liên hệ quản lý để bổ sung tựa sách.</div>';
        return;
      }
      renderTitleResults();
    }).catch(function (err) {
      if (sequence !== titleSequence || authError(err)) { return; }
      $('bookTitleResults').innerHTML = '<div class="list-group-item text-danger small">' + esc(message(err)) + '</div>';
    });
  }

  function selectTitle(id) {
    var item = titleResults.find(function (t) { return String(t.maTuaSach) === String(id); });
    if (!item || bookReadOnly || editingBookId !== null) { return; }
    selectedTitle = item;
    setValue('bookCategory', item.maDM);
    error('bookTitleError', '');
    renderTitleResults();
    showTitleInfo(item);
  }

  function clearTitleSelection() {
    selectedTitle = null;
    showTitleInfo(null);
  }

  function loadBookForm() {
    var sequence = ++detailSequence;
    var id = editingBookId;
    var readOnly = bookReadOnly;
    bookReady = false;
    error('bookFormError', '');
    visible('retryBookDetail', false);
    visible('bookModalLoading', true);
    syncControls();
    return Promise.all([id === null ? categories() : Promise.resolve(null), id === null ? Promise.resolve(null) : OB.request('GET', BASE + '/' + encodeURIComponent(id))])
      .then(function (results) {
        if (sequence !== detailSequence) { return; }
        var items = results[0];
        var book = results[1];
        if (id !== null && (!book || String(book.maSach) !== String(id))) { throw new Error('Không tải được thông tin sách. Vui lòng thử lại.'); }
        if (book) {
          selectedTitle = { giaGoc: book.giaBia };
          showTitleInfo({ tenSach: book.tenSach, tacGia: book.tacGia, nhaXuatBan: book.nhaXuatBan, namXuatBan: book.namXuatBan,
            tenDanhMuc: book.tenDanhMuc, giaGoc: book.giaBia, hinhAnhUrl: book.hinhAnhUrl });
          setValue('bookCondition', book.doMoiPercent);
          setValue('bookPhysicalCondition', book.tinhTrangVatLy);
          resetBookImages(book.hinhAnhs);
          updateSalePrice();
          $('bookModalTitle').textContent = (readOnly ? 'Chi tiết: ' : 'Sửa nội dung: ') + book.tenSach;
          if (readOnly) {
            var saleStatus = book.trangThaiBan === 'DANG_BAN' ? 'Đang bán' : (book.trangThaiBan === 'NGUNG_BAN' ? 'Ngừng bán' : 'Chưa xác định');
            $('bookViewStatus').innerHTML = badge(book.trangThaiDuyet) + ' <span class="badge text-bg-secondary">' + esc(saleStatus) + '</span>';
            $('bookViewStock').textContent = 'Tồn kho: ' + (book.soLuongTon === null || book.soLuongTon === undefined ? 'Chưa xác định' : book.soLuongTon);
            $('bookViewRejection').textContent = 'Lý do từ chối: ' + (book.lyDoTuChoi || 'Chưa có lý do được cung cấp.');
            visible('bookViewRejection', book.trangThaiDuyet === 'TU_CHOI' || !!book.lyDoTuChoi);
            visible('bookViewDetails', true);
          }
        } else {
          $('bookCategory').innerHTML = '<option value="">Chọn danh mục</option>' + items.map(function (item) {
            return '<option value="' + esc(item.maDM) + '">' + esc(item.tenDanhMuc) + '</option>';
          }).join('');
          searchTitles();
        }
        bookReady = true;
      }).catch(function (err) {
        if (sequence !== detailSequence || authError(err)) { return; }
        error('bookFormError', message(err));
        visible('retryBookDetail', true);
      }).finally(function () {
        if (sequence !== detailSequence) { return; }
        visible('bookModalLoading', false);
        syncControls();
      });
  }

  function openBook(id, readOnly) {
    if (readOnly ? !canRead() : !canWrite()) { return; }
    bookReadOnly = !!readOnly;
    editingBookId = id === undefined ? null : id;
    var creating = editingBookId === null;
    $('sellerBookForm').reset();
    resetBookImages(null);
    clearTimeout(titleTimer);
    ++titleSequence;
    titleResults = [];
    selectedTitle = null;
    showTitleInfo(null);
    error('bookTitleError', '');
    $('bookTitleResults').innerHTML = '';
    $('bookCategory').innerHTML = '<option value="">Đang tải danh mục...</option>';
    $('bookModalTitle').textContent = bookReadOnly ? 'Chi tiết sách' : (creating ? 'Thêm sách mới' : 'Sửa nội dung sách');
    visible('bookSubmissionNotice', !bookReadOnly);
    visible('bookViewDetails', false);
    visible('saveBook', !bookReadOnly);
    visible('bookPickerGroup', creating);
    visible('bookImageInputGroup', !bookReadOnly);
    $('bookCategory').required = creating;
    $('bookCategory').disabled = !creating;
    $('bookTitleSearch').disabled = !creating;
    $('bookModalClose').textContent = bookReadOnly ? 'Đóng' : 'Hủy';
    visible('initialStockGroup', creating);
    $('bookInitialStock').required = creating;
    $('bookInitialStock').disabled = !creating;
    $('saveBook').textContent = creating ? 'Gửi sách để duyệt' : 'Lưu và gửi duyệt lại';
    bookModal.show();
    loadBookForm();
  }

  function write(method, url, body, errorId, onSuccess, prepareBody) {
    if (writing) { return; }
    writing = true;
    error(errorId, '');
    syncControls();
    var request = prepareBody
      ? prepareBody().then(function (prepared) { return OB.request(method, url, prepared); })
      : OB.request(method, url, body);
    return request.then(onSuccess).catch(function (err) {
      if (authError(err)) { return; }
      error(errorId, message(err));
      if (err instanceof OB.ApiError && err.status === 400 && err.message === 'Cửa hàng phải được phê duyệt trước khi quản lý sản phẩm') {
        return loadStore();
      }
    }).finally(function () { writing = false; syncControls(); });
  }

  function saveStore(event) {
    event.preventDefault();
    if (writing || storeLoading || !$('storeForm').reportValidity()) { return; }
    // Tên cửa hàng không gửi lên: máy chủ tự đặt "Oldbook chi nhánh N" và không cho đổi.
    var place = storeLocation.value();
    var phone = (nullableText('storePhoneInput') || '').replace(/\s+/g, '');
    var body = { moTa: nullableText('storeDescriptionInput'), maTinh: place.maTinh, maXa: place.maXa,
      diaChiChiTiet: nullableText('storeAddressInput'), soDienThoaiCH: phone || null };
    if (!body.maTinh || !body.maXa || !body.diaChiChiTiet || !body.soDienThoaiCH) {
      error('storeFormError', 'Tỉnh/thành phố, phường/xã, địa chỉ chi tiết và số điện thoại không được để trống.');
      return;
    }
    if (!/^0\d{9,10}$/.test(body.soDienThoaiCH)) {
      error('storeFormError', 'Số điện thoại bắt đầu bằng 0 và gồm 10-11 chữ số.');
      return;
    }
    write(store ? 'PUT' : 'POST', store ? '/api/store/me' : '/api/store', body, 'storeFormError', function (data) {
      if (!data || !data.maCH) { throw new Error('Phản hồi lưu cửa hàng không hợp lệ. Hãy làm mới để kiểm tra.'); }
      store = data;
      renderStore();
      OB.toast('Thông tin cửa hàng đã được gửi để duyệt.', 'success');
      page = 0;
      return loadBooks();
    });
  }

  function saveBook(event) {
    event.preventDefault();
    if (bookReadOnly || !canWrite() || !bookReady) { return; }
    if (editingBookId === null && !selectedTitle) {
      error('bookTitleError', 'Vui lòng chọn một tựa sách trong danh sách.');
      $('bookTitleSearch').focus();
      return;
    }
    if (!$('sellerBookForm').reportValidity()) { return; }
    if (!images.length) { error('bookImageError', 'Vui lòng thêm ít nhất 1 ảnh sách.'); return; }
    var body = { doMoiPercent: nullableNumber('bookCondition'), tinhTrangVatLy: nullableText('bookPhysicalCondition') };
    if (!body.tinhTrangVatLy) { error('bookFormError', 'Tình trạng thực tế không được để trống.'); return; }
    if (editingBookId === null) {
      body.maTuaSach = selectedTitle.maTuaSach;
      body.soLuongTon = Number($('bookInitialStock').value);
    }
    var url = BASE + (editingBookId === null ? '' : '/' + encodeURIComponent(editingBookId));
    write(editingBookId === null ? 'POST' : 'PUT', url, body, 'bookFormError', function () {
      closeSavedModal(bookModal);
      OB.toast('Nội dung sách đã được gửi để duyệt.', 'success');
      return loadBooks();
    }, function () {
      return uploadPendingImages().then(function () {
        body.hinhAnhUrls = images.map(function (item) { return item.url; });
        return body;
      });
    });
  }

  function openStock(book) {
    if (!canWrite()) { return; }
    stockBookId = book.maSach;
    $('stockBookTitle').textContent = book.tenSach;
    setValue('stockQuantity', Math.max(0, Number(book.soLuongTon) || 0));
    error('stockFormError', '');
    syncControls();
    stockModal.show();
  }

  function saveStock(event) {
    event.preventDefault();
    if (!canWrite() || stockBookId === null || !$('sellerStockForm').reportValidity()) { return; }
    write('PATCH', BASE + '/' + encodeURIComponent(stockBookId) + '/stock', { soLuongTon: Number($('stockQuantity').value) }, 'stockFormError', function () {
      closeSavedModal(stockModal);
      OB.toast('Đã cập nhật tồn kho.', 'success');
      return loadBooks();
    });
  }

  function changeSale(book) {
    if (!canWrite()) { return; }
    var status = book.trangThaiBan === 'DANG_BAN' ? 'NGUNG_BAN' : 'DANG_BAN';
    write('PATCH', BASE + '/' + encodeURIComponent(book.maSach) + '/sale-status', { trangThaiBan: status }, 'booksErrorText', function () {
      OB.toast(status === 'DANG_BAN' ? 'Đã bật bán sách.' : 'Đã ngừng bán sách.', 'success');
      return loadBooks();
    });
  }

  function init() {
    if (!OB.auth.isLoggedIn()) { OB.auth.redirectToLogin('/store/books'); return; }
    if (!OB.auth.hasRole(ROLE)) {
      visible('sellerWorkspace', false);
      visible('sellerAccessError', true);
      $('refreshStore').disabled = true;
      return;
    }
    if (!window.bootstrap || !window.bootstrap.Modal) {
      visible('storeLoading', false);
      $('storeLoadErrorText').textContent = 'Không tải được giao diện hộp thoại. Vui lòng tải lại trang.';
      visible('storeLoadError', true);
      $('retryStore').addEventListener('click', function () { window.location.reload(); });
      return;
    }
    bookModal = new bootstrap.Modal($('sellerBookModal'));
    stockModal = new bootstrap.Modal($('sellerStockModal'));
    $('refreshStore').addEventListener('click', function () { page = 0; loadStore(); });
    $('retryStore').addEventListener('click', loadStore);
    $('editStore').addEventListener('click', openStoreForm);
    $('cancelStoreEdit').addEventListener('click', renderStore);
    $('storeForm').addEventListener('submit', saveStore);
    $('addBook').addEventListener('click', function () { openBook(); });
    $('sellerBookForm').addEventListener('submit', saveBook);
    $('bookImageFile').addEventListener('change', selectBookImages);
    $('bookImageList').addEventListener('click', function (event) {
      var button = event.target.closest('[data-image-index]');
      if (button) { removeBookImage(Number(button.getAttribute('data-image-index'))); }
    });
    $('bookCondition').addEventListener('input', updateSalePrice);
    $('bookCategory').addEventListener('change', function () {
      if (selectedTitle && String(selectedTitle.maDM) !== $('bookCategory').value) { clearTitleSelection(); }
      searchTitles();
    });
    $('bookTitleSearch').addEventListener('input', function () {
      clearTimeout(titleTimer);
      titleTimer = setTimeout(searchTitles, 300);
    });
    $('bookTitleSearch').addEventListener('keydown', function (event) {
      if (event.key === 'Enter') { event.preventDefault(); clearTimeout(titleTimer); searchTitles(); }
    });
    $('bookTitleResults').addEventListener('click', function (event) {
      var button = event.target.closest('[data-title-id]');
      if (button) { selectTitle(button.getAttribute('data-title-id')); }
    });
    $('retryBookDetail').addEventListener('click', loadBookForm);
    $('sellerStockForm').addEventListener('submit', saveStock);
    $('retryBooks').addEventListener('click', loadBooks);
    $('booksFilters').addEventListener('submit', function (event) { event.preventDefault(); page = 0; loadBooks(); });
    $('booksFilters').addEventListener('change', function () { page = 0; loadBooks(); });
    $('booksPrevious').addEventListener('click', function () { if (page > 0) { --page; loadBooks(); } });
    $('booksNext').addEventListener('click', function () { if (page + 1 < totalPages) { ++page; loadBooks(); } });
    $('sellerBooksRows').addEventListener('click', function (event) {
      var button = event.target.closest('[data-book-action]');
      if (!button || !canRead()) { return; }
      var book = books.find(function (item) { return String(item.maSach) === button.getAttribute('data-book-id'); });
      if (!book) { return; }
      if (button.dataset.bookAction === 'view') { openBook(book.maSach, true); return; }
      if (!canWrite()) { return; }
      if (button.dataset.bookAction === 'edit') { openBook(book.maSach); }
      if (button.dataset.bookAction === 'stock') { openStock(book); }
      if (button.dataset.bookAction === 'sale') { changeSale(book); }
    });
    $('sellerBookModal').addEventListener('hide.bs.modal', function (event) {
      if (writing && !closingAfterSave) { event.preventDefault(); return; }
      ++detailSequence;
      bookReady = false;
      clearTimeout(titleTimer);
      resetBookImages(null);
      syncControls();
    });
    $('sellerStockModal').addEventListener('hide.bs.modal', function (event) {
      if (writing && !closingAfterSave) { event.preventDefault(); }
    });
    loadStore();
    if (OB.notifications) {
      var idle = function () {
        return !writing && !storeLoading && !document.querySelector('.modal.show') && $('storeForm').classList.contains('d-none');
      };
      OB.notifications.refreshOn(['CUA_HANG_DUOC_DUYET', 'CUA_HANG_BI_TU_CHOI'], loadStore, idle);
      OB.notifications.refreshOn(['SACH_DUOC_DUYET', 'SACH_BI_TU_CHOI'], loadBooks, idle);
    }
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); }
  else { init(); }
})();
