(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || OB.reviews) { return; }
  var esc = OB.escapeHtml;
  var MB = 1024 * 1024;
  var mediaFormats = {
    'image/jpeg': { extension: /\.(jpe?g)$/i, type: 'IMAGE', limit: 10 * MB },
    'image/png': { extension: /\.png$/i, type: 'IMAGE', limit: 10 * MB },
    'image/webp': { extension: /\.webp$/i, type: 'IMAGE', limit: 10 * MB },
    'video/mp4': { extension: /\.mp4$/i, type: 'VIDEO', limit: 50 * MB },
    'video/webm': { extension: /\.webm$/i, type: 'VIDEO', limit: 50 * MB }
  };

  function mediaError(files) {
    if (files.length > 5) { return 'Bạn chỉ có thể đính kèm tối đa 5 ảnh hoặc video.'; }
    var total = 0;
    for (var i = 0; i < files.length; i++) {
      var file = files[i];
      var format = Object.prototype.hasOwnProperty.call(mediaFormats, file.type) ? mediaFormats[file.type] : null;
      if (!format || !format.extension.test(file.name)) {
        return 'Chỉ hỗ trợ ảnh JPG, PNG, WEBP và video MP4, WEBM.';
      }
      if (!file.size) { return 'Tệp đính kèm không được trống.'; }
      if (file.size > format.limit) {
        return format.type === 'IMAGE' ? 'Mỗi ảnh không được lớn hơn 10 MB.' : 'Mỗi video không được lớn hơn 50 MB.';
      }
      total += file.size;
    }
    return total > 60 * MB ? 'Tổng dung lượng ảnh và video không được lớn hơn 60 MB.' : '';
  }

  function cloudinaryUrl(value) {
    if (typeof value !== 'string') { return ''; }
    try {
      var url = new window.URL(value);
      return url.protocol === 'https:' && url.hostname === 'res.cloudinary.com' &&
        !url.username && !url.password && !url.port ? url.href : '';
    } catch (error) { return ''; }
  }

  function renderMedia(media) {
    if (!Array.isArray(media)) { return ''; }
    var content = media.slice(0, 5).map(function (attachment) {
      if (!attachment) { return ''; }
      var url = cloudinaryUrl(attachment.mediaUrl);
      if (!url) { return ''; }
      if (attachment.loaiMedia === 'IMAGE') {
        return '<a class="review-media-item" href="' + esc(url) + '" target="_blank" rel="noopener noreferrer" aria-label="Xem ảnh sách khách hàng đã nhận">' +
          '<img src="' + esc(url) + '" alt="Ảnh sách khách hàng đã nhận" loading="lazy"></a>';
      }
      return attachment.loaiMedia === 'VIDEO' ? '<video class="review-media-item" src="' + esc(url) +
        '" controls preload="metadata" playsinline aria-label="Video sách khách hàng đã nhận"></video>' : '';
    }).join('');
    return content ? '<div class="review-media mt-2 mb-2">' + content + '</div>' : '';
  }

  function eligible(item, status) {
    return status === 'DA_GIAO' && item.coTheDanhGia === true && !item.danhGia;
  }

  function render(item, status) {
    var review = item.danhGia;
    if (review) {
      var stars = '';
      for (var i = 1; i <= 5; i++) {
        stars += '<i class="bi bi-star' + (i <= Number(review.soSao) ? '-fill' : '') + '" aria-hidden="true"></i>';
      }
      return '<div class="order-review mt-2"><div class="small fw-semibold">Đánh giá của bạn ' +
        '<span class="rating-stars ms-1" role="img" aria-label="' + esc(review.soSao) + ' trên 5 sao">' + stars + '</span></div>' +
        '<p class="order-review-comment small mb-1">' + esc(review.binhLuan || '') + '</p>' +
        renderMedia(review.media) +
        '<a class="small" href="/books/' + encodeURIComponent(item.maSach) + '#customerReviews">Xem đánh giá trên trang sách</a></div>';
    }
    return eligible(item, status) ? '<button type="button" class="btn-order-cancel mt-2" aria-label="Đánh giá sách ' + esc(item.tenSach) + '" data-review-order-item="' + esc(item.maCTDH) +
      '"><i class="bi bi-star me-1" aria-hidden="true"></i>Đánh giá sách</button>' : '';
  }

  function bind(options) {
    var modalElement = document.getElementById('orderReviewModal');
    var form = document.getElementById('orderReviewForm');
    if (!modalElement || !form) { return; }
    var modal = bootstrap.Modal.getOrCreateInstance(modalElement);
    var comment = document.getElementById('orderReviewComment');
    var title = document.getElementById('orderReviewBookTitle');
    var error = document.getElementById('orderReviewError');
    var counter = document.getElementById('orderReviewCounter');
    var submit = document.getElementById('orderReviewSubmit');
    var fileInput = document.getElementById('orderReviewFiles');
    var previews = document.getElementById('orderReviewPreviews');
    var mediaCounter = document.getElementById('orderReviewMediaCounter');
    var radios = form.querySelectorAll('input[name="reviewStars"]');
    var itemId = null, saving = false, opener = null;
    var selectedMedia = [];

    function renderPreviews() {
      previews.innerHTML = selectedMedia.map(function (attachment, index) {
        var label = attachment.file.name;
        var content = attachment.type === 'IMAGE' ? '<img src="' + esc(attachment.url) + '" alt="' + esc(label) + '">' :
          '<video src="' + esc(attachment.url) + '" controls preload="metadata" playsinline aria-label="' + esc(label) + '"></video>';
        return '<div class="review-media-preview">' + content +
          '<div class="review-media-caption"><span title="' + esc(label) + '">' + esc(label) + '</span>' +
          '<button type="button" class="btn btn-sm btn-outline-secondary" data-review-remove="' + index +
          '" aria-label="Xóa tệp ' + esc(label) + '"><i class="bi bi-x-lg" aria-hidden="true"></i></button></div></div>';
      }).join('');
      previews.classList.toggle('d-none', selectedMedia.length === 0);
      mediaCounter.textContent = selectedMedia.length + '/5 tệp đã chọn';
    }

    function clearMedia() {
      selectedMedia.forEach(function (attachment) { window.URL.revokeObjectURL(attachment.url); });
      selectedMedia = [];
      fileInput.value = '';
      renderPreviews();
    }

    function showError(message, control) {
      error.textContent = message;
      error.classList.remove('d-none');
      if (control) { control.focus(); }
    }

    function setSaving(value) {
      saving = value;
      form.setAttribute('aria-busy', String(value));
      modalElement.querySelectorAll('input, textarea, button').forEach(function (control) { control.disabled = value; });
      submit.textContent = value ? 'Đang gửi đánh giá...' : 'Gửi đánh giá';
    }

    function updateCounter() { counter.textContent = comment.value.length + '/2000 ký tự'; }
    comment.addEventListener('input', updateCounter);
    fileInput.addEventListener('change', function () {
      if (saving) { return; }
      var added = Array.prototype.slice.call(fileInput.files || []);
      fileInput.value = '';
      if (!added.length) { return; }
      var message = mediaError(selectedMedia.map(function (attachment) { return attachment.file; }).concat(added));
      if (message) { showError(message, fileInput); return; }
      added.forEach(function (file) {
        selectedMedia.push({ file: file, type: mediaFormats[file.type].type, url: window.URL.createObjectURL(file) });
      });
      error.classList.add('d-none');
      renderPreviews();
    });
    previews.addEventListener('click', function (event) {
      var button = event.target.closest('[data-review-remove]');
      if (!button || saving) { return; }
      var index = Number(button.getAttribute('data-review-remove'));
      if (!selectedMedia[index]) { return; }
      window.URL.revokeObjectURL(selectedMedia[index].url);
      selectedMedia.splice(index, 1);
      renderPreviews();
      fileInput.focus();
    });
    window.addEventListener('pagehide', clearMedia);

    options.container.addEventListener('click', function (event) {
      var button = event.target.closest('[data-review-order-item]');
      if (!button || saving) { return; }
      var id = button.getAttribute('data-review-order-item');
      var entry = options.findItem(id);
      if (!entry || !eligible(entry.item, entry.status)) { return; }
      itemId = id;
      opener = button;
      title.textContent = entry.item.tenSach;
      radios.forEach(function (radio) { radio.checked = false; });
      comment.value = '';
      clearMedia();
      error.classList.add('d-none');
      error.textContent = '';
      updateCounter();
      setSaving(false);
      modal.show();
    });

    modalElement.addEventListener('shown.bs.modal', function () { radios[0].focus(); });
    modalElement.addEventListener('hide.bs.modal', function (event) {
      if (saving) { event.preventDefault(); }
    });
    modalElement.addEventListener('hidden.bs.modal', function () {
      itemId = null;
      clearMedia();
      var connected = false;
      options.container.querySelectorAll('[data-review-order-item]').forEach(function (button) {
        if (button === opener) { connected = true; }
      });
      if (connected) { opener.focus(); }
      else {
        var alert = document.getElementById('ordersAlert');
        (alert.classList.contains('d-none') ? document.getElementById('ordersTitle') : alert).focus();
      }
      opener = null;
    });
    modalElement.querySelectorAll('[data-review-close]').forEach(function (button) {
      button.addEventListener('click', function () { if (!saving) { modal.hide(); } });
    });

    form.addEventListener('submit', function (event) {
      event.preventDefault();
      if (saving || itemId === null) { return; }
      error.classList.add('d-none');
      var stars = 0;
      radios.forEach(function (radio) { if (radio.checked) { stars = Number(radio.value); } });
      var text = comment.value.trim();
      if (stars < 1 || stars > 5 || Math.floor(stars) !== stars) {
        showError('Vui lòng chọn từ 1 đến 5 sao.', radios[0]);
        return;
      }
      if (!text) { showError('Vui lòng nhập bình luận về sách.', comment); return; }
      if (text.length > 2000) { showError('Bình luận không được dài quá 2000 ký tự.', comment); return; }
      var files = selectedMedia.map(function (attachment) { return attachment.file; });
      var attachmentError = mediaError(files);
      if (attachmentError) { showError(attachmentError, fileInput); return; }
      var body = { soSao: stars, binhLuan: text };
      if (files.length) {
        body = new window.FormData();
        body.append('soSao', String(stars));
        body.append('binhLuan', text);
        files.forEach(function (file) { body.append('files', file); });
      }
      var submittedItemId = itemId;
      setSaving(true);
      OB.request('POST', '/api/reviews/order-items/' + encodeURIComponent(submittedItemId), body)
        .then(function (review) {
          setSaving(false);
          options.onSaved(submittedItemId, review);
          modal.hide();
        }).catch(function (err) {
          setSaving(false);
          if (OB.auth.handleAuthError(err, OB.ROLE_CUSTOMER)) { return; }
          showError(err.message || 'Chưa thể gửi đánh giá. Vui lòng thử lại.');
        });
    });
  }

  OB.reviews = { eligible: eligible, render: render, bind: bind };
})();
