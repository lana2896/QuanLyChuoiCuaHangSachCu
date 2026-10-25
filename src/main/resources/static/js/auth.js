(function () {
  'use strict';

  if (window.OldBook) { return; }

  var TOKEN_KEY = 'oldbook_token';
  var NAME_KEY = 'oldbook_name';
  var AVATAR_KEY = 'oldbook_avatar';
  var ACCOUNTS_KEY = 'oldbook_accounts';
  var PENDING_REGISTER_EMAIL_KEY = 'oldbook_pending_register_email';
  var PENDING_REGISTER_SENT_AT_KEY = 'oldbook_pending_register_sent_at';
  var ROLE_CUSTOMER = 'KHACH_HANG';
  var ROLE_SELLER = 'CHU_CUA_HANG';
  var ACCOUNT_ACTIVE = 'HOAT_DONG';

  var ROLE_LABELS = {
    KHACH_HANG: 'Khách hàng',
    CHU_CUA_HANG: 'Chủ cửa hàng',
    DON_VI_VAN_CHUYEN: 'Đơn vị vận chuyển',
    QUAN_LY: 'Quản lý',
    QUAN_TRI_VIEN: 'Quản trị viên'
  };

  var ROLE_HOME_URLS = {
    CHU_CUA_HANG: '/store/books',
    QUAN_LY: '/store-moderation',
    QUAN_TRI_VIEN: '/admin/dashboard'
  };

  function lsGet(key) {
    try { return window.localStorage.getItem(key); } catch (e) { return null; }
  }
  function lsSet(key, value) {
    try { window.localStorage.setItem(key, value); } catch (e) { }
  }
  function lsRemove(key) {
    try { window.localStorage.removeItem(key); } catch (e) { }
  }

  function decodePayload(token) {
    try {
      var part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      while (part.length % 4) { part += '='; }
      return JSON.parse(window.atob(part));
    } catch (e) {
      return null;
    }
  }

  function escapeHtml(value) {
    return String(value === null || value === undefined ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function formatMoney(value) {
    return Number(value || 0).toLocaleString('en-US') + ' đ';
  }

  function safeRedirect(target, fallback) {
    if (typeof target === 'string'
        && target.charAt(0) === '/'
        && target.charAt(1) !== '/'
        && target.charAt(1) !== '\\') {
      return target;
    }
    return fallback || '/';
  }

  // Tìm tài khoản theo vai trò trong danh sách tài khoản của người đang đăng nhập.
  function findAccount(accounts, role, activeOnly) {
    for (var i = 0; i < accounts.length; i++) {
      if (accounts[i].vaiTro === role && (!activeOnly || accounts[i].trangThai === ACCOUNT_ACTIVE)) {
        return accounts[i];
      }
    }
    return null;
  }

  function ApiError(status, message) {
    this.name = 'ApiError';
    this.status = status;
    this.message = message;
  }
  ApiError.prototype = Object.create(Error.prototype);
  ApiError.prototype.constructor = ApiError;

  function defaultMessage(status) {
    if (status === 401) { return 'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.'; }
    if (status === 403) { return 'Bạn không có quyền thực hiện thao tác này.'; }
    if (status >= 500) { return 'Máy chủ gặp lỗi. Vui lòng thử lại sau.'; }
    return 'Yêu cầu không hợp lệ.';
  }

  var Auth = {

    getToken: function () {
      var token = lsGet(TOKEN_KEY);
      if (!token) { return null; }

      var payload = decodePayload(token);
      if (!payload || !payload.exp || payload.exp * 1000 <= Date.now()) {
        Auth.clear();
        return null;
      }
      return token;
    },

    getUser: function () {
      var token = Auth.getToken();
      if (!token) { return null; }

      var payload = decodePayload(token) || {};
      return {
        maND: payload.maND,
        maTK: payload.maTK,
        vaiTro: payload.vaiTro,
        hoTen: lsGet(NAME_KEY),
        avatarUrl: lsGet(AVATAR_KEY)
      };
    },

    isLoggedIn: function () {
      return Auth.getToken() !== null;
    },

    hasRole: function (role) {
      var user = Auth.getUser();
      return !!user && user.vaiTro === role;
    },

    // Mọi tài khoản (mỗi vai trò một tài khoản) của người đang đăng nhập, lấy từ /api/profile.
    // Trả null khi chưa tải xong, để phân biệt với "đã tải và chỉ có một tài khoản".
    getAccounts: function () {
      if (!Auth.getToken()) { return null; }
      try {
        var accounts = JSON.parse(lsGet(ACCOUNTS_KEY));
        return Array.isArray(accounts) ? accounts : null;
      } catch (e) {
        return null;
      }
    },

    saveSession: function (loginData) {
      lsSet(TOKEN_KEY, loginData.token);
      lsRemove(NAME_KEY);
      lsRemove(AVATAR_KEY);
      lsRemove(ACCOUNTS_KEY);
      renderHeader();
    },

    clear: function () {
      lsRemove(TOKEN_KEY);
      lsRemove(NAME_KEY);
      lsRemove(AVATAR_KEY);
      lsRemove(ACCOUNTS_KEY);
    },

    safeRedirect: safeRedirect,

    homeUrl: function () {
      var user = Auth.getUser();
      return (user && ROLE_HOME_URLS[user.vaiTro]) || '/';
    },

    navigate: function (url) {
      window.location.assign(url);
    },

    loginUrl: function (redirect) {
      var back = redirect || (window.location.pathname + window.location.search);
      return '/login?redirect=' + encodeURIComponent(safeRedirect(back, '/'));
    },

    redirectToLogin: function (redirect) {
      Auth.navigate(Auth.loginUrl(redirect));
    },

    handleAuthError: function (err, requiredRole) {
      if (!(err instanceof ApiError)) { return false; }

      var user = Auth.getUser();
      var wrongRole = !!user && !!requiredRole && user.vaiTro !== requiredRole;
      var sessionInvalid = err.status === 401;

      if (!sessionInvalid) { return false; }

      Auth.clear();
      Auth.redirectToLogin();
      return true;
    },

    register: function (hoTen, email, matKhau, soDienThoai) {
      return request('POST', '/api/auth/register', {
        hoTen: hoTen,
        email: email,
        matKhau: matKhau,
        soDienThoai: soDienThoai || null
      }, { token: null });
    },

    setPendingRegister: function (email, sentAt) {
      lsSet(PENDING_REGISTER_EMAIL_KEY, email);
      lsSet(PENDING_REGISTER_SENT_AT_KEY, String(sentAt || Date.now()));
    },

    getPendingRegisterEmail: function () {
      return lsGet(PENDING_REGISTER_EMAIL_KEY);
    },

    getPendingRegisterSentAt: function () {
      var value = Number(lsGet(PENDING_REGISTER_SENT_AT_KEY) || 0);
      return Number.isFinite(value) ? value : 0;
    },

    clearPendingRegister: function () {
      lsRemove(PENDING_REGISTER_EMAIL_KEY);
      lsRemove(PENDING_REGISTER_SENT_AT_KEY);
    },

    verifyRegisterOtp: function (email, maOtp) {
      return request('POST', '/api/auth/verify-register-otp', {
        email: email,
        maOtp: maOtp
      }, { token: null });
    },

    resendRegisterOtp: function (email) {
      return request('POST', '/api/auth/resend-register-otp', {
        email: email
      }, { token: null });
    },

    login: function (email, matKhau) {
      return request('POST', '/api/auth/login', { email: email, matKhau: matKhau }, { token: null });
    },

    forgotPassword: function (email) {
        return request(
            'POST',
            '/api/auth/forgot-password',
            {
                email: email
            },
            { token: null }
        );
    },

    resendForgotPasswordOtp: function (email) {
        return request(
            'POST',
            '/api/auth/resend-forgot-password-otp',
            {
                email: email
            },
            { token: null }
        );
    },

    resetPassword: function (
        email,
        maOtp,
        matKhauMoi,
        xacNhanMatKhauMoi
    ) {
        return request(
            'POST',
            '/api/auth/reset-password',
            {
                email: email,
                maOtp: maOtp,
                matKhauMoi: matKhauMoi,
                xacNhanMatKhauMoi: xacNhanMatKhauMoi
            },
            { token: null }
        );
    },

    changePassword: function (
        matKhauHienTai,
        matKhauMoi,
        xacNhanMatKhauMoi
    ) {
        return request(
            'POST',
            '/api/auth/change-password',
            {
                matKhauHienTai: matKhauHienTai,
                matKhauMoi: matKhauMoi,
                xacNhanMatKhauMoi: xacNhanMatKhauMoi
            }
        );
    },

    selectRole: function (roleSelectionToken, maTK) {
      return request('POST', '/api/auth/select-role', { maTK: maTK }, { token: roleSelectionToken });
    },

    // Chuyển sang một tài khoản khác của chính mình (ví dụ Khách hàng sang Chủ cửa hàng) mà không đăng nhập lại.
    switchAccount: function (maTK) {
      return request('POST', '/api/auth/switch-account', { maTK: maTK }).then(function (loginData) {
        Auth.saveSession(loginData);
        return Auth.loadProfileName();
      }).then(function () {
        Auth.navigate(Auth.homeUrl());
      });
    },

    loadProfileName: function () {
      return request('GET', '/api/profile').then(function (profile) {
        if (profile && profile.hoTen) {
          lsSet(NAME_KEY, profile.hoTen);
        }
        if (profile && profile.avatarUrl) {
          lsSet(AVATAR_KEY, profile.avatarUrl);
        } else {
          lsRemove(AVATAR_KEY);
        }
        if (profile && Array.isArray(profile.taiKhoans)) {
          lsSet(ACCOUNTS_KEY, JSON.stringify(profile.taiKhoans));
        } else {
          lsRemove(ACCOUNTS_KEY);
        }
        renderHeader();
      }, function () { });
    },

    logout: function (redirect) {
      var finish = function () {
        Auth.clear();
        Auth.navigate(safeRedirect(redirect, '/'));
      };

      if (!Auth.getToken()) { finish(); return; }

      request('POST', '/api/auth/logout').then(finish, finish);
    }
  };

  function request(method, url, body, options) {
    options = options || {};

    var headers = { 'Accept': 'application/json' };
    var token = options.token !== undefined ? options.token : Auth.getToken();
    if (token) { headers['Authorization'] = 'Bearer ' + token; }

    var init = { method: method, headers: headers };
    if (body !== undefined) {
      if (typeof window.FormData === 'function' && body instanceof window.FormData) {
        init.body = body;
      } else {
        headers['Content-Type'] = 'application/json';
        init.body = JSON.stringify(body);
      }
    }

    return window.fetch(url, init)
      .catch(function () {
        throw new ApiError(0, 'Không kết nối được máy chủ. Vui lòng thử lại.');
      })
      .then(function (res) {
        return res.text().then(function (text) {
          var json = null;
          if (text) {
            try { json = JSON.parse(text); } catch (e) { json = null; }
          }

          var codeOk = !json || json.code === undefined || json.code === 200 || json.code === 201;

          if (res.ok && codeOk) {
            return json ? json.data : null;
          }

          throw new ApiError(res.status, (json && json.message) || defaultMessage(res.status));
        });
      });
  }

  function toast(message, type, options) {
    options = options || {};

    var holder = document.getElementById('ob-toast-holder');
    if (!holder) {
      holder = document.createElement('div');
      holder.id = 'ob-toast-holder';
      holder.className = 'position-fixed top-0 end-0 p-3';
      holder.style.zIndex = '1100';
      holder.style.maxWidth = '380px';
      document.body.appendChild(holder);
    }

    var box = document.createElement('div');
    box.className = 'alert alert-' + (type || 'info') + ' shadow-sm mb-2';
    box.setAttribute('role', 'alert');

    var text = document.createElement('span');
    text.textContent = message;
    box.appendChild(text);

    if (options.link) {
      var link = document.createElement('a');
      link.href = options.link.href;
      link.className = 'ms-2 fw-semibold';
      link.textContent = options.link.text;
      box.appendChild(link);
    }

    holder.appendChild(box);

    window.setTimeout(function () {
      if (box.parentNode) { box.parentNode.removeChild(box); }
    }, options.duration || 3500);
  }

  /* Hộp xác nhận hiển thị trên màn hình (thay cho window.confirm của trình duyệt). Trả về Promise<boolean>. */
  function confirmBox(message, options) {
    options = options || {};
    return new Promise(function (resolve) {
      var previous = document.activeElement;

      var overlay = document.createElement('div');
      overlay.className = 'ob-confirm-overlay';
      overlay.style.cssText = 'position:fixed;top:0;right:0;bottom:0;left:0;z-index:1200;display:flex;' +
        'align-items:center;justify-content:center;padding:1rem;background:rgba(40,35,20,.45)';

      var dialog = document.createElement('div');
      dialog.className = 'ob-confirm-dialog';
      dialog.setAttribute('role', 'alertdialog');
      dialog.setAttribute('aria-modal', 'true');
      dialog.setAttribute('aria-labelledby', 'ob-confirm-title');
      dialog.setAttribute('aria-describedby', 'ob-confirm-message');
      dialog.style.cssText = 'background:#fff;border-radius:14px;width:100%;max-width:440px;padding:1.5rem;' +
        'box-shadow:0 12px 40px rgba(0,0,0,.25)';

      var title = document.createElement('h2');
      title.id = 'ob-confirm-title';
      title.className = 'fs-5 fw-bold mb-2';
      title.textContent = options.title || 'Xác nhận';

      var body = document.createElement('p');
      body.id = 'ob-confirm-message';
      body.className = 'mb-4';
      body.style.whiteSpace = 'pre-line';
      body.textContent = message;

      var actions = document.createElement('div');
      actions.className = 'd-flex justify-content-end gap-2 flex-wrap';

      var cancelBtn = document.createElement('button');
      cancelBtn.type = 'button';
      cancelBtn.className = 'btn btn-outline-secondary';
      cancelBtn.textContent = options.cancelText || 'Hủy';

      var okBtn = document.createElement('button');
      okBtn.type = 'button';
      okBtn.className = 'btn btn-accent';
      okBtn.textContent = options.confirmText || 'Đồng ý';

      actions.appendChild(cancelBtn);
      actions.appendChild(okBtn);
      dialog.appendChild(title);
      dialog.appendChild(body);
      dialog.appendChild(actions);
      overlay.appendChild(dialog);
      document.body.appendChild(overlay);

      function close(result) {
        document.removeEventListener('keydown', onKey, true);
        if (overlay.parentNode) { overlay.parentNode.removeChild(overlay); }
        if (previous && typeof previous.focus === 'function') { previous.focus(); }
        resolve(result);
      }
      function onKey(e) {
        if (e.key === 'Escape') { e.preventDefault(); close(false); return; }
        if (e.key === 'Tab') {
          var first = cancelBtn, last = okBtn;
          if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
          else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
        }
      }

      cancelBtn.addEventListener('click', function () { close(false); });
      okBtn.addEventListener('click', function () { close(true); });
      overlay.addEventListener('mousedown', function (e) { if (e.target === overlay) { close(false); } });
      document.addEventListener('keydown', onKey, true);
      cancelBtn.focus();
    });
  }

  function renderHeader() {
    var loggedIn = Auth.isLoggedIn();
    var user = Auth.getUser();

    var links = document.querySelectorAll('[data-auth], [data-roles]');
    for (var i = 0; i < links.length; i++) {
      var authState = links[i].getAttribute('data-auth');
      var visible = authState === 'guest' ? !loggedIn : authState !== 'user' || loggedIn;
      var allowedRoles = links[i].getAttribute('data-roles');
      if (allowedRoles !== null) {
        var roleAllowed = user ? allowedRoles.split(',').some(function (role) {
          return role.trim() === user.vaiTro;
        }) : links[i].hasAttribute('data-allow-guest');
        visible = visible && roleAllowed;
      }
      links[i].classList.toggle('d-none', !visible);
      links[i].hidden = !visible;
    }

    // Phần tử chỉ dành cho một vai trò nhất định (ví dụ liên kết quản lý đơn của chủ cửa hàng)
    var roleOnly = document.querySelectorAll('[data-role]');
    for (var r = 0; r < roleOnly.length; r++) {
      var allowed = loggedIn && !!user && user.vaiTro === roleOnly[r].getAttribute('data-role');
      roleOnly[r].classList.toggle('d-none', !allowed);
    }

    // "Chuyển sang <vai trò>": hiện cho mỗi tài khoản khác đang hoạt động của cùng người dùng.
    var loadedAccounts = user ? Auth.getAccounts() : null;
    var accounts = loadedAccounts || [];
    var switches = document.querySelectorAll('[data-switch-to]');
    for (var s = 0; s < switches.length; s++) {
      var target = findAccount(accounts, switches[s].getAttribute('data-switch-to'), true);
      var canSwitch = !!target && target.maTK !== user.maTK;
      switches[s].classList.toggle('d-none', !canSwitch);
      switches[s].hidden = !canSwitch;
    }

    // Lời mời đăng ký gian hàng: khách vãng lai, hoặc khách hàng chưa có tài khoản chủ cửa hàng nào.
    // Khi chưa tải xong danh sách tài khoản thì chưa mời, tránh mời nhầm người đã có gian hàng.
    var canRegisterStore = !user
      || (user.vaiTro === ROLE_CUSTOMER && loadedAccounts !== null && !findAccount(accounts, ROLE_SELLER, false));
    var sellerCtas = document.querySelectorAll('[data-seller-cta]');
    for (var c = 0; c < sellerCtas.length; c++) {
      sellerCtas[c].classList.toggle('d-none', !canRegisterStore);
      sellerCtas[c].hidden = !canRegisterStore;
    }

    var label = (user && (user.hoTen || ROLE_LABELS[user.vaiTro])) || 'Tài khoản';
    var names = document.querySelectorAll('[data-user-name]');
    for (var k = 0; k < names.length; k++) {
      names[k].textContent = label;
    }

    var roleLabels = document.querySelectorAll('[data-user-role]');
    for (var r = 0; r < roleLabels.length; r++) {
      roleLabels[r].textContent = (user && ROLE_LABELS[user.vaiTro]) || '';
    }

    var avatarImages = document.querySelectorAll('[data-user-avatar]');
    var avatarPlaceholders =
      document.querySelectorAll('[data-user-avatar-placeholder]');

    var hasAvatar = !!(user && user.avatarUrl);

    for (var x = 0; x < avatarImages.length; x++) {
      if (hasAvatar) {
        avatarImages[x].src = user.avatarUrl;
        avatarImages[x].classList.remove('d-none');
      } else {
        avatarImages[x].removeAttribute('src');
        avatarImages[x].classList.add('d-none');
      }
    }

    for (var y = 0; y < avatarPlaceholders.length; y++) {
      avatarPlaceholders[y].classList.toggle('d-none', hasAvatar);
    }
  }

  document.addEventListener('click', function (e) {
    var guarded = e.target.closest ? e.target.closest('[data-requires-login]') : null;

    if (guarded && !Auth.isLoggedIn()) {
      e.preventDefault();
      e.stopImmediatePropagation();

      var href = guarded.getAttribute('href');
      var redirect = (href && href.charAt(0) === '/') ? href : null;
      Auth.redirectToLogin(redirect);
      return;
    }

    var logout = e.target.closest ? e.target.closest('[data-action="logout"]') : null;
    if (logout) {
      e.preventDefault();
      Auth.logout();
    }

    var switchTo = e.target.closest ? e.target.closest('[data-switch-to]') : null;
    if (switchTo) {
      e.preventDefault();
      var account = findAccount(Auth.getAccounts() || [], switchTo.getAttribute('data-switch-to'), true);
      if (account) {
        Auth.switchAccount(account.maTK).catch(function (err) {
          if (Auth.handleAuthError(err)) { return; }
          toast(err.message, 'danger', { duration: 5000 });
        });
      }
    }
  }, true);

  function init() {
    if (document.body && document.body.hasAttribute('data-page-requires-login') && !Auth.isLoggedIn()) {
      Auth.redirectToLogin();
      return;
    }
    renderHeader();

    if (Auth.isLoggedIn()) {
      Auth.loadProfileName();
    }
  }

  window.OldBook = {
    auth: Auth,
    request: request,
    ApiError: ApiError,
    toast: toast,
    confirm: confirmBox,
    renderHeader: renderHeader,
    escapeHtml: escapeHtml,
    formatMoney: formatMoney,
    roleLabels: ROLE_LABELS,
    ROLE_CUSTOMER: ROLE_CUSTOMER,
    ROLE_SELLER: ROLE_SELLER
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
