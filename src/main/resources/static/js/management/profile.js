// Trang Hồ sơ của tôi trong cổng Quản lý: sửa họ tên, số điện thoại, tiểu sử và tải ảnh đại diện lên.
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('pfForm')) { return; }

  var Auth = OB.auth;
  var MAX_AVATAR_SIZE = 10 * 1024 * 1024;
  var original = null;
  var busy = false;
  var avatarFile = null;
  var avatarUploadedUrl = null;
  var avatarCurrentUrl = '';
  var previewUrl = null;

  function $(id) { return document.getElementById(id); }
  function show(id, on) { $(id).classList.toggle('d-none', !on); }

  function releasePreview() {
    if (previewUrl) { URL.revokeObjectURL(previewUrl); previewUrl = null; }
  }

  function renderAvatar(url) {
    if (url) {
      $('pfAvatar').src = url;
    } else {
      $('pfAvatar').removeAttribute('src');
    }
    show('pfAvatar', !!url);
    show('pfAvatarPlaceholder', !url);
    $('pfAvatarRemove').disabled = busy || !url;
  }

  function setAvatarError(message) {
    $('pfAvatarError').textContent = message || '';
    show('pfAvatarError', !!message);
  }

  function resetAvatar(url) {
    releasePreview();
    avatarFile = null;
    avatarUploadedUrl = null;
    avatarCurrentUrl = url || '';
    $('pfAvatarFile').value = '';
    setAvatarError('');
    renderAvatar(avatarCurrentUrl);
  }

  function roleLabel(role) { return (OB.roleLabels && OB.roleLabels[role]) || role; }

  function fill(profile) {
    original = {
      hoTen: profile.hoTen || '',
      soDienThoai: profile.soDienThoai || '',
      avatarUrl: profile.avatarUrl || '',
      tieuSu: profile.tieuSu || '',
      email: profile.email || '',
      taiKhoans: profile.taiKhoans || []
    };
    $('pfName').textContent = original.hoTen || 'Chưa đặt tên';
    $('pfEmail').textContent = original.email || '—';
    $('pfRoles').innerHTML = original.taiKhoans.map(function (tk) {
      return '<span class="badge text-bg-light border">' + OB.escapeHtml(roleLabel(tk.vaiTro)) + '</span>';
    }).join('');
    $('pfHoTen').value = original.hoTen;
    $('pfPhone').value = original.soDienThoai;
    $('pfEmailInput').value = original.email;
    $('pfBio').value = original.tieuSu;
    $('pfBioCounter').textContent = String(original.tieuSu.length);
    $('pfHoTen').classList.remove('is-invalid');
    $('pfPhone').classList.remove('is-invalid');
    resetAvatar(original.avatarUrl);
  }

  function setBusy(on) {
    busy = on;
    ['pfHoTen', 'pfPhone', 'pfBio', 'pfAvatarFile', 'pfSave', 'pfCancel'].forEach(function (id) { $(id).disabled = on; });
    $('pfAvatarRemove').disabled = on || (!avatarFile && !avatarCurrentUrl);
    $('pfSave').innerHTML = on
      ? '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Đang lưu...'
      : 'Lưu hồ sơ';
  }

  function load() {
    show('pfError', false);
    show('pfLoading', true);
    OB.request('GET', '/api/profile').then(function (profile) {
      show('pfLoading', false);
      fill(profile || {});
      show('pfContent', true);
      if (OB.managerShell) { OB.managerShell.touch(); }
    }, function (err) {
      show('pfLoading', false);
      if (Auth.handleAuthError && Auth.handleAuthError(err)) { return; }
      $('pfErrorText').textContent = err.message || 'Không tải được hồ sơ.';
      show('pfError', true);
    });
  }

  function onAvatarSelected() {
    if (busy) { return; }
    var file = $('pfAvatarFile').files && $('pfAvatarFile').files[0];
    if (!file) { return; }
    var message = '';
    if (['image/jpeg', 'image/png', 'image/webp'].indexOf(file.type) === -1) {
      message = 'Vui lòng chọn ảnh JPG, PNG hoặc WebP.';
    } else if (!file.size || file.size > MAX_AVATAR_SIZE) {
      message = 'Ảnh phải có dữ liệu và không vượt quá 10 MB.';
    }
    if (message) {
      $('pfAvatarFile').value = '';
      setAvatarError(message);
      return;
    }
    releasePreview();
    avatarFile = file;
    avatarUploadedUrl = null;
    setAvatarError('');
    previewUrl = URL.createObjectURL(file);
    renderAvatar(previewUrl);
  }

  function uploadAvatar() {
    if (!avatarFile) { return Promise.resolve(avatarCurrentUrl || null); }
    if (avatarUploadedUrl) { return Promise.resolve(avatarUploadedUrl); }
    var data = new FormData();
    data.append('file', avatarFile, avatarFile.name);
    return OB.request('POST', '/api/profile/avatar', data).then(function (result) {
      if (!result || typeof result.avatarUrl !== 'string' || !/^https:\/\//i.test(result.avatarUrl)) {
        throw new Error('Không nhận được đường dẫn ảnh đã tải lên.');
      }
      avatarUploadedUrl = result.avatarUrl;
      return avatarUploadedUrl;
    });
  }

  function validate() {
    var ok = true;
    var hoTen = $('pfHoTen').value.trim();
    $('pfHoTenError').textContent = hoTen ? '' : 'Họ tên không được để trống.';
    $('pfHoTen').classList.toggle('is-invalid', !hoTen);
    if (!hoTen) { ok = false; }
    var phone = $('pfPhone').value.trim();
    var phoneBad = !!phone && !/^0\d{9,10}$/.test(phone);
    $('pfPhone').classList.toggle('is-invalid', phoneBad);
    if (phoneBad) { ok = false; }
    return ok;
  }

  function save(e) {
    e.preventDefault();
    if (busy || !original || !validate()) { return; }
    var payload = {
      hoTen: $('pfHoTen').value.trim(),
      soDienThoai: $('pfPhone').value.trim() || null,
      tieuSu: $('pfBio').value.trim() || null
    };
    setBusy(true);
    uploadAvatar().then(function (avatarUrl) {
      payload.avatarUrl = avatarUrl;
      return OB.request('PUT', '/api/profile', payload);
    }).then(function (profile) {
      fill(profile || {});
      OB.toast('Đã lưu hồ sơ.', 'success');
      return Auth.loadProfileName();
    }).catch(function (err) {
      if (Auth.handleAuthError && Auth.handleAuthError(err)) { return; }
      OB.toast(err.message || 'Không lưu được hồ sơ.', 'danger', { duration: 5000 });
    }).then(function () { setBusy(false); });
  }

  function leave() {
    releasePreview();
    document.removeEventListener('manager:leave', leave);
  }

  function init() {
    if (!Auth.isLoggedIn()) { return; }
    $('pfForm').addEventListener('submit', save);
    $('pfCancel').addEventListener('click', function () { if (!busy && original) { fill(original); } });
    $('pfAvatarFile').addEventListener('change', onAvatarSelected);
    $('pfAvatarRemove').addEventListener('click', function () { if (!busy) { resetAvatar(''); } });
    $('pfAvatar').addEventListener('error', function () { renderAvatar(''); });
    $('pfBio').addEventListener('input', function () { $('pfBioCounter').textContent = String($('pfBio').value.length); });
    $('pfRetry').addEventListener('click', load);
    document.addEventListener('manager:leave', leave);
    load();
  }

  init();
})();
