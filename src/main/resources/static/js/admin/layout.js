/*
 * Hành vi dùng chung của layout admin: chặn truy cập, danh tính trên sidebar/header,
 * thông báo (#adminNotification) và hộp thoại (#adminModal).
 * Nạp sau auth.js và trước script riêng của từng trang.
 */
(function () {
    'use strict';

    var OB = window.OldBook;
    if (!OB || window.AdminUI) {
        return;
    }

    var Auth = OB.auth;
    var ADMIN_ROLE = 'QUAN_TRI_VIEN';

    var $ = function (id) { return document.getElementById(id); };

    // =========================================================
    // THÔNG BÁO
    // =========================================================

    var NOTICE_STYLES = {
        info: { icon: 'info', color: 'text-primary' },
        success: { icon: 'check_circle', color: 'text-secondary' },
        error: { icon: 'error', color: 'text-error' }
    };

    var noticeTimer = null;

    function hideNotice() {
        var box = $('adminNotification');
        if (box) {
            box.classList.add('hidden');
        }
        window.clearTimeout(noticeTimer);
    }

    function notify(message, type, duration) {
        var box = $('adminNotification');
        if (!box) {
            return;
        }

        var style = NOTICE_STYLES[type] || NOTICE_STYLES.info;
        var icon = $('adminNotificationIcon');
        icon.textContent = style.icon;
        icon.className = 'material-symbols-outlined ' + style.color;
        $('adminNotificationMessage').textContent = message;
        box.classList.remove('hidden');

        window.clearTimeout(noticeTimer);
        noticeTimer = window.setTimeout(hideNotice, duration || (type === 'error' ? 7000 : 4000));
    }

    // =========================================================
    // HỘP THOẠI
    // =========================================================

    var BUTTON_STYLES = {
        primary: 'bg-primary text-on-primary hover:bg-primary-container',
        danger: 'bg-mahogany-danger text-on-error hover:opacity-90',
        neutral: 'bg-surface-cream text-primary hover:bg-surface-container'
    };

    var modalResolve = null;
    var modalOpener = null;

    function closeModal(result) {
        var modal = $('adminModal');
        if (!modal || modal.classList.contains('hidden')) {
            return;
        }

        modal.classList.add('hidden');
        modal.classList.remove('flex');

        if (modalOpener && modalOpener.focus) {
            modalOpener.focus();
        }

        var resolve = modalResolve;
        modalResolve = null;
        modalOpener = null;
        if (resolve) {
            resolve(result === undefined ? null : result);
        }
    }

    /*
     * options.title   : tiêu đề
     * options.body    : chuỗi (luôn hiển thị dạng văn bản thuần) hoặc một phần tử DOM
     * options.actions : [{ key, label, style: 'primary' | 'danger' | 'neutral' }]
     *
     * Trả về Promise nhận key của nút được bấm, hoặc null nếu hộp thoại bị đóng.
     */
    function openModal(options) {
        var modal = $('adminModal');
        if (!modal) {
            return Promise.resolve(null);
        }

        closeModal(null);
        options = options || {};

        $('adminModalTitle').textContent = options.title || '';

        var body = $('adminModalBody');
        body.textContent = '';
        if (options.body && options.body.nodeType) {
            body.appendChild(options.body);
        } else {
            body.textContent = options.body || '';
        }

        var actions = options.actions || [{ key: 'close', label: 'Đóng', style: 'neutral' }];
        var footer = $('adminModalFooter');
        footer.textContent = '';
        actions.forEach(function (action) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'rounded-lg px-4 py-2.5 font-label-lg text-label-lg transition-colors ' +
                (BUTTON_STYLES[action.style] || BUTTON_STYLES.neutral);
            button.textContent = action.label;
            button.addEventListener('click', function () { closeModal(action.key); });
            footer.appendChild(button);
        });

        modalOpener = document.activeElement;
        modal.classList.remove('hidden');
        modal.classList.add('flex');

        // Nút đầu tiên là nút an toàn (Hủy/Đóng), nên Enter không vô tình xác nhận thao tác nguy hiểm.
        if (footer.firstChild) {
            footer.firstChild.focus();
        }

        return new Promise(function (resolve) { modalResolve = resolve; });
    }

    // Trả về Promise<boolean>: true khi người dùng bấm xác nhận.
    function confirm(options) {
        options = options || {};

        return openModal({
            title: options.title || 'Xác nhận',
            body: options.message,
            actions: [
                { key: 'cancel', label: options.cancelLabel || 'Hủy', style: 'neutral' },
                { key: 'ok', label: options.confirmLabel || 'Xác nhận', style: options.danger ? 'danger' : 'primary' }
            ]
        }).then(function (key) { return key === 'ok'; });
    }

    // =========================================================
    // QUYỀN TRUY CẬP
    // =========================================================

    function showDenied() {
        var main = document.querySelector('main');
        if (!main) {
            return;
        }

        main.innerHTML =
            '<div class="px-6 py-10">' +
            '<div class="mx-auto max-w-xl rounded-2xl border border-line-border bg-surface-white p-8 text-center shadow-sm">' +
            '<span class="material-symbols-outlined text-5xl text-text-muted">admin_panel_settings</span>' +
            '<h1 class="mt-3 font-headline-md text-headline-md text-primary">Bạn không có quyền truy cập khu vực quản trị</h1>' +
            '<p class="mt-2 font-body-md text-body-md text-text-muted">Chức năng này chỉ dành cho Quản trị viên.</p>' +
            '<a href="/" class="mt-5 inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 font-label-lg text-label-lg text-on-primary">Về trang chủ</a>' +
            '</div>' +
            '</div>';
    }

    /*
     * Chỉ là lớp giao diện: trang HTML admin được phục vụ công khai vì JWT nằm trong localStorage.
     * Dữ liệu được bảo vệ thật sự ở /api/admin/** (SecurityConfig).
     */
    function guard() {
        if (!Auth.isLoggedIn()) {
            Auth.redirectToLogin();
            return false;
        }
        if (!Auth.hasRole(ADMIN_ROLE)) {
            showDenied();
            return false;
        }
        return true;
    }

    // true nếu lỗi đã được xử lý xong: 401 → về trang đăng nhập, 403 → màn hình từ chối.
    function handleApiError(err) {
        if (Auth.handleAuthError(err, ADMIN_ROLE)) {
            return true;
        }
        if (err && err.status === 403) {
            showDenied();
            return true;
        }
        return false;
    }

    // =========================================================
    // DANH TÍNH ADMIN
    // =========================================================

    function renderIdentity(profile) {
        if (!profile) {
            return;
        }

        var email = $('adminSidebarEmail');
        if (email) {
            email.textContent = profile.email || '';
        }

        if (!profile.avatarUrl) {
            return;
        }

        var holders = document.querySelectorAll('[data-admin-avatar]');
        for (var i = 0; i < holders.length; i++) {
            var image = document.createElement('img');
            image.src = profile.avatarUrl;
            image.alt = 'Ảnh đại diện';
            image.className = 'h-full w-full object-cover';
            holders[i].textContent = '';
            holders[i].appendChild(image);
        }
    }

    // =========================================================
    // TRUNG TÂM THÔNG BÁO (chuông trên header)
    // Khác với notify() ở trên: notify() là lời nhắn tạm thời về thao tác vừa làm,
    // còn đây là các thông báo được lưu trong database, có trạng thái đã đọc.
    // =========================================================

    var NOTIFY_API = '/api/notifications';
    var NOTIFY_PAGE_SIZE = 10;

    // Mức độ luôn đi kèm biểu tượng và nhãn chữ, không chỉ phân biệt bằng màu.
    var NOTIFY_LEVELS = {
        NGHIEM_TRONG: { label: 'Nghiêm trọng', icon: 'gpp_maybe', color: 'text-error', badge: 'bg-error-container text-on-error-container' },
        QUAN_TRONG: { label: 'Quan trọng', icon: 'priority_high', color: 'text-tertiary', badge: 'bg-tertiary-fixed text-on-tertiary-fixed' },
        THONG_TIN: { label: 'Thông tin', icon: 'info', color: 'text-olive-grove', badge: 'bg-surface-container text-on-surface-variant' }
    };

    var notifyUnread = 0;
    var notifyNextPage = 0;
    var notifyLoading = false;

    function setUnread(count) {
        notifyUnread = Math.max(0, Number(count) || 0);

        var badge = $('adminNotifyBadge');
        badge.textContent = notifyUnread > 99 ? '99+' : String(notifyUnread);
        badge.classList.toggle('hidden', notifyUnread === 0);

        $('btnNotifyReadAll').disabled = notifyUnread === 0;
        $('btnNotifications').setAttribute(
            'title',
            notifyUnread ? 'Thông báo (' + notifyUnread + ' chưa đọc)' : 'Thông báo'
        );
    }

    function loadUnread() {
        OB.request('GET', NOTIFY_API + '/unread')
            .then(function (data) { setUnread(data && data.soLuong); }, function () { });
    }

    function notifyNote(message) {
        var note = document.createElement('p');
        note.className = 'px-4 py-6 text-center font-body-sm text-body-sm text-text-muted';
        note.textContent = message;
        return note;
    }

    // Tiêu đề và nội dung chứa tên người dùng, lý do khóa...: luôn gán bằng textContent.
    function notifyItem(item) {
        var level = NOTIFY_LEVELS[item.mucDo] || NOTIFY_LEVELS.THONG_TIN;

        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'flex w-full gap-3 border-b border-line-border/60 px-4 py-3 text-left hover:bg-surface-cream/60' +
            (item.daDoc ? '' : ' bg-surface-warm');
        button.setAttribute('data-notify-id', item.maThongBao);

        var icon = document.createElement('span');
        icon.className = 'material-symbols-outlined mt-0.5 ' + level.color;
        icon.setAttribute('aria-hidden', 'true');
        icon.textContent = level.icon;

        var body = document.createElement('span');
        body.className = 'block min-w-0 flex-1';

        var title = document.createElement('span');
        title.className = 'block font-label-lg text-label-lg text-primary';
        title.textContent = item.tieuDe;

        var content = document.createElement('span');
        content.className = 'mt-0.5 block font-body-sm text-body-sm text-on-surface-variant';
        content.textContent = item.noiDung || '';

        var meta = document.createElement('span');
        meta.className = 'mt-1.5 flex flex-wrap items-center gap-2 font-caption text-caption text-text-muted';

        var badge = document.createElement('span');
        badge.className = 'rounded-full px-2 py-0.5 font-label-md text-label-md ' + level.badge;
        badge.textContent = level.label;

        var time = document.createElement('span');
        var date = new Date(item.ngayTao);
        time.textContent = isNaN(date.getTime()) ? '' : date.toLocaleString('vi-VN');

        meta.appendChild(badge);
        meta.appendChild(time);
        if (!item.daDoc) {
            var unread = document.createElement('span');
            unread.className = 'font-label-md text-label-md text-secondary';
            unread.setAttribute('data-notify-unread', '');
            unread.textContent = 'Chưa đọc';
            meta.appendChild(unread);
        }

        body.appendChild(title);
        body.appendChild(content);
        body.appendChild(meta);
        button.appendChild(icon);
        button.appendChild(body);

        button.addEventListener('click', function () { openNotifyItem(item, button); });
        return button;
    }

    // reset = true: tải lại từ trang đầu (mỗi lần mở chuông); false: nối thêm trang kế tiếp.
    function loadNotifications(reset) {
        if (notifyLoading) {
            return;
        }
        notifyLoading = true;

        var list = $('adminNotifyList');
        var page = reset ? 0 : notifyNextPage;
        if (reset) {
            list.textContent = '';
            list.appendChild(notifyNote('Đang tải thông báo...'));
        }

        OB.request('GET', NOTIFY_API + '?page=' + page + '&size=' + NOTIFY_PAGE_SIZE)
            .then(function (data) {
                var items = (data && data.content) || [];
                if (reset) {
                    list.textContent = '';
                }
                if (reset && !items.length) {
                    list.appendChild(notifyNote('Chưa có thông báo nào.'));
                }
                items.forEach(function (item) { list.appendChild(notifyItem(item)); });

                notifyNextPage = page + 1;
                $('btnNotifyMore').classList.toggle('hidden', !data || notifyNextPage >= data.totalPages);
            })
            .catch(function (err) {
                if (handleApiError(err)) {
                    return;
                }
                if (reset) {
                    list.textContent = '';
                    list.appendChild(notifyNote('Không tải được thông báo. Hãy mở lại chuông để thử lại.'));
                } else {
                    notify((err && err.message) || 'Không tải thêm được thông báo.', 'error');
                }
            })
            .then(function () { notifyLoading = false; });
    }

    function markItemRead(item, button) {
        item.daDoc = true;
        button.classList.remove('bg-surface-warm');
        var unread = button.querySelector('[data-notify-unread]');
        if (unread) {
            unread.parentNode.removeChild(unread);
        }
        setUnread(notifyUnread - 1);
    }

    // Chỉ nhận đường dẫn trong chính trang web này ("/admin/..."), không nhận địa chỉ ngoài như "//evil.example".
    function internalPath(path) {
        var internal = typeof path === 'string'
            && path.charAt(0) === '/'
            && path.charAt(1) !== '/'
            && path.charAt(1) !== '\\';
        return internal ? path : '';
    }

    // Bấm vào một thông báo: đánh dấu đã đọc rồi mở trang cần xử lý.
    function openNotifyItem(item, button) {
        var target = internalPath(item.duongDan);
        var go = function () {
            if (target) {
                Auth.navigate(target);
            }
        };

        if (item.daDoc) {
            go();
            return;
        }

        OB.request('PUT', NOTIFY_API + '/' + encodeURIComponent(item.maThongBao) + '/read')
            .then(function () { markItemRead(item, button); }, function () { })
            .then(go);
    }

    function markAllNotificationsRead() {
        OB.request('PUT', NOTIFY_API + '/read-all')
            .then(function () {
                setUnread(0);
                loadNotifications(true);
            })
            .catch(function (err) {
                if (!handleApiError(err)) {
                    notify((err && err.message) || 'Không đánh dấu được thông báo.', 'error');
                }
            });
    }

    function setNotifyOpen(open) {
        $('adminNotifyPanel').classList.toggle('hidden', !open);
        $('btnNotifications').setAttribute('aria-expanded', String(open));
        if (open) {
            loadNotifications(true);
        }
    }

    function initNotifications() {
        var bell = $('btnNotifications');
        if (!bell) {
            return;
        }

        bell.addEventListener('click', function () {
            setNotifyOpen($('adminNotifyPanel').classList.contains('hidden'));
        });
        $('btnNotifyReadAll').addEventListener('click', markAllNotificationsRead);
        $('btnNotifyMore').addEventListener('click', function () { loadNotifications(false); });

        // Bấm ra ngoài hoặc nhấn Esc thì đóng bảng thông báo.
        document.addEventListener('click', function (event) {
            if (!event.target.closest || !event.target.closest('#adminNotify')) {
                setNotifyOpen(false);
            }
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                setNotifyOpen(false);
            }
        });

        loadUnread();
    }

    // =========================================================
    // TẢI FILE
    // =========================================================

    // Tải một file văn bản do trình duyệt tự tạo (ví dụ CSV). BOM ở đầu giúp Excel đọc đúng tiếng Việt.
    function downloadFile(filename, text, mimeType) {
        var blob = new Blob(['﻿' + text], { type: mimeType || 'text/plain;charset=utf-8;' });
        var url = URL.createObjectURL(blob);

        var link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        URL.revokeObjectURL(url);
    }

    function init() {
        var closeNotice = $('btnCloseNotification');
        if (closeNotice) {
            closeNotice.addEventListener('click', hideNotice);
        }

        var modal = $('adminModal');
        if (modal) {
            $('btnCloseAdminModal').addEventListener('click', function () { closeModal(null); });
            modal.addEventListener('click', function (event) {
                if (event.target === modal) {
                    closeModal(null);
                }
            });
            document.addEventListener('keydown', function (event) {
                if (event.key === 'Escape') {
                    closeModal(null);
                }
            });
        }

        if (Auth.isLoggedIn() && Auth.hasRole(ADMIN_ROLE)) {
            // auth.js đã tự điền tên vào [data-user-name]; email và ảnh đại diện thì nó không giữ lại.
            OB.request('GET', '/api/profile').then(renderIdentity, function () { });
            initNotifications();
        }
    }

    window.AdminUI = {
        ROLE: ADMIN_ROLE,
        notify: notify,
        openModal: openModal,
        closeModal: closeModal,
        confirm: confirm,
        guard: guard,
        handleApiError: handleApiError,
        downloadFile: downloadFile
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
