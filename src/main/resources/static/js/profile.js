(function () {
    'use strict';

    var OB = window.OldBook;
    if (!OB) {
        console.error('OldBook chưa được khởi tạo.');
        return;
    }

    var form = document.getElementById('profileForm');
    if (!form) { return; }

    var emailInput = document.getElementById('profileEmail');
    var hoTenInput = document.getElementById('profileHoTen');
    var phoneInput = document.getElementById('profileSoDienThoai');
    var avatarFileInput = document.getElementById('profileAvatarFile');
    var avatarError = document.getElementById('profileAvatarError');
    var removeAvatarButton = document.getElementById('profileAvatarRemoveBtn');
    var tieuSuInput = document.getElementById('profileTieuSu');
    var avatarImg = document.getElementById('profileAvatar');
    var avatarPlaceholder = document.getElementById('profileAvatarPlaceholder');
    var headerName = document.getElementById('profileHeaderName');
    var headerEmail = document.getElementById('profileHeaderEmail');
    var saveButton = document.getElementById('profileSaveBtn');
    var cancelButton = document.getElementById('profileCancelBtn');
    var bioCounter = document.getElementById('profileBioCounter');
    var originalProfile = null;
    var selectedAvatarFile = null;
    var uploadedAvatarUrl = null;
    var currentAvatarUrl = '';
    var previewUrl = null;
    var busy = false;
    var maxAvatarSize = 10 * 1024 * 1024;

    function renderAvatar(url) {
        if (url && url.trim()) {
            avatarImg.src = url.trim();
            avatarImg.classList.remove('d-none');
            avatarPlaceholder.classList.add('d-none');
        } else {
            avatarImg.removeAttribute('src');
            avatarImg.classList.add('d-none');
            avatarPlaceholder.classList.remove('d-none');
        }
        removeAvatarButton.disabled = busy || (!selectedAvatarFile && !currentAvatarUrl);
    }

    avatarImg.addEventListener('error', function () {
        avatarImg.removeAttribute('src');
        avatarImg.classList.add('d-none');
        avatarPlaceholder.classList.remove('d-none');
    });

    function releasePreview() {
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            previewUrl = null;
        }
    }

    function resetAvatar(url) {
        releasePreview();
        selectedAvatarFile = null;
        uploadedAvatarUrl = null;
        currentAvatarUrl = url || '';
        avatarFileInput.value = '';
        avatarFileInput.classList.remove('is-invalid');
        avatarError.textContent = '';
        renderAvatar(currentAvatarUrl);
    }

    avatarFileInput.addEventListener('change', function () {
        if (busy) { return; }
        var file = avatarFileInput.files && avatarFileInput.files[0];
        var message = '';
        if (!file) {
            resetAvatar(currentAvatarUrl);
            return;
        }
        if (['image/jpeg', 'image/png', 'image/webp'].indexOf(file.type) === -1) {
            message = 'Vui lòng chọn ảnh JPG, PNG hoặc WebP.';
        } else if (!file.size || file.size > maxAvatarSize) {
            message = 'Ảnh phải có dữ liệu và không vượt quá 10 MB.';
        }
        if (message) {
            resetAvatar(currentAvatarUrl);
            avatarFileInput.classList.add('is-invalid');
            avatarError.textContent = message;
            OB.toast(message, 'danger');
            return;
        }
        releasePreview();
        selectedAvatarFile = file;
        uploadedAvatarUrl = null;
        avatarFileInput.classList.remove('is-invalid');
        avatarError.textContent = '';
        previewUrl = URL.createObjectURL(file);
        renderAvatar(previewUrl);
    });

    removeAvatarButton.addEventListener('click', function () {
        if (!busy) { resetAvatar(''); }
    });

    window.addEventListener('pagehide', releasePreview);

    function updateBioCounter() {
        bioCounter.textContent = tieuSuInput.value.length + '/1000';
    }
    tieuSuInput.addEventListener('input', updateBioCounter);

    function clearValidation() {
        hoTenInput.classList.remove('is-invalid');
        phoneInput.classList.remove('is-invalid');
    }

    function fillProfile(profile) {
        emailInput.value = profile.email;
        hoTenInput.value = profile.hoTen;
        phoneInput.value = profile.soDienThoai;
        tieuSuInput.value = profile.tieuSu;
        headerName.textContent = profile.hoTen || 'Tài khoản';
        headerEmail.textContent = profile.email;
        resetAvatar(profile.avatarUrl);
        updateBioCounter();
        clearValidation();
    }

    function rememberProfile(profile) {
        if (!profile) { throw new Error('Không nhận được dữ liệu hồ sơ.'); }
        originalProfile = {
            maND: profile.maND,
            email: profile.email || '',
            hoTen: profile.hoTen || '',
            soDienThoai: profile.soDienThoai || '',
            avatarUrl: profile.avatarUrl || '',
            tieuSu: profile.tieuSu || ''
        };
        fillProfile(originalProfile);
    }

    function showError(err, fallback) {
        if (!OB.auth.handleAuthError(err)) {
            OB.toast(err.message || fallback, 'danger');
        }
    }

    function setLoading(loading) {
        busy = loading;
        Array.prototype.forEach.call(form.elements, function (control) {
            control.disabled = loading;
        });
        saveButton.disabled = loading || !originalProfile;
        cancelButton.disabled = loading || !originalProfile;
        avatarFileInput.disabled = loading || !originalProfile;
        removeAvatarButton.disabled = loading || (!selectedAvatarFile && !currentAvatarUrl);
        form.setAttribute('aria-busy', String(loading));
        saveButton.innerHTML = loading
            ? '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý...'
            : '<i class="bi bi-check2-circle me-1"></i>Xác nhận & lưu';
    }

    function loadProfile() {
        setLoading(true);
        OB.request('GET', '/api/profile')
            .then(rememberProfile)
            .catch(function (err) { showError(err, 'Không thể tải hồ sơ.'); })
            .finally(function () { setLoading(false); });
    }

    function validateForm() {
        clearValidation();
        var hoTen = hoTenInput.value.trim();
        if (!hoTen || hoTen.length > 255) {
            hoTenInput.classList.add('is-invalid');
            document.getElementById('hoTenError').textContent = !hoTen
                ? 'Họ tên không được để trống.'
                : 'Họ tên không được vượt quá 255 ký tự.';
            hoTenInput.focus();
            return false;
        }
        var phone = phoneInput.value.trim();
        if (phone && !/^0\d{9,10}$/.test(phone)) {
            phoneInput.classList.add('is-invalid');
            OB.toast('Số điện thoại không hợp lệ.', 'danger');
            phoneInput.focus();
            return false;
        }
        return true;
    }

    function uploadAvatar() {
        if (!selectedAvatarFile) { return Promise.resolve(currentAvatarUrl || null); }
        // Reuse the completed upload if saving the profile needs to be retried.
        if (uploadedAvatarUrl) { return Promise.resolve(uploadedAvatarUrl); }
        var data = new FormData();
        data.append('file', selectedAvatarFile, selectedAvatarFile.name);
        return OB.request('POST', '/api/profile/avatar', data).then(function (result) {
            if (!result || typeof result.avatarUrl !== 'string' || !/^https:\/\//i.test(result.avatarUrl)) {
                throw new Error('Không nhận được đường dẫn ảnh đã tải lên.');
            }
            uploadedAvatarUrl = result.avatarUrl;
            return uploadedAvatarUrl;
        });
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (busy || !originalProfile || !validateForm()) { return; }
        var payload = {
            hoTen: hoTenInput.value.trim(),
            soDienThoai: phoneInput.value.trim() || null,
            tieuSu: tieuSuInput.value.trim() || null
        };
        setLoading(true);
        uploadAvatar()
            .then(function (avatarUrl) {
                payload.avatarUrl = avatarUrl;
                return OB.request('PUT', '/api/profile', payload);
            })
            .then(function (profile) {
                rememberProfile(profile);
                OB.auth.loadProfileName();
                OB.toast('Cập nhật hồ sơ thành công.', 'success');
            })
            .catch(function (err) { showError(err, 'Không thể cập nhật hồ sơ.'); })
            .finally(function () { setLoading(false); });
    });

    cancelButton.addEventListener('click', function () {
        if (busy || !originalProfile) { return; }
        fillProfile(originalProfile);
        OB.toast('Đã hủy các thay đổi chưa lưu.', 'info');
    });

    loadProfile();
})();
