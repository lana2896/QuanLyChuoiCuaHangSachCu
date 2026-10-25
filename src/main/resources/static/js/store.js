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
  var bookImages = [];
  var IMAGE_MAX_SIZE = 10 * 1024 * 1024;
  var MAX_IMAGES = 10;
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

  var contentFields = {
    tenSach: 'bookTitle', tacGia: 'bookAuthor', nhaXuatBan: 'bookPublisher', namXuatBan: 'bookYear',
    giaBia: 'bookCoverPrice', giaBanCu: 'bookSalePrice', doMoiPercent: 'bookCondition',
    tinhTrangVatLy: 'bookPhysicalCondition', moTaChiTiet: 'bookDescription'
  };

  function safeImageSrc(url) {
    return url && (/^blob:/.test(url) || /^https?:\/\//i.test(url) || /^\/(?!\/)/.test(url)) ? url : '';
  }

  function releaseImagePreviews() {
    bookImages.forEach(function (image) {
      if (image.preview) { window.URL.revokeObjectURL(image.preview); image.preview = null; }
    });
  }

  function renderBookImages() {
    $('bookImageList').innerHTML = bookImages.map(function (image, index) {
      var src = safeImageSrc(image.preview || image.url);
      var actions = bookReadOnly ? '' :
        '<button type="button" class="btn btn-outline-danger btn-sm py-0 px-1 mt-1" data-image-action="remove" data-image-index="' + index + '">Xóa</button>';
      return '<div class="col-6 col-md-3"><div class="border rounded p-1 h-100">' +
        '<img class="w-100 rounded" src="' + esc(src) + '" alt="Ảnh ' + (index + 1) + '" style="aspect-ratio:3/4;object-fit:cover">' +
        '<div class="small fw-semibold mt-1">Ảnh ' + (index + 1) + '</div>' + actions + '</div></div>';
    }).join('');
    visible('bookImagePreviewPanel', bookImages.length > 0);
  }

  function resetBookImages(urls) {
    releaseImagePreviews();
    bookImages = (urls || []).map(function (url) { return { url: url, file: null, preview: null }; });
    $('bookImageFile').value = '';
    $('bookImageFile').setCustomValidity('');
    error('bookImageError', '');
    visible('bookImageUploading', false);
    renderBookImages();
  }

  function imageValidationMessage(file) {
    if (['image/jpeg', 'image/png', 'image/webp'].indexOf(file.type) === -1) {
      return 'Vui lòng chọn ảnh JPEG, PNG hoặc WebP.';
    }
    if (!file.size) { return 'Ảnh không được để trống.'; }
    if (file.size > IMAGE_MAX_SIZE) { return 'Ảnh không được vượt quá 10 MB.'; }
    return '';
  }

  function selectBookImages() {
    if (bookReadOnly || !canWrite() || !bookReady) { return; }
    var files = Array.prototype.slice.call($('bookImageFile').files);
    $('bookImageFile').value = '';
    var problem = '';
    files.forEach(function (file) {
      var validation = imageValidationMessage(file);
      if (!validation && bookImages.length >= MAX_IMAGES) { validation = 'Mỗi sách chỉ được tối đa ' + MAX_IMAGES + ' ảnh.'; }
      if (validation) { problem = problem || validation; return; }
      bookImages.push({ url: null, file: file, preview: window.URL.createObjectURL(file) });
    });
    error('bookImageError', problem);
    renderBookImages();
  }

  function changeBookImages(event) {
    var button = event.target.closest('[data-image-action]');
    if (!button || bookReadOnly || !canWrite() || !bookReady) { return; }
    var index = Number(button.getAttribute('data-image-index'));
    if (!bookImages[index]) { return; }
    var removed = bookImages.splice(index, 1)[0];
    if (removed.preview) { window.URL.revokeObjectURL(removed.preview); }
    error('bookImageError', '');
    renderBookImages();
  }

  function pendingImages() {
    return bookImages.filter(function (image) { return !image.url; });
  }

  function uploadBookImage(image) {
    if (image.url) { return Promise.resolve(image.url); }
    var data = new window.FormData();
    data.append('file', image.file);
    return OB.request('POST', BASE + '/images', data).then(function (result) {
      if (!result || typeof result.hinhAnhUrl !== 'string' || !/^https:\/\//i.test(result.hinhAnhUrl)) {
        throw new Error('Phản hồi tải ảnh không hợp lệ. Vui lòng thử lại.');
      }
      image.url = result.hinhAnhUrl;
      return image.url;
    });
  }

  function uploadBookImages() {
    visible('bookImageUploading', true);
    return pendingImages().reduce(function (chain, image) {
      return chain.then(function () { return uploadBookImage(image); });
    }, Promise.resolve()).finally(function () { visible('bookImageUploading', false); });
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
    return Promise.all([readOnly ? Promise.resolve(null) : categories(), id === null ? Promise.resolve(null) : OB.request('GET', BASE + '/' + encodeURIComponent(id))])
      .then(function (results) {
        if (sequence !== detailSequence) { return; }
        var items = results[0];
        var book = results[1];
        if (id !== null && (!book || String(book.maSach) !== String(id))) { throw new Error('Không tải được thông tin sách. Vui lòng thử lại.'); }
        $('bookCategory').innerHTML = readOnly
          ? '<option value="' + esc(book.maDM) + '">' + esc(book.tenDanhMuc || 'Chưa có danh mục') + '</option>'
          : '<option value="">Chọn danh mục</option>' + items.map(function (item) {
            return '<option value="' + esc(item.maDM) + '">' + esc(item.tenDanhMuc) + '</option>';
          }).join('');
        if (book) {
          Object.keys(contentFields).forEach(function (key) { setValue(contentFields[key], book[key]); });
          resetBookImages(book.hinhAnhUrls);
          setValue('bookCategory', book.maDM);
          $('bookModalTitle').textContent = (readOnly ? 'Chi tiết: ' : 'Sửa nội dung: ') + book.tenSach;
          if (readOnly) {
            var saleStatus = book.trangThaiBan === 'DANG_BAN' ? 'Đang bán' : (book.trangThaiBan === 'NGUNG_BAN' ? 'Ngừng bán' : 'Chưa xác định');
            $('bookViewStatus').innerHTML = badge(book.trangThaiDuyet) + ' <span class="badge text-bg-secondary">' + esc(saleStatus) + '</span>';
            $('bookViewStock').textContent = 'Tồn kho: ' + (book.soLuongTon === null || book.soLuongTon === undefined ? 'Chưa xác định' : book.soLuongTon);
            $('bookViewRejection').textContent = 'Lý do từ chối: ' + (book.lyDoTuChoi || 'Chưa có lý do được cung cấp.');
            visible('bookViewRejection', book.trangThaiDuyet === 'TU_CHOI' || !!book.lyDoTuChoi);
            visible('bookViewDetails', true);
          }
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
    $('sellerBookForm').reset();
    resetBookImages([]);
    $('bookCategory').innerHTML = '<option value="">Đang tải danh mục...</option>';
    $('bookModalTitle').textContent = bookReadOnly ? 'Chi tiết sách' : (editingBookId === null ? 'Thêm sách mới' : 'Sửa nội dung sách');
    visible('bookSubmissionNotice', !bookReadOnly);
    visible('bookViewDetails', false);
    visible('saveBook', !bookReadOnly);
    $('bookModalClose').textContent = bookReadOnly ? 'Đóng' : 'Hủy';
    visible('initialStockGroup', editingBookId === null);
    $('bookInitialStock').required = editingBookId === null;
    $('bookInitialStock').disabled = editingBookId !== null;
    $('saveBook').textContent = editingBookId === null ? 'Gửi sách để duyệt' : 'Lưu và gửi duyệt lại';
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
    if (bookReadOnly || !canWrite() || !bookReady || !$('sellerBookForm').reportValidity()) { return; }
    var body = { maDM: Number($('bookCategory').value), tenSach: nullableText('bookTitle'),
      tacGia: nullableText('bookAuthor'), nhaXuatBan: nullableText('bookPublisher'), namXuatBan: nullableNumber('bookYear'),
      giaBia: nullableNumber('bookCoverPrice'), giaBanCu: nullableNumber('bookSalePrice'), doMoiPercent: nullableNumber('bookCondition'),
      tinhTrangVatLy: nullableText('bookPhysicalCondition'), moTaChiTiet: nullableText('bookDescription'), hinhAnhUrls: [] };
    if (!body.tenSach) { error('bookFormError', 'Tên sách không được để trống.'); return; }
    if (editingBookId === null) { body.soLuongTon = Number($('bookInitialStock').value); }
    var url = BASE + (editingBookId === null ? '' : '/' + encodeURIComponent(editingBookId));
    write(editingBookId === null ? 'POST' : 'PUT', url, body, 'bookFormError', function () {
      closeSavedModal(bookModal);
      OB.toast('Nội dung sách đã được gửi để duyệt.', 'success');
      return loadBooks();
    }, function () {
      return uploadBookImages().then(function () {
        body.hinhAnhUrls = bookImages.map(function (image) { return image.url; });
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
    $('bookImageList').addEventListener('click', changeBookImages);
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
      resetBookImages([]);
      syncControls();
    });
    $('sellerStockModal').addEventListener('hide.bs.modal', function (event) {
      if (writing && !closingAfterSave) { event.preventDefault(); }
    });
    loadStore();
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); }
  else { init(); }
})();
