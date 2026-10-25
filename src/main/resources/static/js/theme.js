/*
 * Giao diện của trang: Mặc định, Tối, Tạp chí.
 *
 * File này được nạp ở đầu header, trước nội dung trang, để đặt thuộc tính data-theme lên thẻ <html>
 * trước khi trình duyệt vẽ; nhờ vậy trang không nháy màu sáng rồi mới sang tối.
 * Màu và hình khối của từng theme nằm ở cuối css/style.css; ở đây chỉ chọn và nhớ theme.
 */
(function () {
  'use strict';

  // Không tạo window.OldBook ở đây: auth.js coi biến đó là dấu hiệu "đã chạy rồi" và sẽ bỏ qua.
  if (window.OldBookTheme) { return; }

  var STORAGE_KEY = 'oldbook_theme';
  var DEFAULT_THEME = 'default';
  var DARK_THEME = 'dark';
  var THEMES = [DEFAULT_THEME, DARK_THEME, 'magazine'];

  var root = document.documentElement;
  var current = DEFAULT_THEME;

  function valid(theme) {
    return THEMES.indexOf(theme) === -1 ? DEFAULT_THEME : theme;
  }

  function read() {
    try { return valid(window.localStorage.getItem(STORAGE_KEY)); } catch (e) { return DEFAULT_THEME; }
  }

  function save(theme) {
    try {
      if (theme === DEFAULT_THEME) {
        window.localStorage.removeItem(STORAGE_KEY);
      } else {
        window.localStorage.setItem(STORAGE_KEY, theme);
      }
    } catch (e) { }
  }

  // Đánh dấu dòng đang chọn trong menu "Đổi giao diện" (có thể có nhiều menu trên một trang).
  function markOptions() {
    var options = document.querySelectorAll('[data-theme-option]');
    for (var i = 0; i < options.length; i++) {
      var active = options[i].getAttribute('data-theme-option') === current;
      options[i].classList.toggle('is-current', active);
      options[i].setAttribute('aria-checked', active ? 'true' : 'false');
    }
  }

  function apply(theme) {
    current = valid(theme);

    if (current === DEFAULT_THEME) {
      root.removeAttribute('data-theme');
    } else {
      root.setAttribute('data-theme', current);
    }

    // Bootstrap 5.3 có sẵn chế độ tối cho ô nhập, dropdown, bảng, modal.
    if (current === DARK_THEME) {
      root.setAttribute('data-bs-theme', 'dark');
    } else {
      root.removeAttribute('data-bs-theme');
    }

    markOptions();
  }

  function set(theme) {
    apply(theme);
    save(current);
  }

  apply(read());

  document.addEventListener('click', function (event) {
    var option = event.target.closest ? event.target.closest('[data-theme-option]') : null;
    if (!option) { return; }
    event.preventDefault();
    set(option.getAttribute('data-theme-option'));
  });

  // Lúc file này chạy thì menu chưa có trong trang; đánh dấu lại khi trang tải xong.
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', markOptions);
  }

  // Đổi theme ở một tab thì các tab khác đang mở đổi theo.
  window.addEventListener('storage', function (event) {
    if (event.key === STORAGE_KEY) { apply(read()); }
  });

  window.OldBookTheme = {
    themes: THEMES.slice(),
    get: function () { return current; },
    set: set
  };
})();
