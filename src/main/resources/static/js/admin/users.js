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

        CHU_CUA_HANG: 'Chủ chi nhánh',

        DON_VI_VAN_CHUYEN: 'Đơn vị vận chuyển',

        QUAN_LY: 'Quản lý',

        QUAN_TRI_VIEN: 'Quản trị viên'

    };


    var STATUS_LABELS = {

        HOAT_DONG: 'Hoạt động',

        CHO_XAC_THUC: 'Chờ xác thực',

        BI_KHOA: 'Bị khóa'

    };


    var state = {

        accounts: [],

        users: [],

        filteredUsers: [],

        page: 1,

        pageSize: 4,

        loading: false

    };


    // =========================================================
    // DOM
    // =========================================================

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


    function formatDate(value) {

        if (!value) {
            return '—';
        }


        var date = new Date(value);


        if (isNaN(date.getTime())) {
            return String(value);
        }


        return date.toLocaleDateString(
            'vi-VN'
        );

    }


    function formatDateTime(value) {

        if (!value) {
            return '—';
        }


        var date = new Date(value);


        if (isNaN(date.getTime())) {
            return String(value);
        }


        return date.toLocaleString(
            'vi-VN'
        );

    }


    function show(id) {

        var element = $(id);

        if (element) {
            element.classList.remove('hidden');
        }

    }


    function hide(id) {

        var element = $(id);

        if (element) {
            element.classList.add('hidden');
        }

    }


    function setText(
        id,
        value
    ) {

        var element = $(id);

        if (!element) {
            return;
        }


        element.textContent =
            value == null
                ? ''
                : String(value);

    }


    function normalizeText(value) {

        return value == null
            ? ''
            : String(value)
                .trim()
                .toLowerCase();

    }


    // =========================================================
    // ACCOUNT -> USER GROUPING
    // =========================================================

    function groupAccountsByUser(
        accounts
    ) {

        var groups = new Map();


        accounts.forEach(
            function (account) {

                var maND =
                    account.maND;


                if (!maND) {
                    return;
                }


                if (!groups.has(maND)) {

                    groups.set(
                        maND,
                        {
                            maND: maND,

                            email:
                                account.email ||
                                '',

                            hoTen:
                                account.hoTen ||
                                'Chưa cập nhật',

                            soDienThoai:
                                account.soDienThoai ||
                                null,

                            accounts: []

                        }
                    );

                }


                var user =
                    groups.get(maND);


                user.accounts.push(
                    account
                );


                /*
                 * Thực tế tất cả account của cùng maND
                 * phải có cùng email / họ tên / số điện thoại.
                 *
                 * Tuy nhiên vẫn cập nhật giá trị nếu
                 * account hiện tại có dữ liệu đầy đủ hơn.
                 */

                if (
                    !user.email &&
                    account.email
                ) {

                    user.email =
                        account.email;

                }


                if (
                    (
                        !user.hoTen ||
                        user.hoTen ===
                        'Chưa cập nhật'
                    ) &&
                    account.hoTen
                ) {

                    user.hoTen =
                        account.hoTen;

                }


                if (
                    !user.soDienThoai &&
                    account.soDienThoai
                ) {

                    user.soDienThoai =
                        account.soDienThoai;

                }

            }
        );


        var users =
            Array.from(
                groups.values()
            );


        users.forEach(
            enrichUser
        );


        return users;

    }


    function enrichUser(
        user
    ) {

        var accounts =
            user.accounts || [];


        user.roles =
            Array.from(
                new Set(
                    accounts
                        .map(
                            function (account) {
                                return account.vaiTro;
                            }
                        )
                        .filter(Boolean)
                )
            );


        user.isMultiAccount =
            accounts.length > 1;


        user.isStoreOwner =
            user.roles.includes(
                'CHU_CUA_HANG'
            );


        user.lockedAccounts =
            accounts.filter(
                function (account) {

                    return account.trangThai ===
                        'BI_KHOA';

                }
            );


        user.activeAccounts =
            accounts.filter(
                function (account) {

                    return account.trangThai ===
                        'HOAT_DONG';

                }
            );


        user.pendingAccounts =
            accounts.filter(
                function (account) {

                    return account.trangThai ===
                        'CHO_XAC_THUC';

                }
            );


        user.statusCode =
            calculateUserStatus(
                user
            );


        user.createdAt =
            getEarliestCreatedAt(
                accounts
            );

    }


    function calculateUserStatus(
        user
    ) {

        var total =
            user.accounts.length;


        if (
            total > 0 &&
            user.lockedAccounts.length ===
            total
        ) {

            return 'BI_KHOA';

        }


        if (
            user.activeAccounts.length >
            0
        ) {

            /*
             * Người dùng có ít nhất một
             * tài khoản hoạt động thì coi là hoạt động.
             *
             * Card sẽ hiển thị thêm số account bị khóa
             * nếu có.
             */

            return 'HOAT_DONG';

        }


        if (
            user.pendingAccounts.length >
            0
        ) {

            return 'CHO_XAC_THUC';

        }


        return 'BI_KHOA';

    }


    function getEarliestCreatedAt(
        accounts
    ) {

        var dates =
            accounts
                .map(
                    function (account) {

                        return account.ngayTao
                            ? new Date(
                                account.ngayTao
                            )
                            : null;

                    }
                )
                .filter(
                    function (date) {

                        return date &&
                            !isNaN(
                                date.getTime()
                            );

                    }
                );


        if (!dates.length) {
            return null;
        }


        dates.sort(
            function (a, b) {
                return a - b;
            }
        );


        return dates[0];

    }


    // =========================================================
    // STATISTICS
    // =========================================================

    function calculateStats() {

        var users =
            state.users;


        var totalUsers =
            users.length;


        var activeUsers =
            users.filter(
                function (user) {

                    return user.activeAccounts.length >
                        0;

                }
            ).length;


        var storeOwners =
            users.filter(
                function (user) {

                    return user.isStoreOwner;

                }
            ).length;


        var lockedAccounts =
            state.accounts.filter(
                function (account) {

                    return account.trangThai ===
                        'BI_KHOA';

                }
            ).length;


        setText(
            'statTotalUsers',
            totalUsers
        );


        setText(
            'statActiveUsers',
            activeUsers
        );


        setText(
            'statStoreOwners',
            storeOwners
        );


        setText(
            'statLockedAccounts',
            lockedAccounts
        );


        setText(
            'quickAllCount',
            '(' + totalUsers + ')'
        );


        setText(
            'quickActiveCount',
            '(' + activeUsers + ')'
        );


        setText(
            'quickMultiCount',
            '(' +
            users.filter(
                function (user) {
                    return user.isMultiAccount;
                }
            ).length +
            ')'
        );


        setText(
            'quickStoreCount',
            '(' + storeOwners + ')'
        );


        setText(
            'quickLockedCount',
            '(' +
            users.filter(
                function (user) {
                    return user.statusCode ===
                        'BI_KHOA';
                }
            ).length +
            ')'
        );

    }


    // =========================================================
    // FILTER
    // =========================================================

    function getFilters() {

        return {

            search:
                normalizeText(
                    $('userSearch').value
                ),

            role:
                $('userRoleFilter').value,

            status:
                $('userStatusFilter').value,

            accountDensity:
                $('userAccountFilter').value,

            created:
                $('createdDateFilter').value,

            storeOwner:
                $('advancedStoreOwner').checked,

            multiAccount:
                $('advancedMultiAccount').checked

        };

    }


    function userMatchesSearch(
        user,
        search
    ) {

        if (!search) {
            return true;
        }


        var accountIds =
            user.accounts
                .map(
                    function (account) {
                        return [
                            account.maTK,
                            account.maND
                        ];
                    }
                )
                .flat()
                .join(' ');


        var searchable = [

            user.hoTen,

            user.email,

            user.soDienThoai,

            user.maND,

            accountIds,

            user.roles.join(' ')

        ]
            .map(normalizeText)
            .join(' ');


        return searchable.includes(
            search
        );

    }


    function matchesCreatedDate(
        user,
        value
    ) {

        if (!value) {
            return true;
        }


        if (!user.createdAt) {
            return false;
        }


        var created =
            new Date(
                user.createdAt
            );


        var now =
            new Date();


        if (value === '30days') {

            var thirtyDaysAgo =
                new Date(
                    now.getTime() -
                    (
                        30 *
                        24 *
                        60 *
                        60 *
                        1000
                    )
                );


            return created >=
                thirtyDaysAgo;

        }


        if (value === 'q1-2026') {

            var q1Start =
                new Date(
                    2026,
                    0,
                    1,
                    0,
                    0,
                    0
                );


            var q1End =
                new Date(
                    2026,
                    3,
                    1,
                    0,
                    0,
                    0
                );


            return created >= q1Start &&
                created < q1End;

        }


        if (value === '2025') {

            var start2025 =
                new Date(
                    2025,
                    0,
                    1,
                    0,
                    0,
                    0
                );


            var start2026 =
                new Date(
                    2026,
                    0,
                    1,
                    0,
                    0,
                    0
                );


            return created >= start2025 &&
                created < start2026;

        }


        return true;

    }


    function applyFilters() {

        var filters =
            getFilters();


        state.filteredUsers =
            state.users.filter(
                function (user) {


                    // SEARCH

                    if (
                        !userMatchesSearch(
                            user,
                            filters.search
                        )
                    ) {

                        return false;

                    }


                    // ROLE

                    if (
                        filters.role &&
                        !user.roles.includes(
                            filters.role
                        )
                    ) {

                        return false;

                    }


                    // STATUS

                    if (
                        filters.status &&
                        !user.accounts.some(
                            function (account) {

                                return account.trangThai ===
                                    filters.status;

                            }
                        )
                    ) {

                        return false;

                    }


                    // ACCOUNT COUNT

                    if (
                        filters.accountDensity ===
                        'single' &&
                        user.accounts.length !==
                        1
                    ) {

                        return false;

                    }


                    if (
                        filters.accountDensity ===
                        'multi' &&
                        user.accounts.length <=
                        1
                    ) {

                        return false;

                    }


                    // CREATED

                    if (
                        !matchesCreatedDate(
                            user,
                            filters.created
                        )
                    ) {

                        return false;

                    }


                    // STORE OWNER

                    if (
                        filters.storeOwner &&
                        !user.isStoreOwner
                    ) {

                        return false;

                    }


                    // MULTI ACCOUNT

                    if (
                        filters.multiAccount &&
                        !user.isMultiAccount
                    ) {

                        return false;

                    }


                    return true;

                }
            );


        state.filteredUsers.sort(
            function (a, b) {

                var da =
                    a.createdAt
                        ? new Date(a.createdAt).getTime()
                        : 0;

                var db =
                    b.createdAt
                        ? new Date(b.createdAt).getTime()
                        : 0;


                return db - da;

            }
        );


        state.page = 1;


        renderUsers();

        updateSearchClearButton();

    }


    // =========================================================
    // QUICK FILTER
    // =========================================================

    function applyQuickFilter(
        type
    ) {

        resetFilterInputs();


        switch (type) {

            case 'all':

                break;


            case 'active':

                $('userStatusFilter').value =
                    'HOAT_DONG';

                break;


            case 'multi':

                $('userAccountFilter').value =
                    'multi';

                break;


            case 'store':

                $('userRoleFilter').value =
                    'CHU_CUA_HANG';

                break;


            case 'locked':

                $('userStatusFilter').value =
                    'BI_KHOA';

                break;

        }


        applyFilters();

        updateQuickFilterUI(
            type
        );

    }


    function resetFilterInputs() {

        $('userSearch').value =
            '';

        $('userRoleFilter').value =
            '';

        $('userStatusFilter').value =
            '';

        $('userAccountFilter').value =
            '';

        $('createdDateFilter').value =
            '';

        $('advancedStoreOwner').checked =
            false;

        $('advancedMultiAccount').checked =
            false;

    }


    function updateQuickFilterUI(
        activeType
    ) {

        document.querySelectorAll(
            '.user-quick-filter'
        ).forEach(
            function (button) {

                var type =
                    button.getAttribute(
                        'data-quick-filter'
                    );


                if (
                    type === activeType
                ) {

                    button.className =
                        'user-quick-filter ' +
                        'px-3 py-1 rounded-full ' +
                        'bg-primary text-on-primary ' +
                        'font-label-md text-label-md ' +
                        'whitespace-nowrap shadow-sm';

                } else {

                    button.className =
                        'user-quick-filter ' +
                        'px-3 py-1 rounded-full ' +
                        'bg-surface-cream text-text-muted ' +
                        'hover:text-primary ' +
                        'font-label-md text-label-md ' +
                        'whitespace-nowrap transition-colors';

                }

            }
        );

    }


    // =========================================================
    // CARD RENDERING
    // =========================================================

    function renderUsers() {

        var total =
            state.filteredUsers.length;


        if (!total) {

            hide('usersList');
            hide('usersPagination');

            show('usersEmpty');

            return;

        }


        hide('usersEmpty');
        show('usersList');


        var start =
            (
                state.page - 1
            ) *
            state.pageSize;


        var end =
            Math.min(
                start +
                state.pageSize,
                total
            );


        var pageUsers =
            state.filteredUsers.slice(
                start,
                end
            );


        $('usersList').innerHTML =
            pageUsers.map(
                renderUserCard
            ).join('');


        renderPagination();


        show('usersPagination');

    }


    function renderUserCard(
        user
    ) {

        var status =
            getUserStatusMeta(
                user
            );


        var created =
            user.createdAt
                ? formatDate(
                    user.createdAt
                )
                : 'Chưa cập nhật';


        var uid =
            String(
                user.maND
            ).padStart(
                5,
                '0'
            );


        var accountCount =
            user.accounts.length;


        var lockedCount =
            user.lockedAccounts.length;


        var hasMixedStatus =
            lockedCount > 0 &&
            lockedCount <
            accountCount;


        var statusText =
            hasMixedStatus
                ? 'Có tài khoản bị khóa'
                : status.label;


        var statusClasses =
            status.code ===
            'BI_KHOA'

                ? 'bg-danger-soft text-mahogany-danger'

                : status.code ===
                  'CHO_XAC_THUC'

                    ? 'bg-surface-cream text-olive-grove'

                    : 'bg-success-soft text-primary';


        var accountChips =
            user.accounts
                .map(
                    renderAccountChip
                )
                .join('');


        var actionHtml =
            renderCardActions(
                user
            );


        return (

            '<article ' +

            'class="' +

            'p-space-lg ' +
            'rounded-2xl ' +
            'bg-surface-white ' +
            'shadow-sm ' +
            'hover:shadow-md ' +
            'transition-all ' +
            'relative overflow-hidden' +

            '">' +

            '<div class="flex flex-col lg:flex-row lg:items-center gap-space-md">' +


            '<div class="flex-1 min-w-0">' +


            '<div class="flex flex-wrap items-center gap-2">' +

            '<h2 class="font-headline-sm text-headline-sm text-primary tracking-tight">' +
            escape(user.hoTen) +
            '</h2>' +


            '<span class="font-caption text-caption text-text-muted font-mono">' +
            'UID-' +
            uid +
            '</span>' +


            '<span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full ' +
            statusClasses +
            ' font-label-sm text-label-sm font-bold">' +

            '<span class="material-symbols-outlined text-[14px]">' +
            status.icon +
            '</span>' +

            escape(statusText) +

            '</span>' +


            (
                user.isMultiAccount

                    ? '<span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-surface-cream text-olive-grove font-label-sm text-label-sm">' +
                      '<span class="material-symbols-outlined text-[14px]">layers</span>' +
                      escape(accountCount + ' tài khoản') +
                      '</span>'

                    : ''
            ) +

            '</div>' +


            '<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-x-space-md gap-y-1 mt-2 text-text-muted font-body-sm text-body-sm">' +


            '<div class="flex items-center gap-1.5 truncate">' +

            '<span class="material-symbols-outlined text-[16px] text-olive-grove shrink-0">' +
            'mail' +
            '</span>' +

            '<span class="text-body-text select-all truncate">' +
            escape(
                user.email ||
                'Chưa cập nhật'
            ) +
            '</span>' +

            '</div>' +


            '<div class="flex items-center gap-1.5">' +

            '<span class="material-symbols-outlined text-[16px] text-olive-grove shrink-0">' +
            'call' +
            '</span>' +

            '<span class="text-body-text select-all">' +
            escape(
                user.soDienThoai ||
                'Chưa cập nhật'
            ) +
            '</span>' +

            '</div>' +


            '<div class="flex items-center gap-1.5">' +

            '<span class="material-symbols-outlined text-[16px] text-olive-grove shrink-0">' +
            'calendar_today' +
            '</span>' +

            '<span class="text-body-text">' +
            'Tạo: ' +
            escape(created) +
            '</span>' +

            '</div>' +


            '</div>' +


            '<div class="flex items-center gap-2 flex-wrap mt-3">' +

            '<span class="font-label-sm text-label-sm text-text-muted uppercase">' +
            'Tài khoản & Quyền hạn:' +
            '</span>' +

            accountChips +


            (
                lockedCount > 0

                    ? '<span class="px-2 py-0.5 rounded-md bg-danger-soft/70 text-mahogany-danger font-label-sm text-label-sm">' +
                      (
                          lockedCount === 1
                              ? '1 tài khoản bị khóa'
                              : lockedCount +
                                ' tài khoản bị khóa'
                      ) +
                      '</span>'

                    : ''
            ) +

            '</div>' +


            '</div>' +


            '<div class="flex items-center justify-end gap-space-sm pt-space-sm lg:pt-0 shrink-0">' +

            '<a ' +

            'href="/admin/users/' +
            encodeURIComponent(
                user.maND
            ) +
            '" ' +

            'class="' +
            (
                user.statusCode === 'BI_KHOA'
                    ? 'px-space-lg py-2.5 rounded-lg bg-surface-cream text-primary hover:bg-surface-variant font-label-lg text-label-lg shadow-sm transition-all inline-flex items-center gap-2'
                    : 'px-space-lg py-2.5 rounded-lg bg-primary text-on-primary hover:bg-primary-container font-label-lg text-label-lg shadow-sm hover:shadow transition-all inline-flex items-center gap-2'
            ) +
            '">' +

            '<span class="material-symbols-outlined text-[18px]">' +
            'visibility' +
            '</span>' +

            '<span>Xem hồ sơ</span>' +

            '</a>' +


            actionHtml +


            '</div>' +


            '</div>' +

            '</article>'

        );

    }


    function renderAccountChip(
        account
    ) {

        var icon =
            getRoleIcon(
                account.vaiTro
            );


        var role =
            ROLE_LABELS[
                account.vaiTro
            ] ||
            account.vaiTro ||
            'Chưa xác định';


        var statusClass =
            account.trangThai ===
            'BI_KHOA'

                ? 'bg-danger-soft text-mahogany-danger'

                : account.trangThai ===
                  'CHO_XAC_THUC'

                    ? 'bg-surface-cream text-text-muted'

                    : 'bg-surface-cream text-text-muted';


        var suffix =
            account.trangThai ===
            'BI_KHOA'

                ? ' · Bị khóa'

                : '';


        return (

            '<span class="' +
            'inline-flex items-center gap-1.5 ' +
            'px-3 py-1 rounded-lg ' +
            statusClass +
            ' font-label-md text-label-md' +
            '">' +

            '<span class="material-symbols-outlined text-[16px]">' +
            icon +
            '</span>' +

            '<span>' +
            escape(role) +
            ' #' +
            escape(account.maTK) +
            escape(suffix) +
            '</span>' +

            '</span>'

        );

    }


    function getRoleIcon(
        role
    ) {

        switch (role) {

            case 'CHU_CUA_HANG':
                return 'storefront';

            case 'QUAN_TRI_VIEN':
                return 'admin_panel_settings';

            case 'QUAN_LY':
                return 'manage_accounts';

            case 'DON_VI_VAN_CHUYEN':
                return 'local_shipping';

            case 'KHACH_HANG':
            default:
                return 'shopping_bag';

        }

    }


    function getUserStatusMeta(
        user
    ) {

        if (
            user.statusCode ===
            'BI_KHOA'
        ) {

            return {

                code: 'BI_KHOA',

                label: STATUS_LABELS.BI_KHOA,

                icon: 'lock'

            };

        }


        if (
            user.statusCode ===
            'CHO_XAC_THUC'
        ) {

            return {

                code: 'CHO_XAC_THUC',

                label: STATUS_LABELS.CHO_XAC_THUC,

                icon: 'schedule'

            };

        }


        return {

            code: 'HOAT_DONG',

            label: STATUS_LABELS.HOAT_DONG,

            icon: 'check_circle'

        };

    }


    function renderCardActions(
        user
    ) {

        var currentUser =
            Auth.getUser();


        var isSelf =
            currentUser &&
            Number(
                currentUser.maND
            ) === Number(
                user.maND
            );


        /*
         * Người dùng đang có tài khoản bị khóa:
         * hiển thị nút mở khóa.
         */

        if (
            user.lockedAccounts.length >
            0
        ) {

            /*
             * Không tự xử lý tài khoản hiện tại.
             */

            if (isSelf) {

                return (

                    '<span class="px-3.5 py-2.5 rounded-lg bg-surface-cream text-text-muted font-label-md text-label-md">' +

                    'Tài khoản hiện tại' +

                    '</span>'

                );

            }


            return (

                '<button ' +

                'type="button" ' +

                'data-user-action="unlock" ' +

                'data-user-id="' +
                escape(user.maND) +
                '" ' +

                'class="px-3.5 py-2.5 rounded-lg bg-success-soft text-on-secondary-container hover:bg-secondary-container font-label-md text-label-md transition-colors inline-flex items-center gap-1.5 shadow-sm">' +

                '<span class="material-symbols-outlined text-[18px]">' +
                'lock_open' +
                '</span>' +

                '<span>' +
                (
                    user.lockedAccounts.length >
                    1
                        ? 'Mở khóa ' +
                          user.lockedAccounts.length +
                          ' tài khoản'
                        : 'Mở khóa'
                ) +
                '</span>' +

                '</button>'

            );

        }


        /*
         * Người dùng bình thường:
         * thêm nút quản lý tài khoản.
         */

        return (

            '<a ' +

            'href="/admin/accounts?tuKhoa=' +
            encodeURIComponent(
                user.email
            ) +
            '" ' +

            'class="px-3.5 py-2.5 rounded-lg bg-surface-cream text-primary hover:bg-surface-variant font-label-md text-label-md transition-colors inline-flex items-center gap-1.5">' +

            '<span class="material-symbols-outlined text-[18px]">' +
            'manage_accounts' +
            '</span>' +

            '<span>Quản lý tài khoản</span>' +

            '</a>'

        );

    }


    // =========================================================
    // PAGINATION
    // =========================================================

    function renderPagination() {

        var total =
            state.filteredUsers.length;


        var pageCount =
            Math.max(
                1,
                Math.ceil(
                    total /
                    state.pageSize
                )
            );


        if (
            state.page >
            pageCount
        ) {

            state.page =
                pageCount;

        }


        var start =
            total === 0
                ? 0
                : (
                    (
                        state.page -
                        1
                    ) *
                    state.pageSize
                ) + 1;


        var end =
            Math.min(
                state.page *
                state.pageSize,
                total
            );


        setText(
            'paginationInfo',
            'Hiển thị ' +
            start +
            '–' +
            end +
            ' / ' +
            total +
            ' người dùng'
        );


        var buttons =
            [];


        buttons.push(
            createPageButton(
                'chevron_left',
                state.page - 1,
                state.page <= 1,
                true
            )
        );


        var pages =
            buildPageNumbers(
                state.page,
                pageCount
            );


        pages.forEach(
            function (page) {

                if (
                    page === '...'
                ) {

                    buttons.push(
                        '<span class="px-1 text-text-muted">' +
                        '...' +
                        '</span>'
                    );

                } else {

                    buttons.push(
                        createPageButton(
                            String(page),
                            page,
                            false,
                            false,
                            page === state.page
                        )
                    );

                }

            }
        );


        buttons.push(
            createPageButton(
                'chevron_right',
                state.page + 1,
                state.page >= pageCount,
                true
            )
        );


        $('paginationButtons').innerHTML =
            buttons.join('');


        $('paginationButtons')
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


                            state.page =
                                page;


                            renderUsers();


                            window.scrollTo(
                                {
                                    top: 0,
                                    behavior: 'smooth'
                                }
                            );

                        }
                    );

                }
            );

    }


    function createPageButton(
        label,
        page,
        disabled,
        icon,
        active
    ) {

        if (icon) {

            return (

                '<button ' +
                'type="button" ' +
                'data-page="' +
                page +
                '" ' +
                (
                    disabled
                        ? 'disabled '
                        : ''
                ) +
                'class="w-9 h-9 rounded-lg ' +

                (
                    disabled
                        ? 'bg-surface-cream text-text-muted opacity-40 '
                        : 'bg-surface-cream text-text-muted hover:bg-surface-variant '
                ) +

                'flex items-center justify-center transition-colors">' +

                '<span class="material-symbols-outlined text-[20px]">' +
                escape(label) +
                '</span>' +

                '</button>'

            );

        }


        return (

            '<button ' +
            'type="button" ' +
            'data-page="' +
            page +
            '" ' +

            'class="w-9 h-9 rounded-lg ' +

            (
                active
                    ? 'bg-primary text-on-primary font-label-md text-label-md shadow-sm'
                    : 'bg-surface-cream text-on-surface hover:bg-surface-variant font-label-md text-label-md transition-colors'
            ) +

            '">' +

            escape(label) +

            '</button>'

        );

    }


    function buildPageNumbers(
        current,
        total
    ) {

        if (total <= 7) {

            return Array
                .from(
                    {
                        length: total
                    },
                    function (_, index) {
                        return index + 1;
                    }
                );

        }


        var pages = [
            1
        ];


        if (current > 4) {

            pages.push('...');

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

            pages.push(page);

        }


        if (current < total - 3) {

            pages.push('...');

        }


        pages.push(total);


        return pages;

    }


    // =========================================================
    // LOAD
    // =========================================================

    function loadAccounts() {

        if (
            state.loading
        ) {

            return;

        }


        state.loading =
            true;


        hide('usersError');
        hide('usersEmpty');
        hide('usersList');
        hide('usersPagination');

        show('usersLoading');


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


                state.users =
                    groupAccountsByUser(
                        state.accounts
                    );


                calculateStats();


                state.page =
                    1;


                applyFilters();


                var now =
                    new Date();


                setText(
                    'usersLastUpdated',
                    'Cập nhật ' +
                    formatDateTime(
                        now
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


                setText(
                    'usersErrorMessage',
                    err.message ||
                    'Không thể tải danh sách người dùng.'
                );


                show('usersError');

            }
        )

        .finally(
            function () {

                state.loading =
                    false;

                hide('usersLoading');

            }
        );

    }


    // =========================================================
    // SEARCH UI
    // =========================================================

    function updateSearchClearButton() {

        var button =
            $('btnClearSearch');


        if (
            $('userSearch').value
                .trim()
        ) {

            button.classList.remove(
                'hidden'
            );

            button.classList.add(
                'flex'
            );

        } else {

            button.classList.add(
                'hidden'
            );

            button.classList.remove(
                'flex'
            );

        }

    }


    // =========================================================
    // UNLOCK
    // =========================================================

    function unlockUser(
        maND
    ) {

        var user =
            state.users.find(
                function (item) {

                    return Number(
                        item.maND
                    ) === Number(
                        maND
                    );

                }
            );


        if (
            !user ||
            !user.lockedAccounts.length
        ) {

            return;

        }


        var lockedAccounts =
            user.lockedAccounts;


        var accountWord =
            lockedAccounts.length === 1
                ? 'tài khoản'
                : 'tài khoản';


        var confirmMessage =

            'Bạn có chắc muốn mở khóa ' +
            lockedAccounts.length +
            ' ' +
            accountWord +
            ' của ' +
            user.hoTen +
            '?';


        if (
            !window.confirm(
                confirmMessage
            )
        ) {

            return;

        }


        var requests =
            lockedAccounts.map(
                function (account) {

                    return OB.request(
                        'PUT',
                        '/api/admin/accounts/' +
                        encodeURIComponent(
                            account.maTK
                        ) +
                        '/unlock'
                    );

                }
            );


        Promise.all(
            requests
        )

        .then(
            function () {

                OB.toast(
                    'Đã mở khóa tài khoản thành công.',
                    'success'
                );


                loadAccounts();

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
    // CSV EXPORT
    // =========================================================

    function exportUsers() {

        var rows =
            state.filteredUsers;


        if (!rows.length) {

            OB.toast(
                'Không có dữ liệu để xuất.',
                'danger'
            );

            return;

        }


        var header = [

            'MaND',

            'HoTen',

            'Email',

            'SoDienThoai',

            'TrangThai',

            'SoTaiKhoan',

            'VaiTro',

            'NgayTao'

        ];


        var csvRows =
            [header];


        rows.forEach(
            function (user) {

                csvRows.push([

                    user.maND,

                    user.hoTen,

                    user.email,

                    user.soDienThoai || '',

                    STATUS_LABELS[
                        user.statusCode
                    ] || user.statusCode,

                    user.accounts.length,

                    user.roles
                        .map(
                            function (role) {
                                return ROLE_LABELS[
                                    role
                                ] || role;
                            }
                        )
                        .join(' | '),

                    user.createdAt
                        ? formatDate(
                            user.createdAt
                        )
                        : ''

                ]);

            }
        );


        var csv =
            csvRows
                .map(
                    function (row) {

                        return row
                            .map(
                                function (cell) {

                                    return '"' +
                                        String(
                                            cell == null
                                                ? ''
                                                : cell
                                        )
                                        .replace(
                                            /"/g,
                                            '""'
                                        ) +
                                        '"';

                                }
                            )
                            .join(',');

                    }
                )
                .join('\r\n');


        var blob =
            new Blob(
                [
                    '\uFEFF' +
                    csv
                ],
                {
                    type:
                        'text/csv;charset=utf-8;'
                }
            );


        var url =
            URL.createObjectURL(
                blob
            );


        var link =
            document.createElement(
                'a'
            );


        link.href =
            url;


        link.download =
            'oldbook-users-' +
            new Date()
                .toISOString()
                .slice(
                    0,
                    10
                ) +
            '.csv';


        document.body.appendChild(
            link
        );


        link.click();


        link.remove();


        URL.revokeObjectURL(
            url
        );

    }


    // =========================================================
    // EVENT HANDLERS
    // =========================================================

    function bindEvents() {


        // SEARCH

        $('userSearch')
            .addEventListener(
                'input',
                function () {

                    updateSearchClearButton();

                    applyFilters();

                    updateQuickFilterUI(
                        null
                    );

                }
            );


        // CLEAR SEARCH

        $('btnClearSearch')
            .addEventListener(
                'click',
                function () {

                    $('userSearch').value =
                        '';

                    applyFilters();

                    updateSearchClearButton();

                }
            );


        // ROLE

        $('userRoleFilter')
            .addEventListener(
                'change',
                function () {

                    applyFilters();

                    updateQuickFilterUI(
                        null
                    );

                }
            );


        // STATUS

        $('userStatusFilter')
            .addEventListener(
                'change',
                function () {

                    applyFilters();

                    updateQuickFilterUI(
                        null
                    );

                }
            );


        // ACCOUNT COUNT

        $('userAccountFilter')
            .addEventListener(
                'change',
                function () {

                    applyFilters();

                    updateQuickFilterUI(
                        null
                    );

                }
            );


        // CREATED DATE

        $('createdDateFilter')
            .addEventListener(
                'change',
                function () {

                    applyFilters();

                }
            );


        // STORE OWNER

        $('advancedStoreOwner')
            .addEventListener(
                'change',
                function () {

                    applyFilters();

                }
            );


        // MULTI ACCOUNT

        $('advancedMultiAccount')
            .addEventListener(
                'change',
                function () {

                    applyFilters();

                }
            );


        // PAGE SIZE

        $('pageSizeSelect')
            .addEventListener(
                'change',
                function () {

                    state.pageSize =
                        Number(
                            this.value
                        ) || 4;

                    state.page =
                        1;

                    renderUsers();

                }
            );


        // ADVANCED TOGGLE

        $('toggleAdvancedFilters')
            .addEventListener(
                'click',
                function () {

                    $('advancedFilters')
                        .classList.toggle(
                            'hidden'
                        );

                }
            );


        // RESET

        $('btnResetFilters')
            .addEventListener(
                'click',
                function () {

                    resetFilterInputs();

                    applyFilters();

                    updateQuickFilterUI(
                        'all'
                    );

                    updateSearchClearButton();

                }
            );


        // QUICK FILTERS

        document
            .querySelectorAll(
                '[data-quick-filter]'
            )
            .forEach(
                function (button) {

                    button.addEventListener(
                        'click',
                        function () {

                            applyQuickFilter(
                                button.getAttribute(
                                    'data-quick-filter'
                                )
                            );

                        }
                    );

                }
            );


        // EXPORT

        $('btnExportUsers')
            .addEventListener(
                'click',
                exportUsers
            );


        // RETRY

        $('btnRetryUsers')
            .addEventListener(
                'click',
                loadAccounts
            );


        // CARD ACTIONS

        $('usersList')
            .addEventListener(
                'click',
                function (event) {

                    var button =
                        event.target.closest(
                            '[data-user-action="unlock"]'
                        );


                    if (!button) {
                        return;
                    }


                    unlockUser(
                        button.getAttribute(
                            'data-user-id'
                        )
                    );

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
                'usersLoading'
            );


            setText(
                'usersErrorMessage',
                'Bạn không có quyền truy cập khu vực quản trị.'
            );


            show(
                'usersError'
            );


            return;

        }


        bindEvents();


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