(function () {
    'use strict';

    var OB = window.OldBook;
    var UI = window.AdminUI;
    if (!OB || !UI) {
        return;
    }

    var $ = function (id) { return document.getElementById(id); };

    // Màu xen kẽ sáng/tối để hai phần nằm cạnh nhau trên vòng tròn luôn phân biệt được
    // (bản cũ dùng hai màu nâu sẫm gần như trùng nhau cho Quản lý và Quản trị viên).
    var ROLE_STYLES = {
        KHACH_HANG: { text: 'text-sand-dune', bg: 'bg-sand-dune' },
        CHU_CUA_HANG: { text: 'text-secondary', bg: 'bg-secondary' },
        DON_VI_VAN_CHUYEN: { text: 'text-outline', bg: 'bg-outline' },
        QUAN_LY: { text: 'text-secondary-fixed-dim', bg: 'bg-secondary-fixed-dim' },
        QUAN_TRI_VIEN: { text: 'text-primary', bg: 'bg-primary' }
    };
    var DEFAULT_ROLE_STYLE = { text: 'text-outline', bg: 'bg-outline' };

    // Vòng tròn bán kính 15.9155 có chu vi đúng bằng 100, nên stroke-dasharray nhận thẳng số phần trăm.
    var DONUT_CIRCLE = ' cx="18" cy="18" r="15.9155" fill="none" stroke="currentColor" stroke-width="3.8"';
    // Khe hở giữa hai phần liền nhau, tính theo phần trăm chu vi (khoảng 2px trên màn hình).
    var DONUT_GAP = 0.5;

    var loading = false;
    var loadedAt = null;

    // Khi một API trả 403, layout thay toàn bộ <main> bằng màn hình từ chối.
    // Các phản hồi về sau đó không còn chỗ để hiển thị nên phải bỏ qua.
    function pageGone() {
        return !$('dashRefresh');
    }

    function formatNumber(value) {
        return Number(value || 0).toLocaleString('vi-VN');
    }

    function formatPercent(part, total) {
        if (!total) {
            return '0%';
        }
        return (part / total * 100).toLocaleString('vi-VN', { maximumFractionDigits: 1 }) + '%';
    }

    function renderKpis(summary) {
        $('kpiNguoiDung').textContent = formatNumber(summary.tongNguoiDung);
        $('kpiTaiKhoan').textContent = formatNumber(summary.tongTaiKhoan);
        $('kpiChoXacThuc').textContent = formatNumber(summary.taiKhoanChoXacThuc);
        $('kpiHoatDong').textContent = formatNumber(summary.taiKhoanHoatDong);
        $('kpiHoatDongTyLe').textContent = formatPercent(summary.taiKhoanHoatDong, summary.tongTaiKhoan);
        $('kpiBiKhoa').textContent = formatNumber(summary.taiKhoanBiKhoa);
        $('kpiBiKhoaTyLe').textContent = formatPercent(summary.taiKhoanBiKhoa, summary.tongTaiKhoan);
    }

    function renderRoles(summary) {
        var roles = summary.phanBoVaiTro || [];
        var total = summary.tongTaiKhoan || 0;
        var hasData = roles.length > 0 && total > 0;

        $('dashRoleTotal').textContent = 'Tổng ' + formatNumber(total) + ' tài khoản';
        $('dashRoleEmpty').classList.toggle('hidden', hasData);
        $('dashRoleChart').classList.toggle('hidden', !hasData);
        if (!hasData) {
            return;
        }

        var arcs = '<circle class="text-surface-variant"' + DONUT_CIRCLE + '></circle>';
        var rows = '';
        var offset = 0;
        var gap = roles.length > 1 ? DONUT_GAP : 0;

        roles.forEach(function (role) {
            var style = ROLE_STYLES[role.vaiTro] || DEFAULT_ROLE_STYLE;
            var percent = role.soLuong / total * 100;

            arcs += '<circle class="' + style.text + '"' + DONUT_CIRCLE +
                ' stroke-dasharray="' + Math.max(percent - gap, 0.1).toFixed(2) + ' 100"' +
                ' stroke-dashoffset="' + (-offset).toFixed(2) + '"></circle>';
            offset += percent;

            rows += '<div class="space-y-1">' +
                '<div class="flex items-center justify-between">' +
                '<span class="flex items-center gap-2 font-label-md text-label-md text-on-surface">' +
                '<span class="w-2.5 h-2.5 rounded-full inline-block ' + style.bg + '"></span>' +
                OB.escapeHtml(role.tenHienThi || role.vaiTro) +
                '</span>' +
                '<span class="font-label-md text-label-md text-primary">' + formatPercent(role.soLuong, total) +
                ' <span class="font-caption text-caption text-text-muted">(' + formatNumber(role.soLuong) + ' TK)</span>' +
                '</span>' +
                '</div>' +
                '<div class="h-2 w-full rounded-full bg-surface-container overflow-hidden">' +
                '<div class="h-full rounded-full ' + style.bg + '" style="width: ' + percent.toFixed(2) + '%"></div>' +
                '</div>' +
                '</div>';
        });

        $('dashRoleDonut').innerHTML = arcs;
        $('dashRoleCount').textContent = roles.length;
        $('dashRoleList').innerHTML = rows;
    }

    function setLoading(value) {
        loading = value;

        // Khi bị từ chối quyền, layout đã thay toàn bộ <main> nên nút có thể không còn.
        var button = $('dashRefresh');
        if (!button) {
            return;
        }
        button.disabled = value;
        $('dashRefreshIcon').classList.toggle('animate-spin', value);
    }

    var LOGS_NOTE_CLASS = 'rounded-xl bg-surface-warm/60 p-space-md font-body-sm text-body-sm text-text-muted';

    var logsLoading = false;

    function formatDateTime(value) {
        if (!value) {
            return '—';
        }
        var date = new Date(value);
        return isNaN(date.getTime()) ? String(value) : date.toLocaleString('vi-VN');
    }

    // state: 'loading' | 'empty' | 'error'
    function renderLogsState(state, message) {
        // Khi bị từ chối quyền, layout đã thay toàn bộ <main> nên khối này có thể không còn.
        var box = $('dashLogs');
        if (!box) {
            return;
        }
        box.setAttribute('aria-busy', state === 'loading' ? 'true' : 'false');

        if (state === 'loading') {
            box.innerHTML = '<p class="' + LOGS_NOTE_CLASS + ' flex items-center gap-2" role="status">' +
                '<span class="material-symbols-outlined animate-spin" aria-hidden="true">progress_activity</span>' +
                'Đang tải nhật ký gần đây...</p>';
            return;
        }

        if (state === 'empty') {
            box.innerHTML = '<p class="' + LOGS_NOTE_CLASS + '">Chưa có hoạt động nào được ghi nhận.</p>';
            return;
        }

        box.innerHTML = '<div class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-error/30 bg-error-container p-space-md" role="alert">' +
            '<p class="font-body-sm text-body-sm text-on-error-container">Không tải được nhật ký: ' + OB.escapeHtml(message) + '</p>' +
            '<button type="button" data-logs-retry class="rounded-lg bg-surface-white px-4 py-2 font-label-lg text-label-lg text-primary shadow-sm hover:bg-surface-cream">Thử lại</button>' +
            '</div>';
    }

    function renderRecentLogs(logs) {
        var box = $('dashLogs');
        if (!box) {
            return;
        }
        if (!logs.length) {
            renderLogsState('empty');
            return;
        }

        box.setAttribute('aria-busy', 'false');
        box.innerHTML = logs.map(function (log) {
            var actor = log.tenNguoiThucHien || log.emailNguoiThucHien || ('Tài khoản #' + log.maTK);
            var role = OB.roleLabels[log.vaiTro] || log.vaiTro;
            var target = log.loaiDoiTuong
                ? log.loaiDoiTuong + (log.maDoiTuong == null ? '' : ' #' + log.maDoiTuong)
                : '';

            return '<div class="flex flex-col md:flex-row md:items-start gap-x-space-md gap-y-1 rounded-xl bg-surface-warm/40 p-space-md">' +
                '<div class="shrink-0 md:w-40 font-caption text-caption text-text-muted">' + OB.escapeHtml(formatDateTime(log.thoiGian)) + '</div>' +
                '<div class="min-w-0 flex-1 space-y-0.5">' +
                '<div class="flex flex-wrap items-center gap-2">' +
                '<span class="font-label-md text-label-md text-primary">' + OB.escapeHtml(log.hanhDong || 'HOẠT ĐỘNG') + '</span>' +
                (target ? '<span class="rounded-full bg-surface-container px-2 py-0.5 font-label-sm text-label-sm text-on-surface-variant">' + OB.escapeHtml(target) + '</span>' : '') +
                '<span class="font-caption text-caption text-text-muted">bởi <strong class="text-on-surface">' + OB.escapeHtml(actor) + '</strong>' +
                (role ? ' · ' + OB.escapeHtml(role) : '') + '</span>' +
                '</div>' +
                (log.moTa ? '<p class="font-body-sm text-body-sm text-on-surface-variant">' + OB.escapeHtml(log.moTa) + '</p>' : '') +
                '</div>' +
                '</div>';
        }).join('');
    }

    function loadRecentLogs() {
        if (logsLoading) {
            return;
        }

        logsLoading = true;
        renderLogsState('loading');

        OB.request(
            'GET',
            '/api/admin/audit-logs?page=0&size=10'
        )
            .then(function (page) {
                var logs =
                    page && Array.isArray(page.content)
                        ? page.content
                        : [];

                renderRecentLogs(logs);
            })
            .catch(function (err) {
                if (UI.handleApiError(err)) {
                    return;
                }

                renderLogsState(
                    'error',
                    (err && err.message) || 'Vui lòng thử lại sau.'
                );
            })
            .then(function () {
                logsLoading = false;
            });
    }

    function loadSummary() {
        if (loading) {
            return;
        }
        setLoading(true);

        OB.request('GET', '/api/admin/dashboard/summary')
            .then(function (summary) {
                if (pageGone()) {
                    return;
                }
                renderKpis(summary);
                renderRoles(summary);

                loadedAt = new Date();
                $('dashUpdatedAt').textContent = 'Cập nhật lúc ' +
                    loadedAt.toLocaleTimeString('vi-VN') + ' ngày ' + loadedAt.toLocaleDateString('vi-VN');
                $('dashError').classList.add('hidden');
            })
            .catch(function (err) {
                if (UI.handleApiError(err) || pageGone()) {
                    return;
                }

                // Giữ số liệu và mốc thời gian của lần tải thành công trước: lần lỗi không được trông như vừa làm mới.
                if (!loadedAt) {
                    $('dashUpdatedAt').textContent = 'Chưa tải được dữ liệu';
                }
                $('dashErrorText').textContent = (err && err.message) || 'Vui lòng thử lại sau.';
                $('dashError').classList.remove('hidden');
            })
            .then(function () {
                setLoading(false);
            });
    }

    function init() {
        if (!UI.guard()) {
            return;
        }

        $('dashRefresh').addEventListener('click', loadAll);
        $('dashRetry').addEventListener('click', loadSummary);

        // Nút "Thử lại" của nhật ký được vẽ lại mỗi lần lỗi, nên bắt sự kiện ở khối cha.
        $('dashLogs').addEventListener('click', function (event) {
            if (event.target.closest('[data-logs-retry]')) {
                loadRecentLogs();
            }
        });

        loadAll();
    }

    function loadAll() {
        loadSummary();
        loadRecentLogs();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
