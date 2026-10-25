(function () {
  'use strict';

  function init() {
    document.querySelectorAll('[data-book-gallery]').forEach(function (root) {
      var main = root.querySelector('[data-gallery-main]');
      var thumbs = Array.prototype.slice.call(root.querySelectorAll('[data-gallery-thumb]')).filter(function (thumb) {
        return /^(https?:\/\/|\/(?!\/))/i.test(thumb.getAttribute('data-gallery-src') || '');
      });
      var controls = root.querySelectorAll('[data-gallery-prev], [data-gallery-next]');
      var current = 0;
      if (!main || !thumbs.length) { return; }

      function show(index, scroll) {
        current = (index + thumbs.length) % thumbs.length;
        main.setAttribute('src', thumbs[current].getAttribute('data-gallery-src'));
        thumbs.forEach(function (thumb, position) {
          var active = position === current;
          if (active) {
            thumb.style.setProperty('border-width', '4px', 'important');
            thumb.style.setProperty('border-color', 'var(--brand)', 'important');
          } else {
            thumb.style.removeProperty('border-width');
            thumb.style.removeProperty('border-color');
          }
          thumb.setAttribute('aria-current', active ? 'true' : 'false');
        });
        if (scroll) { thumbs[current].scrollIntoView({ block: 'nearest', inline: 'center' }); }
      }

      controls.forEach(function (control) { control.hidden = thumbs.length < 2; });
      root.addEventListener('click', function (event) {
        var thumb = event.target.closest('[data-gallery-thumb]');
        if (thumb && thumbs.indexOf(thumb) !== -1) { show(thumbs.indexOf(thumb), true); return; }
        if (event.target.closest('[data-gallery-prev]')) { show(current - 1, true); }
        else if (event.target.closest('[data-gallery-next]')) { show(current + 1, true); }
      });
      show(0);
    });
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); }
  else { init(); }
}());
