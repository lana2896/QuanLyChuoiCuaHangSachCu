(function () {

    'use strict';


    var OB = window.OldBook;

    if (!OB) {
        return;
    }


    var Auth = OB.auth;


    // =========================================================
    // CONSTANTS
    // =========================================================

    var ROLE_LABELS = OB.roleLabels || {

        KHACH_HANG: 'Khách hàng',

        CHU_CUA_HANG: 'Chủ cửa hàng',

        QUAN_LY: 'Quản lý',

        QUAN_TRI_VIEN: 'Quản trị viên',

        DON_VI_VAN_CHUYEN: 'Đơn vị vận chuyển'

    };


    var STATUS_LABELS = {

        HOAT_DONG: 'Hoạt động',

        CHO_XAC_THUC: 'Chờ xác thực',

        BI_KHOA: 'Bị khóa'

    };


    var state = {

        accounts: [],

        filteredAccounts: [],

        currentPage: 1,

        pageSize: 10,

        selectedAccount: null,

        busy: false

    };


    var $ = function (id) {

        return document.getElementById(id);

    };


    // =========================================================
    // HELPERS
    // =========================================================

    function escape(value) {

        return OB.escapeHtml(
            value == null
                ? ''
                : String(value)
        );

    }


    function text(
        id,
        value
    ) {

        var element = $(id);

        if (element) {

            element.textContent =
                value == null
                    ? ''
                    : String(value);

        }

    }


    function show(id) {

        var element = $(id);

        if (element) {

            element.classList.remove(
                'hidden'
            );

        }

    }


    function hide(id) {

        var element = $(id);

        if (element) {

            element.classList.add(
                'hidden'
            );

        }

    }


    function openModal(
        id
    ) {

        var element = $(id);

        if (!element) {
            return;
        }


        element.classList.remove(
            'hidden'
        );

        element.classList.add(
            'flex'
        );

    }


    function closeModal(
        id
    ) {

        var element = $(id);

        if (!element) {
            return;
        }


        element.classList.add(
            'hidden'
        );

        element.classList.remove(
            'flex'
        );

    }


    function normalize(
        value
    ) {

        return value == null
            ? ''
            : String(value)
                .trim()
                .toLowerCase();

    }


    function formatDate(
        value
    ) {

        if (!value) {
            return '—';
        }


        var date =
            new Date(value);


        if (
            isNaN(
                date.getTime()
            )
        ) {

            return String(value);

        }


        return date.toLocaleDateString(
            'vi-VN'
        );

    }


    function formatDateTime(
        value
    ) {

        if (!value) {
            return '—';
        }


        var date =
            new Date(value);


        if (
            isNaN(
                date.getTime()
            )
        ) {

            return String(value);

        }


        return date.toLocaleString(
            'vi-VN'
        );

    }


    // =========================================================
    // QUERY STRING
    // =========================================================

    function readQueryParams() {

        var params =
            new URLSearchParams(
                window.location.search
            );


        var keyword =
            params.get(
                'tuKhoa'
            );


        var role =
            params.get(
                'vaiTro'
            );


        var status =
            params.get(
                'trangThai'
            );


        if (keyword) {

            $('accountKeyword').value =
                keyword;

        }


        if (role) {

            $('accountRoleFilter').value =
                role;

        }


        if (status) {

            $('accountStatusFilter').value =
                status;

        }

    }


    // =========================================================
    // FILTER
    // =========================================================

    function buildFilterQuery() {

        var keyword =
            $('accountKeyword')
                .value
                .trim();


        var role =
            $('accountRoleFilter')
                .value;


        var status =
            $('accountStatusFilter')
                .value;


        var params =
            new URLSearchParams();


        if (keyword) {

            params.set(
                'tuKhoa',
                keyword
            );

        }


        if (role) {

            params.set(
                'vaiTro',
                role
            );

        }


        if (status) {

            params.set(
                'trangThai',
                status
            );

        }


        return params;

    }


    function applyClientFilters() {

        var keyword =
            normalize(
                $('accountKeyword').value
            );


        var role =
            $('accountRoleFilter').value;


        var status =
            $('accountStatusFilter').value;


        state.filteredAccounts =
            state.accounts.filter(
                function (account) {

                    var searchable = [

                        account.maTK,

                        account.maND,

                        account.hoTen,

                        account.email,

                        account.soDienThoai,

                        account.vaiTro,

                        ROLE_LABELS[
                            account.vaiTro
                        ]

                    ]
                        .map(normalize)
                        .join(' ');


                    if (
                        keyword &&
                        !searchable.includes(
                            keyword
                        )
                    ) {

                        return false;

                    }


                    if (
                        role &&
                        account.vaiTro !==
                        role
                    ) {

                        return false;

                    }


                    if (
                        status &&
                        account.trangThai !==
                        status
                    ) {

                        return false;

                    }


                    return true;

                }
            );


        state.currentPage =
            1;


        renderAccounts();

    }


    // =========================================================
    // LOAD
    // =========================================================

    function loadAccounts() {

        hide(
            'accountsError'
        );

        hide(
            'accountsEmpty'
        );

        hide(
            'accountsTableWrapper'
        );

        show(
            'accountsLoading'
        );


        OB.request(
            'GET',
            '/api/admin/accounts'
        )

        .then(
            function (data) {

                state.accounts =
                    Array.isArray(data)
                        ? data
                        : [];


                calculateStats();


                applyClientFilters();


                text(
                    'accountsLastUpdated',
                    'Cập nhật ' +
                    formatDateTime(
                        new Date()
                    )
                );

            }
        )

        .catch(
            function (err) {

                if (
                    Auth.handleAuthError(
                        err,
                        'QUAN_TRI_VIEN'
                    )
                ) {

                    return;

                }


                text(
                    'accountsErrorMessage',
                    err.message ||
                    'Không thể tải danh sách tài khoản.'
                );


                show(
                    'accountsError'
                );

            }
        )

        .finally(
            function () {

                hide(
                    'accountsLoading'
                );

            }
        );

    }


    // =========================================================
    // STATISTICS
    // =========================================================

    function calculateStats() {

        var accounts =
            state.accounts;


        text(
            'statTotal',
            accounts.length
        );


        text(
            'statActive',
            accounts.filter(
                function (account) {

                    return account.trangThai ===
                        'HOAT_DONG';

                }
            ).length
        );


        text(
            'statLocked',
            accounts.filter(
                function (account) {

                    return account.trangThai ===
                        'BI_KHOA';

                }
            ).length
        );


        text(
            'statStoreOwners',
            accounts.filter(
                function (account) {

                    return account.vaiTro ===
                        'CHU_CUA_HANG';

                }
            ).length
        );


        text(
            'statAdmins',
            accounts.filter(
                function (account) {

                    return account.vaiTro ===
                        'QUAN_TRI_VIEN';

                }
            ).length
        );

    }


    // =========================================================
    // TABLE
    // =========================================================

    function renderAccounts() {

        var total =
            state.filteredAccounts.length;


        if (!total) {

            hide(
                'accountsTableWrapper'
            );

            show(
                'accountsEmpty'
            );

            return;

        }


        hide(
            'accountsEmpty'
        );

        show(
            'accountsTableWrapper'
        );


        var start =
            (
                state.currentPage -
                1
            ) *
            state.pageSize;


        var end =
            Math.min(
                start +
                state.pageSize,
                total
            );


        var pageItems =
            state.filteredAccounts.slice(
                start,
                end
            );


        $('accountsRows').innerHTML =
            pageItems
                .map(
                    renderAccountRow
                )
                .join('');


        text(
            'accountsResultInfo',
            'Hiển thị ' +
            (start + 1) +
            '–' +
            end +
            ' / ' +
            total +
            ' tài khoản'
        );


        renderPagination();

    }


    function renderAccountRow(
        account
    ) {

        var statusClass =
            account.trangThai ===
            'BI_KHOA'

                ? 'bg-danger-soft text-mahogany-danger'

                : account.trangThai ===
                  'CHO_XAC_THUC'

                    ? 'bg-surface-cream text-olive-grove'

                    : 'bg-success-soft text-primary';


        var roleClass =
            'bg-surface-cream text-primary';


        var currentUser =
            Auth.getUser();


        var isSelf =
            currentUser &&
            Number(
                currentUser.maTK
            ) === Number(
                account.maTK
            );


        var actionButtons = '';


        // View profile

        actionButtons +=

            '<a ' +

            'href="/admin/users/' +
            encodeURIComponent(
                account.maND
            ) +
            '" ' +

            'class="inline-flex items-center justify-center w-9 h-9 rounded-lg bg-surface-cream text-primary hover:bg-surface-variant" title="Xem hồ sơ">' +

            '<span class="material-symbols-outlined text-[19px]">' +
            'visibility' +
            '</span>' +

            '</a>';


        // Change role

        actionButtons +=

            '<button ' +

            'type="button" ' +

            'data-action="role" ' +

            'data-account-id="' +
            escape(
                account.maTK
            ) +
            '" ' +

            (
                isSelf
                    ? 'disabled title="Không đổi vai trò tài khoản đang đăng nhập" '
                    : 'title="Đổi vai trò" '
            ) +

            'class="inline-flex items-center justify-center w-9 h-9 rounded-lg ' +

            (
                isSelf
                    ? 'bg-surface-cream text-text-muted opacity-50 cursor-not-allowed'
                    : 'bg-surface-cream text-primary hover:bg-surface-variant'
            ) +

            '">' +

            '<span class="material-symbols-outlined text-[19px]">' +
            'manage_accounts' +
            '</span>' +

            '</button>';


        // Lock / unlock

        if (
            account.trangThai ===
            'BI_KHOA'
        ) {

            actionButtons +=

                '<button ' +

                'type="button" ' +

                'data-action="unlock" ' +

                'data-account-id="' +
                escape(
                    account.maTK
                ) +
                '" ' +

                (
                    isSelf
                        ? 'disabled '
                        : ''
                ) +

                'class="inline-flex items-center justify-center w-9 h-9 rounded-lg ' +

                (
                    isSelf
                        ? 'bg-surface-cream text-text-muted opacity-50 cursor-not-allowed'
                        : 'bg-success-soft text-primary hover:bg-secondary-container'
                ) +

                '" ' +

                'title="Mở khóa">' +

                '<span class="material-symbols-outlined text-[19px]">' +
                'lock_open' +
                '</span>' +

                '</button>';

        } else {

            actionButtons +=

                '<button ' +

                'type="button" ' +

                'data-action="lock" ' +

                'data-account-id="' +
                escape(
                    account.maTK
                ) +
                '" ' +

                (
                    isSelf
                        ? 'disabled '
                        : ''
                ) +

                'class="inline-flex items-center justify-center w-9 h-9 rounded-lg ' +

                (
                    isSelf
                        ? 'bg-surface-cream text-text-muted opacity-50 cursor-not-allowed'
                        : 'bg-danger-soft text-mahogany-danger hover:bg-red-100'
                ) +

                '" ' +

                'title="Khóa">' +

                '<span class="material-symbols-outlined text-[19px]">' +
                'lock'
                +
                '</span>' +

                '</button>';

        }


        return (

            '<tr class="hover:bg-background/70 transition-colors">' +


            '<td class="px-5 py-4 align-top">' +

            '<div class="font-semibold text-primary">' +

            '#' +
            escape(
                account.maTK
            ) +

            '</div>' +

            '<div class="mt-0.5 text-[11px] text-text-muted">' +

            'ND #' +
            escape(
                account.maND
            ) +

            '</div>' +

            '</td>' +


            '<td class="px-5 py-4 align-top">' +

            '<a ' +

            'href="/admin/users/' +
            encodeURIComponent(
                account.maND
            ) +
            '" ' +

            'class="font-semibold text-primary hover:underline">' +

            escape(
                account.hoTen ||
                'Chưa cập nhật'
            ) +

            '</a>' +

            '</td>' +


            '<td class="px-5 py-4 align-top">' +

            '<div class="text-sm text-body-text break-all">' +

            escape(
                account.email ||
                '—'
            ) +

            '</div>' +

            '<div class="mt-1 text-[11px] text-text-muted">' +

            'Tạo: ' +
            escape(
                formatDate(
                    account.ngayTao
                )
            ) +

            '</div>' +

            '</td>' +


            '<td class="px-5 py-4 align-top text-sm text-body-text">' +

            escape(
                account.soDienThoai ||
                'Chưa cập nhật'
            ) +

            '</td>' +


            '<td class="px-5 py-4 align-top">' +

            '<span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full ' +
            roleClass +
            ' font-label-md text-label-md">' +

            '<span class="material-symbols-outlined text-[15px]">' +
            getRoleIcon(
                account.vaiTro
            ) +
            '</span>' +

            escape(
                ROLE_LABELS[
                    account.vaiTro
                ] ||
                account.vaiTro
            ) +

            '</span>' +

            '</td>' +


            '<td class="px-5 py-4 align-top">' +

            '<span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full ' +
            statusClass +
            ' font-label-md text-label-md">' +

            '<span class="material-symbols-outlined text-[15px]">' +
            getStatusIcon(
                account.trangThai
            ) +
            '</span>' +

            escape(
                STATUS_LABELS[
                    account.trangThai
                ] ||
                account.trangThai
            ) +

            '</span>' +

            '</td>' +


            '<td class="px-5 py-4 align-top">' +

            '<div class="flex items-center justify-end gap-1.5">' +

            actionButtons +

            '</div>' +

            '</td>' +

            '</tr>'

        );

    }


    function getRoleIcon(
        role
    ) {

        switch (role) {

            case 'CHU_CUA_HANG':
                return 'storefront';

            case 'QUAN_LY':
                return 'manage_accounts';

            case 'QUAN_TRI_VIEN':
                return 'admin_panel_settings';

            case 'DON_VI_VAN_CHUYEN':
                return 'local_shipping';

            default:
                return 'person';

        }

    }


    function getStatusIcon(
        status
    ) {

        switch (status) {

            case 'BI_KHOA':
                return 'lock';

            case 'CHO_XAC_THUC':
                return 'schedule';

            default:
                return 'check_circle';

        }

    }


    // =========================================================
    // PAGINATION
    // =========================================================

    function renderPagination() {

        var total =
            state.filteredAccounts.length;


        var pageCount =
            Math.max(
                1,
                Math.ceil(
                    total /
                    state.pageSize
                )
            );


        if (
            state.currentPage >
            pageCount
        ) {

            state.currentPage =
                pageCount;

        }


        var html = '';


        html +=

            '<button ' +

            'type="button" ' +

            'data-page="' +
            (state.currentPage - 1) +
            '" ' +

            (
                state.currentPage <= 1
                    ? 'disabled '
                    : ''
            ) +

            'class="w-9 h-9 rounded-lg bg-surface-cream text-text-muted flex items-center justify-center ' +

            (
                state.currentPage <= 1
                    ? 'opacity-40'
                    : 'hover:bg-surface-variant'
            ) +

            '">' +

            '<span class="material-symbols-outlined text-[19px]">' +
            'chevron_left' +
            '</span>' +

            '</button>';


        var numbers =
            buildPageNumbers(
                state.currentPage,
                pageCount
            );


        numbers.forEach(
            function (page) {

                if (
                    page === '...'
                ) {

                    html +=
                        '<span class="px-1 text-text-muted">...</span>';

                    return;

                }


                html +=

                    '<button ' +

                    'type="button" ' +

                    'data-page="' +
                    page +
                    '" ' +

                    'class="w-9 h-9 rounded-lg ' +

                    (
                        page ===
                        state.currentPage

                            ? 'bg-primary text-on-primary'

                            : 'bg-surface-cream text-primary hover:bg-surface-variant'
                    ) +

                    ' font-label-md text-label-md">' +

                    page +

                    '</button>';

            }
        );


        html +=

            '<button ' +

            'type="button" ' +

            'data-page="' +
            (state.currentPage + 1) +
            '" ' +

            (
                state.currentPage >= pageCount
                    ? 'disabled '
                    : ''
            ) +

            'class="w-9 h-9 rounded-lg bg-surface-cream text-text-muted flex items-center justify-center ' +

            (
                state.currentPage >= pageCount
                    ? 'opacity-40'
                    : 'hover:bg-surface-variant'
            ) +

            '">' +

            '<span class="material-symbols-outlined text-[19px]">' +
            'chevron_right' +
            '</span>' +

            '</button>';


        $('accountsPagination').innerHTML =
            html;


        $('accountsPagination')
            .querySelectorAll(
                '[data-page]'
            )
            .forEach(
                function (button) {

                    button.addEventListener(
                        'click',
                        function () {

                            var page =
                                Number(
                                    button.getAttribute(
                                        'data-page'
                                    )
                                );


                            if (
                                !page ||
                                page < 1 ||
                                page > pageCount
                            ) {

                                return;

                            }


                            state.currentPage =
                                page;


                            renderAccounts();

                        }
                    );

                }
            );

    }


    function buildPageNumbers(
        current,
        total
    ) {

        if (
            total <= 7
        ) {

            return Array.from(
                {
                    length: total
                },
                function (_, index) {

                    return index + 1;

                }
            );

        }


        var result = [
            1
        ];


        if (
            current > 4
        ) {

            result.push(
                '...'
            );

        }


        var start =
            Math.max(
                2,
                current - 1
            );


        var end =
            Math.min(
                total - 1,
                current + 1
            );


        for (
            var page = start;
            page <= end;
            page++
        ) {

            result.push(
                page
            );

        }


        if (
            current <
            total - 3
        ) {

            result.push(
                '...'
            );

        }


        result.push(
            total
        );


        return result;

    }


    // =========================================================
    // CREATE ACCOUNT
    // =========================================================

    function openCreateAccount() {

        $('createAccountForm').reset();

        hide(
            'createAccountError'
        );


        $('createAccountRole').value =
            'KHACH_HANG';


        openModal(
            'createAccountModal'
        );

    }


    function submitCreateAccount(
        event
    ) {

        event.preventDefault();


        if (
            state.busy
        ) {

            return;

        }


        var hoTen =
            $('createAccountName')
                .value
                .trim();


        var email =
            $('createAccountEmail')
                .value
                .trim();


        var soDienThoai =
            $('createAccountPhone')
                .value
                .trim();


        var matKhau =
            $('createAccountPassword')
                .value;


        var vaiTro =
            $('createAccountRole')
                .value;


        var error =
            validateCreateAccount(
                hoTen,
                email,
                soDienThoai,
                matKhau,
                vaiTro
            );


        if (error) {

            showCreateError(
                error
            );

            return;

        }


        state.busy =
            true;


        hide(
            'createAccountError'
        );


        $('btnSubmitCreateAccount')
            .disabled =
            true;


        $('btnSubmitCreateAccount')
            .textContent =
            'Đang lưu...';


        OB.request(
            'POST',
            '/api/admin/accounts',
            {

                hoTen: hoTen,

                email: email,

                soDienThoai:
                    soDienThoai ||
                    null,

                matKhau: matKhau,

                vaiTro: vaiTro

            }
        )

        .then(
            function () {

                closeModal(
                    'createAccountModal'
                );


                OB.toast(
                    'Tạo tài khoản thành công.',
                    'success'
                );


                return loadAccountsPromise();

            }
        )

        .catch(
            function (err) {

                if (
                    Auth.handleAuthError(
                        err,
                        'QUAN_TRI_VIEN'
                    )
                ) {

                    return;

                }


                showCreateError(
                    err.message ||
                    'Không thể tạo tài khoản.'
                );

            }
        )

        .finally(
            function () {

                state.busy =
                    false;


                $('btnSubmitCreateAccount')
                    .disabled =
                    false;


                $('btnSubmitCreateAccount')
                    .textContent =
                    'Lưu tài khoản';

            }
        );

    }


    function loadAccountsPromise() {

        return OB.request(
            'GET',
            '/api/admin/accounts'
        )
        .then(
            function (data) {

                state.accounts =
                    Array.isArray(data)
                        ? data
                        : [];


                calculateStats();

                applyClientFilters();

                text(
                    'accountsLastUpdated',
                    'Cập nhật ' +
                    formatDateTime(
                        new Date()
                    )
                );

            }
        );

    }


    function validateCreateAccount(
        hoTen,
        email,
        phone,
        password,
        role
    ) {

        if (!hoTen) {

            return 'Họ tên không được để trống.';

        }


        if (!email) {

            return 'Email không được để trống.';

        }


        if (
            !/^[^\s@]+@[^\s@]+\.[^\s@]+$/
                .test(email)
        ) {

            return 'Email không hợp lệ.';

        }


        if (
            phone &&
            !/^0\d{9,10}$/
                .test(phone)
        ) {

            return 'Số điện thoại không hợp lệ.';

        }


        if (
            !password ||
            password.length < 6
        ) {

            return 'Mật khẩu phải có ít nhất 6 ký tự.';

        }


        if (
            [
                'KHACH_HANG',
                'CHU_CUA_HANG',
                'QUAN_LY',
                'QUAN_TRI_VIEN'
            ].indexOf(role) === -1
        ) {

            return 'Vai trò không hợp lệ.';

        }


        return null;

    }


    function showCreateError(
        message
    ) {

        text(
            'createAccountError',
            message
        );


        show(
            'createAccountError'
        );

    }


    // =========================================================
    // CHANGE ROLE
    // =========================================================

    function openChangeRole(
        account
    ) {

        state.selectedAccount =
            account;


        hide(
            'changeRoleError'
        );


        text(
            'changeRoleAccountInfo',

            'Tài khoản #' +
            account.maTK +
            ' · ' +
            (
                account.hoTen ||
                'Chưa cập nhật'
            )

        );


        $('accountNewRole')
            .value =
            account.vaiTro;


        openModal(
            'changeRoleModal'
        );

    }


    function submitChangeRole(
        event
    ) {

        event.preventDefault();


        var account =
            state.selectedAccount;


        if (!account) {
            return;
        }


        var currentUser =
            Auth.getUser();


        if (
            currentUser &&
            Number(
                currentUser.maTK
            ) === Number(
                account.maTK
            )
        ) {

            showChangeRoleError(
                'Không thể thay đổi vai trò của tài khoản quản trị đang đăng nhập.'
            );

            return;

        }


        var newRole =
            $('accountNewRole')
                .value;


        if (
            newRole ===
            account.vaiTro
        ) {

            showChangeRoleError(
                'Vai trò mới phải khác vai trò hiện tại.'
            );

            return;

        }


        hide(
            'changeRoleError'
        );


        $('btnSubmitChangeRole')
            .disabled =
            true;


        $('btnSubmitChangeRole')
            .textContent =
            'Đang lưu...';


        OB.request(
            'PUT',
            '/api/admin/accounts/' +
            encodeURIComponent(
                account.maTK
            ) +
            '/role',
            {
                vaiTro: newRole
            }
        )

        .then(
            function () {

                closeModal(
                    'changeRoleModal'
                );


                OB.toast(
                    'Đã thay đổi vai trò tài khoản.',
                    'success'
                );


                return loadAccountsPromise();

            }
        )

        .catch(
            function (err) {

                if (
                    Auth.handleAuthError(
                        err,
                        'QUAN_TRI_VIEN'
                    )
                ) {

                    return;

                }


                showChangeRoleError(
                    err.message ||
                    'Không thể thay đổi vai trò.'
                );

            }
        )

        .finally(
            function () {

                $('btnSubmitChangeRole')
                    .disabled =
                    false;


                $('btnSubmitChangeRole')
                    .textContent =
                    'Xác nhận thay đổi';

            }
        );

    }


    function showChangeRoleError(
        message
    ) {

        text(
            'changeRoleError',
            message
        );


        show(
            'changeRoleError'
        );

    }


    // =========================================================
    // LOCK
    // =========================================================

    function openLockAccount(
        account
    ) {

        state.selectedAccount =
            account;


        hide(
            'lockAccountError'
        );


        $('accountLockReason')
            .value =
            '';


        text(
            'lockAccountInfo',

            'Tài khoản #' +
            account.maTK +
            ' · ' +
            (
                account.hoTen ||
                'Chưa cập nhật'
            )

        );


        openModal(
            'lockAccountModal'
        );

    }


    function submitLockAccount(
        event
    ) {

        event.preventDefault();


        var account =
            state.selectedAccount;


        if (!account) {
            return;
        }


        var reason =
            $('accountLockReason')
                .value
                .trim();


        if (!reason) {

            showLockError(
                'Lý do khóa tài khoản không được để trống.'
            );

            return;

        }


        var currentUser =
            Auth.getUser();


        if (
            currentUser &&
            Number(
                currentUser.maTK
            ) === Number(
                account.maTK
            )
        ) {

            showLockError(
                'Hệ thống không cho phép tự khóa tài khoản hiện tại.'
            );

            return;

        }


        hide(
            'lockAccountError'
        );


        $('btnSubmitLockAccount')
            .disabled =
            true;


        $('btnSubmitLockAccount')
            .textContent =
            'Đang xử lý...';


        OB.request(
            'PUT',
            '/api/admin/accounts/' +
            encodeURIComponent(
                account.maTK
            ) +
            '/lock',
            {
                lyDo: reason
            }
        )

        .then(
            function () {

                closeModal(
                    'lockAccountModal'
                );


                OB.toast(
                    'Đã khóa tài khoản.',
                    'success'
                );


                return loadAccountsPromise();

            }
        )

        .catch(
            function (err) {

                if (
                    Auth.handleAuthError(
                        err,
                        'QUAN_TRI_VIEN'
                    )
                ) {

                    return;

                }


                showLockError(
                    err.message ||
                    'Không thể khóa tài khoản.'
                );

            }
        )

        .finally(
            function () {

                $('btnSubmitLockAccount')
                    .disabled =
                    false;


                $('btnSubmitLockAccount')
                    .textContent =
                    'Xác nhận khóa';

            }
        );

    }


    function showLockError(
        message
    ) {

        text(
            'lockAccountError',
            message
        );


        show(
            'lockAccountError'
        );

    }


    // =========================================================
    // UNLOCK
    // =========================================================

    function unlockAccount(
        account
    ) {

        var currentUser =
            Auth.getUser();


        if (
            currentUser &&
            Number(
                currentUser.maTK
            ) === Number(
                account.maTK
            )
        ) {

            OB.toast(
                'Không thể tự mở khóa tài khoản hiện tại từ màn hình này.',
                'danger'
            );

            return;

        }


        var confirmed =
            window.confirm(

                'Bạn có chắc muốn mở khóa tài khoản #' +
                account.maTK +
                ' của ' +
                (
                    account.hoTen ||
                    'người dùng này'
                ) +
                '?'

            );


        if (!confirmed) {
            return;
        }


        OB.request(
            'PUT',
            '/api/admin/accounts/' +
            encodeURIComponent(
                account.maTK
            ) +
            '/unlock'
        )

        .then(
            function () {

                OB.toast(
                    'Đã mở khóa tài khoản.',
                    'success'
                );


                return loadAccountsPromise();

            }
        )

        .catch(
            function (err) {

                if (
                    Auth.handleAuthError(
                        err,
                        'QUAN_TRI_VIEN'
                    )
                ) {

                    return;

                }


                OB.toast(
                    err.message ||
                    'Không thể mở khóa tài khoản.',
                    'danger'
                );

            }
        );

    }


    // =========================================================
    // EVENTS
    // =========================================================

    function bindEvents() {


        // FILTER

        $('accountsFilterForm')
            .addEventListener(
                'submit',
                function (event) {

                    event.preventDefault();


                    applyClientFilters();


                    /*
                     * Giữ URL đồng bộ để từ users.html
                     * chuyển sang accounts vẫn giữ filter.
                     */

                    var params =
                        buildFilterQuery();


                    var url =
                        window.location.pathname;


                    if (
                        params.toString()
                    ) {

                        url +=
                            '?' +
                            params.toString();

                    }


                    window.history.replaceState(
                        {},
                        '',
                        url
                    );

                }
            );


        // RESET

        $('btnResetAccountFilters')
            .addEventListener(
                'click',
                function () {

                    $('accountKeyword')
                        .value =
                        '';

                    $('accountRoleFilter')
                        .value =
                        '';

                    $('accountStatusFilter')
                        .value =
                        '';


                    updateQuickFilterUI(
                        ''
                    );


                    applyClientFilters();


                    window.history.replaceState(
                        {},
                        '',
                        '/admin/accounts'
                    );

                }
            );


        // QUICK STATUS

        document
            .querySelectorAll(
                '[data-quick-status]'
            )
            .forEach(
                function (button) {

                    button.addEventListener(
                        'click',
                        function () {

                            var status =
                                button.getAttribute(
                                    'data-quick-status'
                                );


                            $('accountStatusFilter')
                                .value =
                                status;


                            applyClientFilters();


                            updateQuickFilterUI(
                                status
                            );

                        }
                    );

                }
            );


        // CREATE

        $('btnOpenCreateAccount')
            .addEventListener(
                'click',
                openCreateAccount
            );


        $('btnCloseCreateAccount')
            .addEventListener(
                'click',
                function () {

                    closeModal(
                        'createAccountModal'
                    );

                }
            );


        $('btnCancelCreateAccount')
            .addEventListener(
                'click',
                function () {

                    closeModal(
                        'createAccountModal'
                    );

                }
            );


        $('createAccountForm')
            .addEventListener(
                'submit',
                submitCreateAccount
            );


        // CHANGE ROLE

        $('btnCloseChangeRole')
            .addEventListener(
                'click',
                function () {

                    closeModal(
                        'changeRoleModal'
                    );

                }
            );


        $('btnCancelChangeRole')
            .addEventListener(
                'click',
                function () {

                    closeModal(
                        'changeRoleModal'
                    );

                }
            );


        $('changeRoleForm')
            .addEventListener(
                'submit',
                submitChangeRole
            );


        // LOCK

        $('btnCloseLockAccount')
            .addEventListener(
                'click',
                function () {

                    closeModal(
                        'lockAccountModal'
                    );

                }
            );


        $('btnCancelLockAccount')
            .addEventListener(
                'click',
                function () {

                    closeModal(
                        'lockAccountModal'
                    );

                }
            );


        $('lockAccountForm')
            .addEventListener(
                'submit',
                submitLockAccount
            );


        // TABLE ACTIONS

        $('accountsRows')
            .addEventListener(
                'click',
                function (event) {

                    var button =
                        event.target.closest(
                            '[data-action]'
                        );


                    if (!button) {
                        return;
                    }


                    var accountId =
                        Number(
                            button.getAttribute(
                                'data-account-id'
                            )
                        );


                    var account =
                        state.accounts.find(
                            function (item) {

                                return Number(
                                    item.maTK
                                ) ===
                                accountId;

                            }
                        );


                    if (!account) {
                        return;
                    }


                    var action =
                        button.getAttribute(
                            'data-action'
                        );


                    if (
                        action ===
                        'role'
                    ) {

                        openChangeRole(
                            account
                        );

                    }


                    if (
                        action ===
                        'lock'
                    ) {

                        openLockAccount(
                            account
                        );

                    }


                    if (
                        action ===
                        'unlock'
                    ) {

                        unlockAccount(
                            account
                        );

                    }

                }
            );


        // RETRY

        $('btnRetryAccounts')
            .addEventListener(
                'click',
                loadAccounts
            );


        // CLOSE MODAL WITH BACKDROP

        [
            'createAccountModal',
            'changeRoleModal',
            'lockAccountModal'
        ]
        .forEach(
            function (id) {

                $(id)
                    .addEventListener(
                        'click',
                        function (event) {

                            if (
                                event.target ===
                                $(id)
                            ) {

                                closeModal(
                                    id
                                );

                            }

                        }
                    );

            }
        );


        // ESC

        document.addEventListener(
            'keydown',
            function (event) {

                if (
                    event.key ===
                    'Escape'
                ) {

                    closeModal(
                        'createAccountModal'
                    );

                    closeModal(
                        'changeRoleModal'
                    );

                    closeModal(
                        'lockAccountModal'
                    );

                }

            }
        );

    }


    function updateQuickFilterUI(
        activeStatus
    ) {

        document
            .querySelectorAll(
                '.account-quick-filter'
            )
            .forEach(
                function (button) {

                    var status =
                        button.getAttribute(
                            'data-quick-status'
                        );


                    if (
                        status ===
                        activeStatus
                    ) {

                        button.className =
                            'account-quick-filter px-3 py-1.5 rounded-full bg-primary text-on-primary font-label-md text-label-md';

                    } else {

                        button.className =
                            'account-quick-filter px-3 py-1.5 rounded-full bg-surface-cream text-text-muted font-label-md text-label-md';

                    }

                }
            );

    }


    // =========================================================
    // INIT
    // =========================================================

    function init() {

        if (
            !Auth.isLoggedIn()
        ) {

            Auth.redirectToLogin(
                window.location.pathname
            );

            return;

        }


        if (
            !Auth.hasRole(
                'QUAN_TRI_VIEN'
            )
        ) {

            hide(
                'accountsLoading'
            );


            text(
                'accountsErrorMessage',
                'Bạn không có quyền truy cập chức năng quản lý tài khoản.'
            );


            show(
                'accountsError'
            );


            return;

        }


        readQueryParams();


        bindEvents();


        updateQuickFilterUI(
            $('accountStatusFilter')
                .value || ''
        );


        loadAccounts();

    }


    if (
        document.readyState ===
        'loading'
    ) {

        document.addEventListener(
            'DOMContentLoaded',
            init
        );

    } else {

        init();

    }

})();