(function () {
    'use strict';

    var OB = window.OldBook;
    if (!OB) {
        return;
    }

    var Auth = OB.auth;
    var config = window.adminProfilePage || {};
    var isSelf = config.mode !== 'user';
    var targetId = isSelf
        ? (Auth.getUser() && Auth.getUser().maND)
        : Number(config.targetId);

    var $ = function (id) { return document.getElementById(id); };

    var roleLabels = OB.roleLabels || {};
    var statusLabels = {
        HOAT_DONG: 'Hoạt động',
        CHO_XAC_THUC: 'Chờ xác thực',
        BI_KHOA: 'Bị khóa'
    };
    var storeStatusLabels = {
        CHO_DUYET: 'Chờ duyệt',
        DA_DUYET: 'Đã duyệt',
        TU_CHOI: 'Từ chối'
    };

    var profile = null;
    var busy = false;

    function show(id) { $(id).classList.remove('hidden'); }
    function hide(id) { $(id).classList.add('hidden'); }
    function text(id, value) { $(id).textContent = value == null || value === '' ? '—' : String(value); }

    function formatDate(value) {
        if (!value) { return '—'; }
        var date = new Date(value);
        return isNaN(date.getTime()) ? String(value) : date.toLocaleString('vi-VN');
    }

    function formatDateOnly(value) {
        if (!value) { return '—'; }
        var date = new Date(value);
        return isNaN(date.getTime()) ? String(value) : date.toLocaleDateString('vi-VN');
    }

    function escape(value) {
        return OB.escapeHtml(value == null ? '' : value);
    }

    function getOverallStatus(accounts) {
        if (!accounts || !accounts.length) {
            return { code: 'CHO_XAC_THUC', label: 'Chưa có tài khoản' };
        }
        if (accounts.some(function (account) { return account.trangThai === 'HOAT_DONG'; })) {
            return { code: 'HOAT_DONG', label: 'Hoạt động' };
        }
        if (accounts.some(function (account) { return account.trangThai === 'CHO_XAC_THUC'; })) {
            return { code: 'CHO_XAC_THUC', label: 'Chờ xác thực' };
        }
        return { code: 'BI_KHOA', label: 'Bị khóa' };
    }

    function renderHero() {
        var status = getOverallStatus(profile.taiKhoans);
        text('profileName', profile.hoTen || 'Tài khoản');
        text('profileUid', 'UID: #' + profile.maND);
        text('profileEmail', profile.email);
        text('profilePhone', profile.soDienThoai || 'Chưa cập nhật');
        text('profileCreatedAt', formatDateOnly(profile.ngayTao));
        text('profileAccountCount', (profile.taiKhoans || []).length + ' tài khoản');

        var statusBadge = $('profileStatusBadge');
        statusBadge.textContent = status.label;
        statusBadge.className = status.code === 'BI_KHOA'
            ? 'rounded-full bg-danger-soft px-2.5 py-1 text-xs font-semibold text-mahogany-danger'
            : status.code === 'CHO_XAC_THUC'
                ? 'rounded-full bg-surface-cream px-2.5 py-1 text-xs font-semibold text-olive-grove'
                : 'rounded-full bg-success-soft px-2.5 py-1 text-xs font-semibold text-primary';

        $('profileStatusDot').className = status.code === 'BI_KHOA'
            ? 'absolute bottom-1 right-2 h-5 w-5 rounded-full bg-error ring-4 ring-surface-white'
            : 'absolute bottom-1 right-2 h-5 w-5 rounded-full bg-secondary ring-4 ring-surface-white';

        var roles = [];
        (profile.taiKhoans || []).forEach(function (account) {
            if (roles.indexOf(account.vaiTro) === -1) {
                roles.push(account.vaiTro);
            }
        });

        $('profileRoles').innerHTML = roles.length
            ? roles.map(function (role) {
                return '<span class="rounded-full bg-surface-cream px-3 py-1 text-xs font-semibold text-primary inline-flex items-center gap-1">' +
                    '<span class="material-symbols-outlined text-[15px]">badge</span>' +
                    escape(roleLabels[role] || role) +
                    '</span>';
            }).join('')
            : '<span class="text-xs text-text-muted">Chưa có vai trò</span>';

        if (profile.avatarUrl) {
            $('profileAvatar').src = profile.avatarUrl;
            $('profileAvatar').classList.remove('hidden');
            $('profileAvatarPlaceholder').classList.add('hidden');
        } else {
            $('profileAvatar').removeAttribute('src');
            $('profileAvatar').classList.add('hidden');
            $('profileAvatarPlaceholder').classList.remove('hidden');
        }
    }

    function renderPersonal() {
        text('readonlyHoTen', profile.hoTen || 'Chưa cập nhật');
        text('readonlyEmail', profile.email || 'Chưa cập nhật');
        text('readonlyPhone', profile.soDienThoai || 'Chưa cập nhật');
        text('readonlyBio', profile.tieuSu || 'Chưa có tiểu sử.');

        if (isSelf) {
            $('editProfileButton').classList.remove('hidden');
            $('editHoTen').value = profile.hoTen || '';
            $('editPhone').value = profile.soDienThoai || '';
            $('editAvatarUrl').value = profile.avatarUrl || '';
            $('editTieuSu').value = profile.tieuSu || '';
            updateBioCounter();
        } else {
            $('editProfileButton').classList.add('hidden');
            $('editProfileForm').classList.add('hidden');
        }
    }

    function renderAccounts() {
        var accounts = profile.taiKhoans || [];
        text('accountCountBadge', accounts.length + ' tài khoản');
        if (!accounts.length) {
            $('accountList').innerHTML = '<div class="rounded-xl bg-surface-bright p-4 text-sm text-text-muted">Người dùng chưa có tài khoản đăng nhập.</div>';
            return;
        }

        $('accountList').innerHTML = accounts.map(function (account) {
            var status = statusLabels[account.trangThai] || account.trangThai;
            var statusClass = account.trangThai === 'HOAT_DONG'
                ? 'bg-success-soft text-primary'
                : account.trangThai === 'BI_KHOA'
                    ? 'bg-danger-soft text-mahogany-danger'
                    : 'bg-surface-cream text-olive-grove';

            return '<div class="rounded-xl bg-surface-warm p-4">' +
                '<div class="flex items-start justify-between gap-3">' +
                '<div class="flex items-center gap-3 min-w-0">' +
                '<div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-sand-dune/30 text-primary">' +
                '<span class="material-symbols-outlined text-[20px]">person</span>' +
                '</div>' +
                '<div class="min-w-0">' +
                '<div class="font-bold text-primary">Tài khoản #' + escape(account.maTK) + '</div>' +
                '<div class="text-xs text-olive-grove">Vai trò: ' + escape(roleLabels[account.vaiTro] || account.vaiTro) + '</div>' +
                '</div>' +
                '</div>' +
                '<span class="shrink-0 rounded-full px-2 py-1 text-[11px] font-semibold ' + statusClass + '">' + escape(status) + '</span>' +
                '</div>' +
                '<div class="mt-3 grid grid-cols-2 gap-3 text-xs text-on-surface-variant">' +
                '<div><span class="block text-text-muted">Ngày tạo</span><span>' + escape(formatDate(account.ngayTao)) + '</span></div>' +
                '<div><span class="block text-text-muted">Mã người dùng</span><span>#' + escape(account.maND) + '</span></div>' +
                '</div>' +
                '</div>';
        }).join('');
    }

    function renderStore() {
        if (!profile.cuaHang) {
            hide('storeSection');
            return;
        }

        show('storeSection');
        var store = profile.cuaHang;
        var status = storeStatusLabels[store.trangThaiDuyet] || store.trangThaiDuyet || '—';

        $('storeCard').innerHTML =
            '<div class="rounded-xl bg-surface-bright p-4">' +
            '<div class="flex flex-wrap items-start justify-between gap-3">' +
            '<div>' +
            '<div class="text-base font-bold text-primary">' + escape(store.tenCuaHang) + '</div>' +
            '<div class="mt-1 text-xs text-text-muted">Mã cửa hàng: #' + escape(store.maCH) + '</div>' +
            '</div>' +
            '<span class="rounded-full bg-secondary-container px-2.5 py-1 text-xs font-semibold text-on-secondary-container">' + escape(status) + '</span>' +
            '</div>' +
            '<div class="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">' +
            '<div class="rounded-lg bg-surface-warm p-3"><span class="block text-[11px] font-bold uppercase tracking-wider text-text-muted">Địa chỉ</span><span class="mt-1 block text-sm text-on-surface-variant">' + escape(store.diaChiCH || 'Chưa cập nhật') + '</span></div>' +
            '<div class="rounded-lg bg-surface-warm p-3"><span class="block text-[11px] font-bold uppercase tracking-wider text-text-muted">Số điện thoại</span><span class="mt-1 block text-sm text-on-surface-variant">' + escape(store.soDienThoaiCH || 'Chưa cập nhật') + '</span></div>' +
            '</div>' +
            '</div>';
    }

    function renderAddresses() {
        var addresses = profile.diaChis || [];
        text('addressCountBadge', addresses.length + ' địa chỉ');
        if (!addresses.length) {
            $('addressList').innerHTML = '<div class="lg:col-span-2 rounded-xl bg-surface-bright p-4 text-sm text-text-muted">Người dùng chưa có địa chỉ nhận hàng.</div>';
            return;
        }

        $('addressList').innerHTML = addresses.map(function (address) {
            var fullAddress = [address.diaChiChiTiet, address.phuongXa, address.quanHuyen, address.tinhThanh]
                .filter(function (value) { return value; })
                .join(', ');

            return '<div class="rounded-xl bg-surface-bright p-4 border ' +
                (address.laMacDinh ? 'border-secondary' : 'border-line-border') + '">' +
                '<div class="flex items-center justify-between gap-2">' +
                '<div class="font-semibold text-primary">' + escape(address.tenNguoiNhan || 'Người nhận') + '</div>' +
                (address.laMacDinh ? '<span class="rounded-full bg-success-soft px-2 py-1 text-[11px] font-semibold text-primary">Mặc định</span>' : '') +
                '</div>' +
                '<div class="mt-1 text-sm text-on-surface-variant">' + escape(address.soDienThoai || 'Chưa có số điện thoại') + '</div>' +
                '<div class="mt-2 text-sm leading-6 text-text-muted">' + escape(fullAddress || 'Chưa cập nhật') + '</div>' +
                '</div>';
        }).join('');
    }

    function renderActivities() {
        var activities = profile.hoatDongGanDay || [];
        if (!activities.length) {
            $('activityList').innerHTML = '<div class="rounded-xl bg-surface-bright p-4 text-sm text-text-muted">Chưa có nhật ký hoạt động.</div>';
            return;
        }

        $('activityList').innerHTML = '<div class="relative ml-2 border-l border-line-border pl-6 space-y-4">' +
            activities.map(function (activity) {
                return '<div class="relative">' +
                    '<span class="absolute -left-[31px] top-1.5 h-3 w-3 rounded-full bg-secondary ring-4 ring-surface-white"></span>' +
                    '<div class="flex flex-wrap items-center justify-between gap-2">' +
                    '<div class="text-sm font-semibold text-primary">' + escape(activity.hanhDong || 'HOẠT ĐỘNG') + '</div>' +
                    '<div class="text-xs text-text-muted">' + escape(formatDate(activity.thoiGian)) + '</div>' +
                    '</div>' +
                    '<div class="mt-1 text-xs text-olive-grove">Tài khoản #' + escape(activity.maTK || '—') +
                    (activity.vaiTro ? ' · ' + escape(roleLabels[activity.vaiTro] || activity.vaiTro) : '') +
                    (activity.diaChiIp ? ' · IP ' + escape(activity.diaChiIp) : '') +
                    '</div>' +
                    (activity.moTa ? '<div class="mt-2 text-sm leading-6 text-on-surface-variant">' + escape(activity.moTa) + '</div>' : '') +
                    '</div>';
            }).join('') +
            '</div>';
    }

    function renderAdminIdentity() {
        var name = profile.hoTen || 'Quản trị viên';
        var email = profile.email || '—';
        var nameNodes = document.querySelectorAll('[data-user-name]');
        for (var i = 0; i < nameNodes.length; i++) {
            nameNodes[i].textContent = name;
        }
        var emailNode = $('adminSidebarEmail');
        if (emailNode) {
            emailNode.textContent = email;
        }
        var avatarNode = $('adminSidebarAvatar');
        if (avatarNode && profile.avatarUrl) {
            avatarNode.innerHTML = '<img src="' + escape(profile.avatarUrl) + '" class="h-full w-full object-cover" alt="Ảnh đại diện">';
        }
    }

    function updateBioCounter() {
        text('editBioCounter', $('editTieuSu').value.length);
    }

    function clearValidation() {
        hide('editHoTenError');
        hide('editPhoneError');
    }

    function validateEdit() {
        clearValidation();
        var name = $('editHoTen').value.trim();
        var phone = $('editPhone').value.trim();
        if (!name) {
            text('editHoTenError', 'Họ tên không được để trống.');
            show('editHoTenError');
            return false;
        }
        if (name.length > 255) {
            text('editHoTenError', 'Họ tên không được vượt quá 255 ký tự.');
            show('editHoTenError');
            return false;
        }
        if (phone && !/^0\d{9,10}$/.test(phone)) {
            text('editPhoneError', 'Số điện thoại không hợp lệ.');
            show('editPhoneError');
            return false;
        }
        return true;
    }

    function setEditMode(enabled) {
        if (!isSelf) {
            return;
        }
        if (enabled) {
            hide('profileReadonly');
            show('editProfileForm');
        } else {
            show('profileReadonly');
            hide('editProfileForm');
            clearValidation();
        }
    }

    function loadProfile() {
        hide('profileError');
        hide('profileContent');
        show('profileLoading');

        OB.request('GET', '/api/admin/users/' + encodeURIComponent(targetId))
            .then(function (data) {
                profile = data;
                hide('profileLoading');
                renderHero();
                renderPersonal();
                renderAccounts();
                renderStore();
                renderAddresses();
                renderActivities();
                // Chỉ khi xem hồ sơ của chính mình: ở /admin/users/{id}, profile là của người đang được xem.
                if (isSelf) {
                    renderAdminIdentity();
                }
                show('profileContent');
            })
            .catch(function (err) {
                hide('profileLoading');
                if (Auth.handleAuthError(err, 'QUAN_TRI_VIEN')) {
                    return;
                }
                if (err.status === 403) {
                    show('profileDenied');
                    return;
                }
                text('profileErrorText', err.message || 'Không thể tải hồ sơ người dùng.');
                show('profileError');
            });
    }

    function saveProfile(event) {
        event.preventDefault();
        if (busy || !validateEdit()) {
            return;
        }

        busy = true;
        $('saveEditButton').disabled = true;
        $('saveEditButton').textContent = 'Đang lưu...';

        OB.request('PUT', '/api/profile', {
            hoTen: $('editHoTen').value.trim(),
            soDienThoai: $('editPhone').value.trim() || null,
            avatarUrl: $('editAvatarUrl').value.trim() || null,
            tieuSu: $('editTieuSu').value.trim() || null
        })
            .then(function () {
                window.AdminUI.notify('Cập nhật hồ sơ thành công.', 'success');
                setEditMode(false);
                return Auth.loadProfileName();
            })
            .then(loadProfile)
            .catch(function (err) {
                if (Auth.handleAuthError(err, 'QUAN_TRI_VIEN')) {
                    return;
                }
                window.AdminUI.notify(err.message || 'Không thể cập nhật hồ sơ.', 'error');
            })
            .then(function () {
                busy = false;
                $('saveEditButton').disabled = false;
                $('saveEditButton').textContent = 'Lưu thay đổi';
            });
    }

    function init() {
        if (!Auth.isLoggedIn()) {
            Auth.redirectToLogin(window.location.pathname);
            return;
        }
        if (!Auth.hasRole('QUAN_TRI_VIEN')) {
            hide('profileLoading');
            show('profileDenied');
            return;
        }
        if (!targetId || targetId <= 0 || Number.isNaN(targetId)) {
            hide('profileLoading');
            text('profileErrorText', 'Không xác định được mã người dùng.');
            show('profileError');
            return;
        }

        if (!isSelf) {
            $('profileBackLink').href = '/admin/users';
            $('profileBackLabel').textContent = 'Quay lại danh sách người dùng';
        } else {
            $('profileBackLink').href = '/admin/dashboard';
            $('profileBackLabel').textContent = 'Về tổng quan quản trị';
        }

        $('editProfileButton').addEventListener('click', function () { setEditMode(true); });
        $('cancelEditButton').addEventListener('click', function () { setEditMode(false); renderPersonal(); });
        $('editTieuSu').addEventListener('input', updateBioCounter);
        $('editProfileForm').addEventListener('submit', saveProfile);
        $('profileRetry').addEventListener('click', loadProfile);

        loadProfile();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
