(function () {
  'use strict';

  var OB = window.OldBook;
  var detail = document.querySelector('[data-viewed-book-id]');
  var grid = document.getElementById('recentlyViewedGrid');
  if (!OB || !OB.bookCards || (!detail && !grid) || OB.recentlyViewed) { return; }

  var Auth = OB.auth;
  var ROLE = OB.ROLE_CUSTOMER;
  var BASE = '/api/recently-viewed';
  var loading = false;
  var History = {
    get: function () { return OB.request('GET', BASE); },
    record: function (id) { return OB.request('PUT', BASE + '/' + encodeURIComponent(id)); }
  };
  OB.recentlyViewed = History;

  function $(id) { return document.getElementById(id); }
  function visible(id, show) {
    var element = $(id);
    if (element) { element.classList.toggle('d-none', !show); }
  }

  function load() {
    if (loading || !Auth.hasRole(ROLE)) { return; }
    loading = true;
    visible('recentlyViewedError', false);
    visible('recentlyViewedRetry', false);
    visible('recentlyViewedEmpty', false);
    visible('recentlyViewedLoading', true);
    $('recentlyViewedRetry').disabled = true;
    History.get().then(function (items) {
      var seen = Object.create(null);
      var books = (Array.isArray(items) ? items : []).filter(function (book) {
        var id = book && OB.bookCards.bookId(book.maSach);
        if (!id || seen[id]) { return false; }
        seen[id] = true;
        return true;
      }).slice(0, 20);
      grid.innerHTML = books.map(function (book) { return OB.bookCards.render(book, { showWishlist: false }); }).join('');
      $('recentlyViewedCount').textContent = books.length + ' cuốn';
      visible('recentlyViewedEmpty', books.length === 0);
    }, function (err) {
      if (Auth.handleAuthError(err, ROLE)) { return; }
      $('recentlyViewedError').textContent = err.message || 'Không tải được sách đã xem. Vui lòng thử lại.';
      visible('recentlyViewedError', true);
      visible('recentlyViewedRetry', true);
    }).then(function () {
      loading = false;
      visible('recentlyViewedLoading', false);
      $('recentlyViewedRetry').disabled = false;
    });
  }

  function recordCurrentBook() {
    if (detail) {
      var id = OB.bookCards.bookId(detail.getAttribute('data-viewed-book-id'));
      if (id) {
        // Viewing a book should stay usable if the background history write fails.
        History.record(id).catch(function () {});
      }
    }
  }

  function init() {
    if (!Auth.hasRole(ROLE)) { return; }
    recordCurrentBook();
    if (grid) {
      visible('recentlyViewedSection', true);
      $('recentlyViewedRetry').addEventListener('click', load);
      load();
    }
  }

  window.addEventListener('pageshow', function (event) {
    if (!event.persisted || !Auth.hasRole(ROLE)) { return; }
    recordCurrentBook();
    if (grid) { load(); }
  });

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
