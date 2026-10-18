(function () {
    'use strict';

    /*
     * auth.js được load trong footer.
     * Vì vậy profile.js sử dụng window.OldBook sau khi auth.js khởi tạo.
     */

    var OB = window.OldBook;

    if (!OB) {
        console.error('OldBook chưa được khởi tạo.');
        return;
    }

    var Auth = OB.auth;


    var form = document.getElementById('profileForm');

    if (!form) {
        return;
    }


    var emailInput = document.getElementById('profileEmail');
    var hoTenInput = document.getElementById('profileHoTen');
    var phoneInput = document.getElementById('profileSoDienThoai');
    var avatarUrlInput = document.getElementById('profileAvatarUrl');
    var tieuSuInput = document.getElementById('profileTieuSu');

    var avatarImg = document.getElementById('profileAvatar');
    var avatarPlaceholder = document.getElementById('profileAvatarPlaceholder');

    var headerName = document.getElementById('profileHeaderName');
    var headerEmail = document.getElementById('profileHeaderEmail');

    var saveButton = document.getElementById('profileSaveBtn');
    var cancelButton = document.getElementById('profileCancelBtn');

    var bioCounter = document.getElementById('profileBioCounter');


    var originalProfile = null;


    /*
     * =========================
     * HIỂN THỊ AVATAR
     * =========================
     */

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
    }


    /*
     * Nếu URL ảnh lỗi -> quay về avatar mặc định.
     */

    avatarImg.addEventListener('error', function () {

        avatarImg.removeAttribute('src');

        avatarImg.classList.add('d-none');
        avatarPlaceholder.classList.remove('d-none');
    });


    /*
     * =========================
     * COUNTER TIỂU SỬ
     * =========================
     */

    function updateBioCounter() {

        var length = tieuSuInput.value.length;

        bioCounter.textContent = length + '/1000';
    }

    tieuSuInput.addEventListener('input', updateBioCounter);


    /*
     * =========================
     * LOAD PROFILE
     * =========================
     */

    function loadProfile() {

        setLoading(true);

        OB.request('GET', '/api/profile')
            .then(function (profile) {

                if (!profile) {
                    throw new Error('Không nhận được dữ liệu hồ sơ.');
                }

                originalProfile = {
                    maND: profile.maND,
                    email: profile.email || '',
                    hoTen: profile.hoTen || '',
                    soDienThoai: profile.soDienThoai || '',
                    avatarUrl: profile.avatarUrl || '',
                    tieuSu: profile.tieuSu || ''
                };


                emailInput.value = originalProfile.email;
                hoTenInput.value = originalProfile.hoTen;
                phoneInput.value = originalProfile.soDienThoai;
                avatarUrlInput.value = originalProfile.avatarUrl;
                tieuSuInput.value = originalProfile.tieuSu;


                headerName.textContent =
                    originalProfile.hoTen || 'Tài khoản';

                headerEmail.textContent =
                    originalProfile.email || '';


                renderAvatar(originalProfile.avatarUrl);

                updateBioCounter();

                clearValidation();

            })
            .catch(function (err) {

                if (OB.auth.handleAuthError(err)) {
                    return;
                }

                OB.toast(
                    err.message || 'Không thể tải hồ sơ.',
                    'danger'
                );

            })
            .finally(function () {
                setLoading(false);
            });
    }


    /*
     * =========================
     * VALIDATE FRONTEND
     * =========================
     */

    function validateForm() {

        var hoTen = hoTenInput.value.trim();
        var phone = phoneInput.value.trim();

        clearValidation();

        if (!hoTen) {

            hoTenInput.classList.add('is-invalid');

            document.getElementById('hoTenError').textContent =
                'Họ tên không được để trống.';

            hoTenInput.focus();

            return false;
        }


        if (hoTen.length > 255) {

            hoTenInput.classList.add('is-invalid');

            document.getElementById('hoTenError').textContent =
                'Họ tên không được vượt quá 255 ký tự.';

            hoTenInput.focus();

            return false;
        }


        if (phone) {

            var phoneRegex = /^0\d{9,10}$/;

            if (!phoneRegex.test(phone)) {

                phoneInput.classList.add('is-invalid');

                OB.toast(
                    'Số điện thoại không hợp lệ.',
                    'danger'
                );

                phoneInput.focus();

                return false;
            }
        }


        return true;
    }


    function clearValidation() {

        hoTenInput.classList.remove('is-invalid');
        phoneInput.classList.remove('is-invalid');
    }


    /*
     * =========================
     * SAVE
     * =========================
     */

    form.addEventListener('submit', function (event) {

        event.preventDefault();

        if (!validateForm()) {
            return;
        }


        var payload = {

            hoTen: hoTenInput.value.trim(),

            soDienThoai:
                phoneInput.value.trim() || null,

            avatarUrl:
                avatarUrlInput.value.trim() || null,

            tieuSu:
                tieuSuInput.value.trim() || null
        };


        setLoading(true);


        OB.request(
            'PUT',
            '/api/profile',
            payload
        )
            .then(function (profile) {

                if (!profile) {
                    throw new Error('Không nhận được dữ liệu sau khi lưu.');
                }


                originalProfile = {
                    maND: profile.maND,
                    email: profile.email || '',
                    hoTen: profile.hoTen || '',
                    soDienThoai: profile.soDienThoai || '',
                    avatarUrl: profile.avatarUrl || '',
                    tieuSu: profile.tieuSu || ''
                };


                emailInput.value = originalProfile.email;
                hoTenInput.value = originalProfile.hoTen;
                phoneInput.value = originalProfile.soDienThoai;
                avatarUrlInput.value = originalProfile.avatarUrl;
                tieuSuInput.value = originalProfile.tieuSu;


                headerName.textContent =
                    originalProfile.hoTen || 'Tài khoản';

                headerEmail.textContent =
                    originalProfile.email || '';


                renderAvatar(originalProfile.avatarUrl);

                updateBioCounter();

                clearValidation();


                /*
                 * Đồng bộ tên + avatar lên header.
                 * auth.js sẽ gọi lại GET /api/profile và lưu localStorage.
                 */
                Auth.loadProfileName();


                OB.toast(
                    'Cập nhật hồ sơ thành công.',
                    'success'
                );

            })
            .catch(function (err) {

                if (OB.auth.handleAuthError(err)) {
                    return;
                }

                OB.toast(
                    err.message || 'Không thể cập nhật hồ sơ.',
                    'danger'
                );

            })
            .finally(function () {

                setLoading(false);
            });

    });


    /*
     * =========================
     * HỦY THAY ĐỔI
     * =========================
     */

    cancelButton.addEventListener('click', function () {

        if (!originalProfile) {
            return;
        }


        emailInput.value = originalProfile.email;
        hoTenInput.value = originalProfile.hoTen;
        phoneInput.value = originalProfile.soDienThoai;
        avatarUrlInput.value = originalProfile.avatarUrl;
        tieuSuInput.value = originalProfile.tieuSu;


        headerName.textContent =
            originalProfile.hoTen || 'Tài khoản';

        headerEmail.textContent =
            originalProfile.email || '';


        renderAvatar(originalProfile.avatarUrl);

        updateBioCounter();

        clearValidation();


        OB.toast(
            'Đã hủy các thay đổi chưa lưu.',
            'info'
        );
    });


    /*
     * =========================
     * LOADING STATE
     * =========================
     */

    function setLoading(loading) {

        saveButton.disabled = loading;
        cancelButton.disabled = loading;


        if (loading) {

            saveButton.innerHTML =
                '<span class="spinner-border spinner-border-sm me-2"></span>' +
                'Đang xử lý...';

        } else {

            saveButton.innerHTML =
                '<i class="bi bi-check2-circle me-1"></i>' +
                'Xác nhận & lưu';
        }
    }


    /*
     * Khởi động trang
     */

    loadProfile();

})();