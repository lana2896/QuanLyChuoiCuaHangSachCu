/*
 * Trang /store/transfers: chi nhánh lập phiếu điều chuyển sách sang chi nhánh khác,
 * nhận / từ chối phiếu gửi tới, hủy phiếu mình đã gửi khi bên nhận chưa xử lý.
 */
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('trWorkspace')) { return; }

  var ROLE = 'CHU_CUA_HANG';
  var BASE = '/api/store/transfers';
  var transfers = [];
  var tab = 'DEN_CHO';
  var books = [];
  var busy = false;
  var rejecting = null;
  var createModal, rejectModal;

  function $(id) { return document.getElementById(id); }
  function esc(v) { return OB.escapeHtml(v === null || v === undefined ? '' : v); }
  function show(id, on) { $(id).classList.toggle('d-none', !on); }
  function authError(err) { return OB.auth.handleAuthError(err, ROLE); }

  function setError(id, text) {
    $(id).textContent = text || '';
    show(id, !!text);
  }

  function dateTime(value) {
    var m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(String(value || ''));
    return m ? m[3] + '/' + m[2] + '/' + m[1] + ' ' + m[4] + ':' + m[5] : '';
  }

  function percent(v) {
    return v === null || v === undefined ? '' : Math.round(Number(v)) + '% mới';
  }

  function statusClass(s) {
    return s === 'CHO_NHAN' ? 'is-wait' : s === 'DA_NHAN' ? 'is-done' : 'is-off';
  }

  function inTab(t) {
    if (tab === 'DEN_CHO') { return t.chieu === 'DEN' && t.trangThai === 'CHO_NHAN'; }
    if (tab === 'DEN') { return t.chieu === 'DEN' && t.trangThai !== 'CHO_NHAN'; }
    return t.chieu === 'DI';
  }

  function renderCounts() {
    var counts = { DEN_CHO: 0, DI: 0, DEN: 0 };
    transfers.forEach(function (t) {
      if (t.chieu === 'DEN' && t.trangThai === 'CHO_NHAN') { counts.DEN_CHO++; }
      else if (t.chieu === 'DEN') { counts.DEN++; }
      if (t.chieu === 'DI') { counts.DI++; }
    });
    document.querySelectorAll('[data-tr-count]').forEach(function (el) {
      var n = counts[el.getAttribute('data-tr-count')];
      el.textContent = n ? '(' + n + ')' : '';
    });
  }

  function actions(t) {
    if (t.trangThai !== 'CHO_NHAN') { return ''; }
    if (t.chieu === 'DEN') {
      return '<div class="transfer-actions">' +
        '<button type="button" class="btn btn-sm btn-accent" data-accept="' + t.maPhieu + '">Nhận vào kho</button>' +
        '<button type="button" class="btn btn-sm btn-outline-danger" data-reject="' + t.maPhieu + '">Từ chối</button></div>';
    }
    return '<div class="transfer-actions"><button type="button" class="btn btn-sm btn-outline-secondary" data-cancel="' +
      t.maPhieu + '">Hủy phiếu</button></div>';
  }

  function card(t) {
    var cover = t.hinhAnhUrl && /^https:\/\//i.test(t.hinhAnhUrl)
      ? '<img class="transfer-cover" src="' + esc(t.hinhAnhUrl) + '" alt="" loading="lazy">'
      : '<span class="transfer-cover d-inline-block" aria-hidden="true"></span>';
    var extra = '';
    if (t.ghiChu) { extra += '<div class="small mt-1">Ghi chú: ' + esc(t.ghiChu) + '</div>'; }
    if (t.lyDoTuChoi) { extra += '<div class="small mt-1 text-danger">Lý do từ chối: ' + esc(t.lyDoTuChoi) + '</div>'; }
    if (t.trangThai === 'DA_NHAN' && t.maSachDich && t.chieu === 'DEN') {
      extra += '<div class="small mt-1"><a href="/books/' + encodeURIComponent(t.maSachDich) + '">Xem bản sách trong kho của bạn</a></div>';
    }
    return '<article class="panel transfer-card">' + cover +
      '<div><div class="d-flex flex-wrap align-items-center gap-2 mb-1">' +
      '<strong>' + esc(t.tenSach) + '</strong><span class="transfer-status ' + statusClass(t.trangThai) + '">' + esc(t.tenTrangThai) + '</span></div>' +
      '<div class="small text-muted mb-1">Phiếu #' + esc(t.maPhieu) + ' · ' + esc(percent(t.doMoiPercent)) +
      (t.tinhTrangVatLy ? ' · ' + esc(t.tinhTrangVatLy) : '') + ' · <strong>' + esc(t.soLuong) + ' cuốn</strong></div>' +
      '<div class="transfer-route"><strong>' + esc(t.tenCuaHangNguon) + '</strong><i class="bi bi-arrow-right" aria-label="chuyển tới"></i><strong>' +
      esc(t.tenCuaHangDich) + '</strong><span>· lập lúc ' + esc(dateTime(t.ngayTao)) + '</span>' +
      (t.ngayXuLy ? '<span>· xử lý lúc ' + esc(dateTime(t.ngayXuLy)) + '</span>' : '') + '</div>' + extra + '</div>' +
      actions(t) + '</article>';
  }

  function render() {
    renderCounts();
    var rows = transfers.filter(inTab);
    $('trList').innerHTML = rows.map(card).join('');
    var empty = { DEN_CHO: 'Không có sách nào đang chờ bạn nhận.', DI: 'Bạn chưa gửi phiếu điều chuyển nào. Bấm "Lập phiếu điều chuyển" để bắt đầu.', DEN: 'Chưa có phiếu nào gửi tới chi nhánh của bạn đã xử lý.' };
    $('trEmpty').textContent = empty[tab];
    show('trEmpty', rows.length === 0);
  }

  function load() {
    setError('trError', '');
    return OB.request('GET', BASE).then(function (data) {
      transfers = data || [];
      show('trLoading', false);
      render();
    }, function (err) {
      show('trLoading', false);
      if (authError(err)) { return; }
      setError('trError', err.message || 'Không tải được phiếu điều chuyển.');
    });
  }

  function selectTab(next) {
    tab = next;
    document.querySelectorAll('[data-tr-tab]').forEach(function (b) {
      var on = b.getAttribute('data-tr-tab') === next;
      b.classList.toggle('active', on);
      b.setAttribute('aria-selected', on ? 'true' : 'false');
    });
    render();
  }

  // ---------- lập phiếu ----------
  function selectedBook() {
    var id = $('trBook').value;
    return books.find(function (b) { return String(b.maSach) === id; }) || null;
  }

  function syncQtyHint() {
    var b = selectedBook();
    $('trQty').max = b ? b.soLuongTon : '';
    $('trQtyHint').textContent = b ? 'Kho còn ' + b.soLuongTon + ' cuốn.' : '';
  }

  function openCreate() {
    setError('trCreateError', '');
    $('trBook').innerHTML = '<option value="">Đang tải...</option>';
    $('trDest').innerHTML = '<option value="">Đang tải...</option>';
    $('trQty').value = 1;
    $('trNote').value = '';
    createModal.show();
    Promise.all([
      OB.request('GET', '/api/store/me/books?page=0&size=100&sort=tenSach,asc&trangThaiDuyet=DA_DUYET'),
      OB.request('GET', BASE + '/destinations')
    ]).then(function (res) {
      books = ((res[0] && res[0].content) || []).filter(function (b) { return b.soLuongTon > 0; });
      var dests = res[1] || [];
      $('trBook').innerHTML = books.length
        ? '<option value="">Chọn sách</option>' + books.map(function (b) {
          var name = b.tenSach.length > 50 ? b.tenSach.slice(0, 49) + '…' : b.tenSach;
          return '<option value="' + b.maSach + '" title="' + esc(b.tenSach) + '">' + esc(name) + ' — ' + esc(percent(b.doMoiPercent)) + ' (còn ' + b.soLuongTon + ')</option>';
        }).join('')
        : '<option value="">Kho chưa có sách đã duyệt còn hàng</option>';
      $('trDest').innerHTML = dests.length
        ? '<option value="">Chọn chi nhánh nhận</option>' + dests.map(function (d) {
          return '<option value="' + d.maCH + '">' + esc(d.tenCuaHang) + (d.diaChiCH ? ' — ' + esc(d.diaChiCH) : '') + '</option>';
        }).join('')
        : '<option value="">Chuỗi chưa có chi nhánh khác đang hoạt động</option>';
      syncQtyHint();
    }, function (err) {
      if (authError(err)) { return; }
      setError('trCreateError', err.message || 'Không tải được dữ liệu để lập phiếu.');
    });
  }

  function submitCreate(e) {
    e.preventDefault();
    if (busy) { return; }
    var b = selectedBook();
    var qty = parseInt($('trQty').value, 10);
    if (!b) { setError('trCreateError', 'Chọn sách cần điều chuyển.'); return; }
    if (!$('trDest').value) { setError('trCreateError', 'Chọn chi nhánh nhận.'); return; }
    if (!qty || qty < 1 || qty > b.soLuongTon) {
      setError('trCreateError', 'Số lượng phải từ 1 đến ' + b.soLuongTon + '.'); return;
    }
    busy = true;
    $('trSubmit').disabled = true;
    OB.request('POST', BASE, {
      maSach: b.maSach, maCHDich: parseInt($('trDest').value, 10), soLuong: qty,
      ghiChu: $('trNote').value.trim() || null
    }).then(function () {
      createModal.hide();
      OB.toast('Đã gửi phiếu điều chuyển. Sách đã rời kho chi nhánh của bạn.', 'success');
      selectTab('DI');
      return load();
    }, function (err) {
      if (authError(err)) { return; }
      setError('trCreateError', err.message || 'Không lập được phiếu điều chuyển.');
    }).finally(function () { busy = false; $('trSubmit').disabled = false; });
  }

  // ---------- nhận / từ chối / hủy ----------
  function act(url, body, okText) {
    if (busy) { return Promise.resolve(); }
    busy = true;
    return OB.request('POST', url, body).then(function () {
      OB.toast(okText, 'success');
      return load();
    }, function (err) {
      if (authError(err)) { return; }
      OB.toast(err.message || 'Thao tác không thành công.', 'danger');
      throw err;
    }).finally(function () { busy = false; });
  }

  function onListClick(e) {
    var accept = e.target.closest('[data-accept]');
    var reject = e.target.closest('[data-reject]');
    var cancel = e.target.closest('[data-cancel]');
    if (accept) {
      var id = accept.getAttribute('data-accept');
      OB.confirm('Xác nhận đã nhận đủ sách và nhập vào kho chi nhánh?', { confirmText: 'Nhận vào kho', cancelText: 'Để sau' }).then(function (ok) {
        if (ok) { act(BASE + '/' + encodeURIComponent(id) + '/accept', undefined, 'Đã nhập sách vào kho chi nhánh.').catch(function () { }); }
      });
    } else if (reject) {
      rejecting = reject.getAttribute('data-reject');
      $('trRejectReason').value = '';
      setError('trRejectError', '');
      rejectModal.show();
    } else if (cancel) {
      var cid = cancel.getAttribute('data-cancel');
      OB.confirm('Hủy phiếu này? Sách sẽ được trả lại kho chi nhánh của bạn.', { confirmText: 'Hủy phiếu', cancelText: 'Giữ phiếu' }).then(function (ok) {
        if (ok) { act(BASE + '/' + encodeURIComponent(cid) + '/cancel', undefined, 'Đã hủy phiếu, sách đã về lại kho.').catch(function () { }); }
      });
    }
  }

  function submitReject(e) {
    e.preventDefault();
    var reason = $('trRejectReason').value.trim();
    if (!reason) { setError('trRejectError', 'Nhập lý do từ chối.'); return; }
    act(BASE + '/' + encodeURIComponent(rejecting) + '/reject', { lyDo: reason }, 'Đã từ chối phiếu, sách được trả về chi nhánh gửi.')
      .then(function () { rejectModal.hide(); }, function (err) { setError('trRejectError', err && err.message); });
  }

  function init() {
    if (!OB.auth.isLoggedIn()) { OB.auth.redirectToLogin('/store/transfers'); return; }
    if (!OB.auth.hasRole(ROLE)) { show('trDenied', true); return; }
    show('trWorkspace', true);
    createModal = new window.bootstrap.Modal($('trCreateModal'));
    rejectModal = new window.bootstrap.Modal($('trRejectModal'));
    document.querySelectorAll('[data-tr-tab]').forEach(function (b) {
      b.addEventListener('click', function () { selectTab(b.getAttribute('data-tr-tab')); });
    });
    $('trOpenCreate').addEventListener('click', openCreate);
    $('trBook').addEventListener('change', syncQtyHint);
    $('trCreateForm').addEventListener('submit', submitCreate);
    $('trRejectForm').addEventListener('submit', submitReject);
    $('trList').addEventListener('click', onListClick);
    load();
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }
})();
