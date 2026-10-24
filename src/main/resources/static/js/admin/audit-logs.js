(function () {

    'use strict';


    var OB = window.OldBook;

    if (!OB) {
        return;
    }


    var Auth = OB.auth;


    // =========================================================
    // STATE
    // =========================================================

    var state = {

        page: 0,

        size: 20,

        totalElements: 0,

        totalPages: 0,

        logs: [],

        selectedLog: null,

        loading: false

    };


    // =========================================================
    // CONSTANTS
    // =========================================================

    var ROLE_LABELS = OB.roleLabels || {

        KHACH_HANG: 'Khách hàng',

        CHU_CUA_HANG: 'Chủ cửa hàng',

        DON_VI_VAN_CHUYEN: 'Đơn vị vận chuyển',

        QUAN_LY: 'Quản lý',

        QUAN_TRI_VIEN: 'Quản trị viên'

    };


    // =========================================================
    // DOM
    // =========================================================

    function $(id) {

        return document.getElementById(id);

    }


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


    function setText(
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


    function formatDateTime(
        value
    ) {

        if (!value) {
            return '—';
        }


        var date =
            new Date(
                value
            );


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


    function formatDate(
        value
    ) {

        if (!value) {
            return '—';
        }


        var date =
            new Date(
                value
            );


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


    function normalize(
        value
    ) {

        return value == null
            ? ''
            : String(value)
                .trim()
                .toLowerCase();

    }


    function getInitials(
        name
    ) {

        if (!name) {
            return '?';
        }


        var parts =
            String(name)
                .trim()
                .split(/\s+/);


        if (parts.length === 1) {

            return parts[0]
                .substring(0, 2)
                .toUpperCase();

        }


        return (
            parts[0][0] +
            parts[parts.length - 1][0]
        )
            .toUpperCase();

    }


    // =========================================================
    // FILTER PARAMS
    // =========================================================

    function buildFilterParams(
        includePage
    ) {

        var params =
            new URLSearchParams();


        var keyword =
            $('input-search-log')
                .value
                .trim();


        var actor =
            $('select-actor')
                .value;


        var action =
            $('select-action')
                .value;


        var ip =
            $('input-ip')
                .value
                .trim();


        var from =
            $('input-date-from')
                .value;


        var to =
            $('input-date-to')
                .value;


        if (keyword) {

            params.set(
                'tuKhoa',
                keyword
            );

        }


        if (actor) {

            params.set(
                'vaiTro',
                actor
            );

        }


        if (action) {

            params.set(
                'hanhDong',
                action
            );

        }


        if (ip) {

            params.set(
                'diaChiIp',
                ip
            );

        }


        if (from) {

            params.set(
                'tuNgay',
                from
            );

        }


        if (to) {

            params.set(
                'denNgay',
                to
            );

        }


        if (includePage) {

            params.set(
                'page',
                String(
                    state.page
                )
            );


            params.set(
                'size',
                String(
                    state.size
                )
            );

        }


        return params;

    }


    // =========================================================
    // LOAD SUMMARY
    // =========================================================

    function loadSummary() {

        return OB.request(
            'GET',
            '/api/admin/audit-logs/summary'
        )

        .then(
            function (data) {

                setText(
                    'summaryActionsToday',
                    data.hanhDongHomNay
                );


                setText(
                    'summaryLockUnlock',
                    data.khoaMoKhoaHomNay
                );


                setText(
                    'summaryRoleChanges',
                    data.phanQuyenHomNay
                );


                setText(
                    'summaryIps',
                    data.soIpGhiNhanHomNay
                );

            }
        );

    }


    // =========================================================
    // LOAD LOGS
    // =========================================================

    function loadLogs(
        resetPage
    ) {

        if (
            state.loading
        ) {

            return;

        }


        if (
            resetPage
        ) {

            state.page = 0;

        }


        state.loading =
            true;


        hide(
            'auditError'
        );

        hide(
            'auditEmpty'
        );

        hide(
            'auditTableSection'
        );

        show(
            'auditLoading'
        );


        var params =
            buildFilterParams(
                true
            );


        OB.request(
            'GET',
            '/api/admin/audit-logs?' +
            params.toString()
        )

        .then(
            function (data) {

                state.logs =
                    Array.isArray(
                        data.content
                    )
                        ? data.content
                        : [];


                state.totalElements =
                    Number(
                        data.totalElements ||
                        0
                    );


                state.totalPages =
                    Number(
                        data.totalPages ||
                        0
                    );


                if (
                    state.logs.length
                ) {

                    renderLogs();

                    show(
                        'auditTableSection'
                    );

                } else {

                    setText(
                        'auditResultBadge',
                        '0 bản ghi'
                    );


                    setText(
                        'auditTotalInfo',
                        '0 bản ghi'
                    );


                    hide(
                        'auditTableSection'
                    );

                    show(
                        'auditEmpty'
                    );

                }

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
                    'auditErrorMessage',
                    err.message ||
                    'Không thể tải nhật ký hệ thống.'
                );


                show(
                    'auditError'
                );

            }
        )

        .finally(
            function () {

                state.loading =
                    false;


                hide(
                    'auditLoading'
                );

            }
        );

    }


    // =========================================================
    // RENDER LOGS
    // =========================================================

    function renderLogs() {

        setText(
            'auditResultBadge',

            state.logs.length +
            ' / ' +
            state.totalElements +
            ' bản ghi'
        );


        setText(
            'auditTotalInfo',

            'Tổng ' +
            state.totalElements +
            ' bản ghi'
        );


        $('auditRows').innerHTML =
            state.logs
                .map(
                    renderLogRow
                )
                .join('');


        renderPagination();

    }


    function renderLogRow(
        log
    ) {

        var action =
            log.hanhDong ||
            'KHÔNG_XÁC_ĐỊNH';


        var actionMeta =
            getActionMeta(
                action
            );


        var actor =
            log.tenNguoiThucHien ||
            (
                log.maTK
                    ? 'Tài khoản #' +
                      log.maTK
                    : 'Không xác định'
            );


        var role =
            ROLE_LABELS[
                log.vaiTro
            ] ||
            log.vaiTro ||
            '—';


        var target =
            buildTargetText(
                log
            );


        var description =
            log.moTa ||
            'Không có mô tả';


        var initials =
            getInitials(
                log.tenNguoiThucHien
            );


        return (

            '<tr class="hover:bg-surface-cream/50 transition-colors group">' +


            // TIME

            '<td class="py-4 px-space-md whitespace-nowrap">' +

            '<div class="flex flex-col">' +

            '<span class="font-headline-sm text-headline-sm text-primary">' +
            escape(
                getTimeOnly(
                    log.thoiGian
                )
            ) +
            '</span>' +

            '<span class="font-caption text-caption text-text-muted">' +
            escape(
                formatDate(
                    log.thoiGian
                )
            ) +
            '</span>' +

            '</div>' +

            '</td>' +


            // ACTOR

            '<td class="py-4 px-space-md whitespace-nowrap">' +

            '<div class="flex items-center gap-space-sm">' +

            '<div class="w-8 h-8 rounded-full ' +
            actionMeta.actorColor +
            ' flex items-center justify-center font-label-sm text-label-sm shrink-0">' +

            escape(initials) +

            '</div>' +

            '<div class="flex flex-col min-w-0">' +

            '<span class="font-label-lg text-label-lg text-primary truncate max-w-[190px]">' +
            escape(actor) +
            '</span>' +

            '<span class="font-caption text-caption text-text-muted">' +

            escape(role) +

            (
                log.diaChiIp
                    ? ' • IP ' +
                      escape(
                          log.diaChiIp
                      )
                    : ''
            ) +

            '</span>' +

            '</div>' +

            '</div>' +

            '</td>' +


            // ACTION

            '<td class="py-4 px-space-md whitespace-nowrap">' +

            '<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full ' +
            actionMeta.badge +
            ' font-label-sm text-label-sm font-semibold">' +

            '<span class="material-symbols-outlined text-[14px]">' +
            actionMeta.icon +
            '</span>' +

            escape(action) +

            '</span>' +

            '</td>' +


            // TARGET

            '<td class="py-4 px-space-md whitespace-nowrap">' +

            '<div class="flex flex-col">' +

            '<span class="font-label-lg text-label-lg text-body-text">' +
            escape(target.title) +
            '</span>' +

            '<span class="font-caption text-caption text-text-muted">' +
            escape(target.subtitle) +
            '</span>' +

            '</div>' +

            '</td>' +


            // DESCRIPTION

            '<td class="py-4 px-space-md max-w-xs xl:max-w-md">' +

            '<p class="font-body-sm text-body-sm text-on-surface-variant truncate" title="' +
            escape(description) +
            '">' +

            escape(description) +

            '</p>' +

            '</td>' +


            // ACTION BUTTON

            '<td class="py-4 px-space-md text-right whitespace-nowrap">' +

            '<button ' +

            'type="button" ' +

            'data-audit-id="' +
            escape(
                log.maNhatKy
            ) +
            '" ' +

            'class="btn-view-audit inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-surface-container hover:bg-primary-container hover:text-on-primary-container text-primary font-label-md text-label-md transition-all shadow-sm">' +

            '<span class="material-symbols-outlined text-[16px]">' +
            'visibility' +
            '</span>' +

            '<span>Xem chi tiết</span>' +

            '</button>' +

            '</td>' +

            '</tr>'

        );

    }


    function getTimeOnly(
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

            return '—';

        }


        return date.toLocaleTimeString(
            'vi-VN',
            {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            }
        );

    }


    function buildTargetText(
        log
    ) {

        var type =
            log.loaiDoiTuong ||
            'ĐỐI_TƯỢNG';


        var id =
            log.maDoiTuong;


        var title =
            type;


        var subtitle =
            id == null
                ? 'Không có mã đối tượng'
                : 'Mã đối tượng #' +
                  id;


        if (
            type ===
            'TAI_KHOAN'
        ) {

            title =
                'Tài khoản #' +
                (
                    id == null
                        ? '—'
                        : id
                );

        }


        if (
            type ===
            'NGUOI_DUNG'
        ) {

            title =
                'Người dùng #' +
                (
                    id == null
                        ? '—'
                        : id
                );

        }


        if (
            type ===
            'CUA_HANG'
        ) {

            title =
                'Cửa hàng #' +
                (
                    id == null
                        ? '—'
                        : id
                );

        }


        if (
            type ===
            'SACH'
        ) {

            title =
                'Sách #' +
                (
                    id == null
                        ? '—'
                        : id
                );

        }


        return {

            title: title,

            subtitle: subtitle

        };

    }


    // =========================================================
    // ACTION META
    // =========================================================

    function getActionMeta(
        action
    ) {

        switch (action) {

            case 'KHOA_TAI_KHOAN':

                return {

                    icon:
                        'lock',

                    badge:
                        'bg-danger-soft text-mahogany-danger',

                    actorColor:
                        'bg-primary-container text-on-primary-container'

                };


            case 'MO_KHOA_TAI_KHOAN':

                return {

                    icon:
                        'lock_open',

                    badge:
                        'bg-surface-container text-primary',

                    actorColor:
                        'bg-primary-container text-on-primary-container'

                };


            case 'THAY_DOI_VAI_TRO':

                return {

                    icon:
                        'swap_horiz',

                    badge:
                        'bg-success-soft text-on-secondary-container',

                    actorColor:
                        'bg-primary-container text-on-primary-container'

                };


            case 'TAO_TAI_KHOAN':

                return {

                    icon:
                        'person_add',

                    badge:
                        'bg-surface-container text-primary',

                    actorColor:
                        'bg-primary-container text-on-primary-container'

                };


            case 'DUYET_CUA_HANG':

                return {

                    icon:
                        'store',

                    badge:
                        'bg-surface-variant text-primary',

                    actorColor:
                        'bg-secondary text-on-secondary'

                };


            case 'DUYET_SACH':

                return {

                    icon:
                        'menu_book',

                    badge:
                        'bg-secondary-container text-on-secondary-container',

                    actorColor:
                        'bg-secondary text-on-secondary'

                };


            case 'DONG_BO_VAN_CHUYEN':

                return {

                    icon:
                        'sync_alt',

                    badge:
                        'bg-surface-variant text-text-muted',

                    actorColor:
                        'bg-surface-container text-primary'

                };


            default:

                return {

                    icon:
                        'receipt_long',

                    badge:
                        'bg-surface-container text-primary',

                    actorColor:
                        'bg-surface-container text-primary'

                };

        }

    }


    // =========================================================
    // PAGINATION
    // =========================================================

    function renderPagination() {

        var total =
            state.totalPages;


        if (
            total <= 0
        ) {

            $('auditPagination')
                .innerHTML =
                '';

            return;

        }


        var html = '';


        html +=

            '<button ' +

            'type="button" ' +

            'data-page="' +
            (state.page - 1) +
            '" ' +

            (
                state.page <= 0
                    ? 'disabled '
                    : ''
            ) +

            'class="w-8 h-8 rounded-lg bg-surface-white flex items-center justify-center text-text-muted hover:text-primary shadow-sm ' +

            (
                state.page <= 0
                    ? 'opacity-50 cursor-not-allowed'
                    : ''
            ) +

            '">' +

            '<span class="material-symbols-outlined text-[18px]">' +
            'chevron_left' +
            '</span>' +

            '</button>';


        var pages =
            buildPageNumbers(
                state.page + 1,
                total
            );


        pages.forEach(
            function (pageNumber) {

                if (
                    pageNumber ===
                    '...'
                ) {

                    html +=
                        '<span class="px-1 text-text-muted">...</span>';

                    return;

                }


                var zeroBased =
                    pageNumber - 1;


                html +=

                    '<button ' +

                    'type="button" ' +

                    'data-page="' +
                    zeroBased +
                    '" ' +

                    'class="w-8 h-8 rounded-lg ' +

                    (
                        zeroBased ===
                        state.page

                            ? 'bg-primary text-on-primary'

                            : 'bg-surface-white text-body-text hover:bg-surface-cream'
                    ) +

                    ' font-label-md text-label-md shadow-sm">' +

                    pageNumber +

                    '</button>';

            }
        );


        html +=

            '<button ' +

            'type="button" ' +

            'data-page="' +
            (state.page + 1) +
            '" ' +

            (
                state.page >=
                total - 1
                    ? 'disabled '
                    : ''
            ) +

            'class="w-8 h-8 rounded-lg bg-surface-white flex items-center justify-center text-text-muted hover:text-primary shadow-sm ' +

            (
                state.page >=
                total - 1
                    ? 'opacity-50 cursor-not-allowed'
                    : ''
            ) +

            '">' +

            '<span class="material-symbols-outlined text-[18px]">' +
            'chevron_right' +
            '</span>' +

            '</button>';


        $('auditPagination').innerHTML =
            html;


        $('auditPagination')
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
                                page < 0 ||
                                page >= state.totalPages
                            ) {

                                return;

                            }


                            state.page =
                                page;


                            loadLogs(
                                false
                            );

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

            var result = [];


            for (
                var i = 1;
                i <= total;
                i++
            ) {

                result.push(
                    i
                );

            }


            return result;

        }


        var pages = [
            1
        ];


        if (
            current > 4
        ) {

            pages.push(
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

            pages.push(
                page
            );

        }


        if (
            current <
            total - 3
        ) {

            pages.push(
                '...'
            );

        }


        pages.push(
            total
        );


        return pages;

    }


    // =========================================================
    // MODAL
    // =========================================================

    function openModal(
        log
    ) {

        state.selectedLog =
            log;


        setText(
            'modalAuditId',
            'MÃ NHẬT KÝ: #' +
            log.maNhatKy
        );


        setText(
            'modalAuditAction',
            log.hanhDong ||
            'KHÔNG_XÁC_ĐỊNH'
        );


        setText(
            'modalActorName',
            log.tenNguoiThucHien ||
            (
                log.maTK
                    ? 'Tài khoản #' +
                      log.maTK
                    : 'Không xác định'
            )
        );


        setText(
            'modalActorInitials',
            getInitials(
                log.tenNguoiThucHien
            )
        );


        setText(
            'modalActorRole',
            'Vai trò: ' +
            (
                ROLE_LABELS[
                    log.vaiTro
                ] ||
                log.vaiTro ||
                '—'
            )
        );


        setText(
            'modalActorEmail',
            log.emailNguoiThucHien
                ? 'Email: ' +
                  log.emailNguoiThucHien
                : 'Email: —'
        );


        setText(
            'modalIp',
            log.diaChiIp ||
            'Không ghi nhận'
        );


        setText(
            'modalTime',
            formatDateTime(
                log.thoiGian
            )
        );


        var target =
            buildTargetText(
                log
            );


        setText(
            'modalTarget',
            target.title
        );


        setText(
            'modalDescription',
            log.moTa ||
            'Không có mô tả.'
        );


        setText(
            'modalAccountId',
            log.maTK == null
                ? '—'
                : '#' +
                  log.maTK
        );


        setText(
            'modalObjectType',
            log.loaiDoiTuong ||
            '—'
        );


        setText(
            'modalObjectId',
            log.maDoiTuong == null
                ? '—'
                : '#' +
                  log.maDoiTuong
        );


        var meta =
            getActionMeta(
                log.hanhDong
            );


        $('modalActionIcon')
            .className =
            'w-12 h-12 rounded-xl flex items-center justify-center shrink-0 shadow-sm ' +
            meta.badge;


        $('modalActionIcon')
            .innerHTML =
            '<span class="material-symbols-outlined text-[28px]">' +
            escape(meta.icon) +
            '</span>';


        show(
            'auditModal'
        );


        $('auditModal')
            .classList
            .add('flex');

    }


    function closeModal() {

        hide(
            'auditModal'
        );


        $('auditModal')
            .classList
            .remove('flex');


        state.selectedLog =
            null;

    }


    function copyAuditId() {

        if (
            !state.selectedLog
        ) {

            return;

        }


        var value =
            '#' +
            state.selectedLog.maNhatKy;


        if (
            navigator.clipboard &&
            navigator.clipboard.writeText
        ) {

            navigator.clipboard
                .writeText(value)
                .then(
                    function () {

                        OB.toast(
                            'Đã sao chép mã nhật ký.',
                            'success'
                        );

                    }
                )
                .catch(
                    function () {

                        fallbackCopy(
                            value
                        );

                    }
                );

            return;

        }


        fallbackCopy(
            value
        );

    }


    function fallbackCopy(
        value
    ) {

        var textarea =
            document.createElement(
                'textarea'
            );


        textarea.value =
            value;


        document.body.appendChild(
            textarea
        );


        textarea.select();


        try {

            document.execCommand(
                'copy'
            );


            OB.toast(
                'Đã sao chép mã nhật ký.',
                'success'
            );

        } catch (e) {

            OB.toast(
                value,
                'success'
            );

        }


        textarea.remove();

    }


    // =========================================================
    // EXPORT
    // =========================================================

    function exportLogs() {

        var params =
            buildFilterParams(
                false
            );


        var button =
            $('btn-export-log');


        var oldHtml =
            button.innerHTML;


        button.disabled =
            true;


        button.innerHTML =
            '<span class="material-symbols-outlined text-[18px] animate-spin">' +
            'progress_activity' +
            '</span>' +
            '<span>Đang xuất CSV...</span>';


        OB.request(
            'GET',
            '/api/admin/audit-logs/export?' +
            params.toString()
        )

        .then(
            function (logs) {

                var list =
                    Array.isArray(logs)
                        ? logs
                        : [];


                if (
                    !list.length
                ) {

                    OB.toast(
                        'Không có dữ liệu log để xuất.',
                        'danger'
                    );

                    return;

                }


                downloadCsv(
                    list
                );


                OB.toast(
                    'Đã xuất ' +
                    list.length +
                    ' bản ghi CSV.',
                    'success'
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


                OB.toast(
                    err.message ||
                    'Không thể xuất nhật ký.',
                    'danger'
                );

            }
        )

        .finally(
            function () {

                button.disabled =
                    false;


                button.innerHTML =
                    oldHtml;

            }
        );

    }


    function downloadCsv(
        logs
    ) {

        var rows = [

            [
                'Mã nhật ký',
                'Thời gian',
                'Mã tài khoản',
                'Người thực hiện',
                'Email',
                'Vai trò',
                'Hành động',
                'Loại đối tượng',
                'Mã đối tượng',
                'Mô tả',
                'Địa chỉ IP'
            ]

        ];


        logs.forEach(
            function (log) {

                rows.push([

                    log.maNhatKy,

                    formatDateTime(
                        log.thoiGian
                    ),

                    log.maTK == null
                        ? ''
                        : log.maTK,

                    log.tenNguoiThucHien ||
                    '',

                    log.emailNguoiThucHien ||
                    '',

                    ROLE_LABELS[
                        log.vaiTro
                    ] ||
                    log.vaiTro ||
                    '',

                    log.hanhDong ||
                    '',

                    log.loaiDoiTuong ||
                    '',

                    log.maDoiTuong == null
                        ? ''
                        : log.maDoiTuong,

                    log.moTa ||
                    '',

                    log.diaChiIp ||
                    ''

                ]);

            }
        );


        var csv =
            rows
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
            'oldbook-audit-logs-' +
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
    // QUICK DATE FILTER
    // =========================================================

    function applyLast7Days() {

        var today =
            new Date();


        var from =
            new Date(
                today.getTime() -
                (
                    6 *
                    24 *
                    60 *
                    60 *
                    1000
                )
            );


        $('input-date-from')
            .value =
            formatInputDate(
                from
            );


        $('input-date-to')
            .value =
            formatInputDate(
                today
            );


        loadLogs(
            true
        );

    }


    function applyCurrentMonth() {

        var today =
            new Date();


        var firstDay =
            new Date(
                today.getFullYear(),
                today.getMonth(),
                1
            );


        $('input-date-from')
            .value =
            formatInputDate(
                firstDay
            );


        $('input-date-to')
            .value =
            formatInputDate(
                today
            );


        loadLogs(
            true
        );

    }


    function formatInputDate(
        date
    ) {

        var year =
            date.getFullYear();


        var month =
            String(
                date.getMonth() + 1
            )
                .padStart(
                    2,
                    '0'
                );


        var day =
            String(
                date.getDate()
            )
                .padStart(
                    2,
                    '0'
                );


        return (
            year +
            '-' +
            month +
            '-' +
            day
        );

    }


    // =========================================================
    // RESET
    // =========================================================

    function resetFilters() {

        $('input-search-log')
            .value =
            '';


        $('select-actor')
            .value =
            '';


        $('select-action')
            .value =
            '';


        $('input-ip')
            .value =
            '';


        $('input-date-from')
            .value =
            '';


        $('input-date-to')
            .value =
            '';


        loadLogs(
            true
        );

    }


    // =========================================================
    // EVENTS
    // =========================================================

    function bindEvents() {


        $('btn-apply-filter')
            .addEventListener(
                'click',
                function () {

                    loadLogs(
                        true
                    );

                }
            );


        $('btn-reset-filter')
            .addEventListener(
                'click',
                resetFilters
            );


        $('btn-last-7-days')
            .addEventListener(
                'click',
                applyLast7Days
            );


        $('btn-current-month')
            .addEventListener(
                'click',
                applyCurrentMonth
            );


        $('btn-refresh')
            .addEventListener(
                'click',
                function () {

                    Promise.all([
                        loadSummary(),
                        loadLogs(false)
                    ]);

                }
            );


        $('btn-export-log')
            .addEventListener(
                'click',
                exportLogs
            );


        $('btn-retry-audit')
            .addEventListener(
                'click',
                function () {

                    Promise.all([
                        loadSummary(),
                        loadLogs(false)
                    ]);

                }
            );


        $('auditPageSize')
            .addEventListener(
                'change',
                function () {

                    state.size =
                        Number(
                            this.value
                        ) || 20;


                    state.page =
                        0;


                    loadLogs(
                        false
                    );

                }
            );


        $('auditRows')
            .addEventListener(
                'click',
                function (event) {

                    var button =
                        event.target.closest(
                            '.btn-view-audit'
                        );


                    if (!button) {
                        return;
                    }


                    var id =
                        Number(
                            button.getAttribute(
                                'data-audit-id'
                            )
                        );


                    var log =
                        state.logs.find(
                            function (item) {

                                return Number(
                                    item.maNhatKy
                                ) === id;

                            }
                        );


                    if (log) {

                        openModal(
                            log
                        );

                    }

                }
            );


        $('btnCloseAuditModal')
            .addEventListener(
                'click',
                closeModal
            );


        $('btnCloseAuditModalFooter')
            .addEventListener(
                'click',
                closeModal
            );


        $('btnCopyAuditId')
            .addEventListener(
                'click',
                copyAuditId
            );


        $('auditModal')
            .addEventListener(
                'click',
                function (event) {

                    if (
                        event.target ===
                        $('auditModal')
                    ) {

                        closeModal();

                    }

                }
            );


        document.addEventListener(
            'keydown',
            function (event) {

                if (
                    event.key ===
                    'Escape'
                ) {

                    closeModal();

                }

            }
        );


        [
            'input-search-log',
            'input-ip'
        ]
        .forEach(
            function (id) {

                $(id)
                    .addEventListener(
                        'keydown',
                        function (event) {

                            if (
                                event.key ===
                                'Enter'
                            ) {

                                event.preventDefault();

                                loadLogs(
                                    true
                                );

                            }

                        }
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
                'auditLoading'
            );


            setText(
                'auditErrorMessage',
                'Bạn không có quyền xem nhật ký hệ thống.'
            );


            show(
                'auditError'
            );


            return;

        }


        bindEvents();


        /*
         * Mặc định 20 dòng/trang.
         */
        state.size =
            Number(
                $('auditPageSize')
                    .value
            ) || 20;


        Promise.all([
            loadSummary(),
            loadLogs(true)
        ]);

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