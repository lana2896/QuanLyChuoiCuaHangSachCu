// Trang Thanh toán & Đối soát của Quản lý: chi trả cửa hàng theo đợt, hoàn tiền khách, sổ giao dịch ngân hàng.
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB || !document.getElementById('paymentsPage')) { return; }

  var Auth = OB.auth;
  var Banks = window.OldBookBanks;
  var BASE = '/api/management/payments';
  var SIZE = 20;
  var TABS = ['payouts', 'refunds', 'transactions'];

  var TRANSFER_STATUS = {
    CHO_THONG_TIN: { label: 'Chờ khách nhập tài khoản', css: 'is-bad' },
    CHO_XU_LY: { label: 'Chờ chuyển khoản', css: 'is-wait' },
    DA_CHUYEN: { label: 'Đã chuyển', css: 'is-ok' }
  };
  var MATCH_STATUS = {
    KHOP: { label: 'Khớp, đã tạo đơn', css: 'is-ok' },
    KHONG_KHOP: { label: 'Không khớp', css: 'is-wait' },
    CAN_HOAN_TIEN: { label: 'Phải hoàn tiền', css: 'is-bad' },
    DANG_XU_LY: { label: 'Đang xử lý', css: 'is-wait' }
  };
  var CANCEL_BY = { KHACH_HANG: 'Khách', CHU_CUA_HANG: 'Cửa hàng', HE_THONG: 'Hệ thống' };

  var tab = 'payouts';
  var groups = [];
  var expanded = {};         // maCH -> true
  var selection = { maCH: null, ids: [] };
  var refundRows = [], refundPage = 0;
  var txPage = 0;
  var transfer = null;       // { kind: 'refund'|'payout', ... } đang mở trong modal
  var busy = false;
  var ids = { payouts: 0, refunds: 0, transactions: 0 };
  var keywordTimer = null;

  function $(id) { return document.getElementById(id); }
  function show(id) { $(id).classList.remove('d-none'); }
  function hide(id) { $(id).classList.add('d-none'); }
  function esc(value) { return OB.escapeHtml(value === null || value === undefined ? '' : value); }
  function money(value) { return OB.formatMoney(value); }
  function modal() { return window.bootstrap.Modal.getOrCreateInstance($('transferModal')); }

  function dateTime(value) {
    if (!value) { return '—'; }
    var m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(String(value));
    return m ? m[3] + '/' + m[2] + '/' + m[1] + ' ' + m[4] + ':' + m[5] : String(value);
  }
  function hhmm(value) {
    var m = /T(\d{2}):(\d{2})/.exec(String(value || ''));
    return m ? m[1] + ':' + m[2] : '';
  }
  function pill(map, value) {
    var s = map[value] || { label: value, css: 'is-wait' };
    return '<span class="pay-status ' + s.css + '">' + esc(s.label) + '</span>';
  }
  function bankLine(bank, number) {
    return number ? esc(Banks.name(bank)) + '<br><span class="small text-muted">' + esc(number) + '</span>' : '';
  }
  // Có số tài khoản thì hiện tài khoản; chỉ có ảnh QR thì ghi rõ để Quản lý biết phải mở ảnh.
  function receiver(bank, number, qr) {
    if (number) { return bankLine(bank, number); }
    return qr ? '<span class="pay-status is-ok">Ảnh mã QR đã tải lên</span>' : '';
  }
  function fail(err) {
    if (Auth.handleAuthError && Auth.handleAuthError(err)) { return; }
    OB.toast(err.message, 'danger');
  }
  function showError(message) {
    $('paymentsErrorText').textContent = message || 'Không tải được dữ liệu.';
    show('paymentsError');
  }

  // ---------------------------------------------------------------
  // Thẻ con
  // ---------------------------------------------------------------

  function selectTab(next, fromUser) {
    if (TABS.indexOf(next) < 0) { next = 'payouts'; }
    tab = next;
    document.querySelectorAll('[data-tab]').forEach(function (button) {
      var active = button.getAttribute('data-tab') === tab;
      button.classList.toggle('active', active);
      button.setAttribute('aria-selected', active ? 'true' : 'false');
    });
    document.querySelectorAll('[data-pane]').forEach(function (pane) {
      pane.classList.toggle('d-none', pane.getAttribute('data-pane') !== tab);
    });
    if (fromUser && OB.managerShell) { OB.managerShell.setQuery('tab', tab); }
    hide('paymentsError');
    loadTab(tab);
  }

  function loadTab(name) {
    if (name === 'payouts') { loadPayouts(); }
    if (name === 'refunds') { loadRefunds(); }
    if (name === 'transactions') { loadTransactions(); }
  }

  // Số đếm trên ba thẻ con, lấy từ API tổng quan.
  function loadCounts() {
    OB.request('GET', '/api/management/overview').then(function (d) {
      if (!d) { return; }
      setCount('payouts', d.cuaHangChoChi);
      setCount('refunds', d.hoanTienCho + d.hoanTienChoKhachKhai);
      setCount('transactions', d.giaoDichLech);
    }, function () { });
  }
  function setCount(name, n) {
    var node = document.querySelector('[data-count="' + name + '"]');
    if (!node) { return; }
    node.textContent = String(n || 0);
    node.classList.toggle('d-none', !n);
  }

  // ---------------------------------------------------------------
  // Chi trả cửa hàng
  // ---------------------------------------------------------------

  function loadPayouts() {
    var id = ++ids.payouts;
    hide('payoutEmpty');
    show('payoutLoading');
    OB.request('GET', BASE + '/store-payouts?trangThai=' + encodeURIComponent($('payoutStatus').value)).then(function (data) {
      if (id !== ids.payouts) { return; }
      groups = data || [];
      reconcileSelection();
      renderPayouts();
      loadCounts();
      if (OB.managerShell) { OB.managerShell.touch(); }
    }, function (err) {
      if (id !== ids.payouts) { return; }
      hide('payoutLoading');
      showError(err.message);
    });
  }

  // Sau mỗi lần tải lại: bỏ tích những khoản vừa được người khác chuyển xong (hoặc cửa hàng không còn trong danh sách).
  function reconcileSelection() {
    if (selection.maCH === null) { return; }
    var group = groups.find(function (g) { return g.maCH === selection.maCH; });
    var pending = {};
    if (group) {
      group.donCon.forEach(function (d) { if (d.trangThai === 'CHO_XU_LY') { pending[d.maYeuCau] = true; } });
    }
    var before = selection.ids.length;
    selection.ids = selection.ids.filter(function (id) { return pending[id]; });
    if (selection.ids.length < before) {
      OB.toast('Một số đơn bạn đã tích vừa được xử lý xong, đã bỏ tích.', 'info');
    }
    if (!selection.ids.length) { selection.maCH = null; }
  }

  function pendingOf(group) {
    return group.donCon.filter(function (d) { return d.trangThai === 'CHO_XU_LY'; });
  }

  function renderPayouts() {
    hide('payoutLoading');
    var html = '';
    groups.forEach(function (g) {
      var open = !!expanded[g.maCH];
      var remindActive = g.daNhacLuc && (Date.now() - new Date(g.daNhacLuc).getTime()) < 3600000;
      var action = g.coTaiKhoan
        ? '<button type="button" class="btn btn-sm btn-outline-brown" data-toggle="' + g.maCH + '">' + (open ? 'Thu gọn' : 'Chọn đơn') + '</button>'
        : '<button type="button" class="btn btn-sm btn-accent" data-remind-store="' + g.maCH + '"' + (remindActive ? ' disabled' : '') + '>'
          + (remindActive ? 'Đã nhắc lúc ' + esc(hhmm(g.daNhacLuc)) : 'Gửi thông báo cập nhật STK') + '</button>';

      html += '<tr class="mg-group-row" data-group="' + g.maCH + '">'
        + '<td><i class="bi ' + (open ? 'bi-chevron-down' : 'bi-chevron-right') + '" aria-hidden="true"></i></td>'
        + '<td><strong>' + esc(g.tenCuaHang) + '</strong><br><span class="small text-muted">'
        + esc(g.tenChuShop) + (g.soDienThoai ? ' · ' + esc(g.soDienThoai) : '') + '</span></td>'
        + '<td>' + (g.coTaiKhoan ? receiver(g.maNganHang, g.soTaiKhoan, g.qrNganHangUrl) : '<span class="pay-status is-bad">Chưa khai tài khoản</span>') + '</td>'
        + '<td class="text-end">' + g.soDon + '</td>'
        + '<td class="text-end">' + esc(money(g.tongTienHang)) + '</td>'
        + '<td class="text-end">' + esc(money(g.tongHoaHong)) + '</td>'
        + '<td class="text-end price"><strong>' + esc(money(g.tongThucNhan)) + '</strong></td>'
        + '<td class="text-end text-nowrap">' + action + '</td></tr>';

      if (open) { html += childRows(g); }
    });
    $('payoutRows').innerHTML = html;
    if (groups.length) { hide('payoutEmpty'); } else { show('payoutEmpty'); }
    renderBar();
  }

  function childRows(g) {
    var rows = g.donCon.map(function (d) {
      var canPick = g.coTaiKhoan && d.trangThai === 'CHO_XU_LY';
      var checked = selection.maCH === g.maCH && selection.ids.indexOf(d.maYeuCau) >= 0;
      return '<tr><td><input type="checkbox" class="form-check-input" data-pick="' + d.maYeuCau + '" data-store="' + g.maCH + '"'
        + (canPick ? '' : ' disabled') + (checked ? ' checked' : '') + ' aria-label="Chọn đơn #' + d.maDHCH + '"></td>'
        + '<td>#' + d.maDHCH + ' / #' + d.maDH + '</td>'
        + '<td>' + esc(dateTime(d.ngayGiao)) + '</td>'
        + '<td class="text-end">' + esc(money(d.tienHang)) + '</td>'
        + '<td class="text-end">' + esc(money(d.giamGiaShop)) + '</td>'
        + '<td class="text-end">' + esc(money(d.hoaHong)) + '</td>'
        + '<td class="text-end"><strong>' + esc(money(d.thucNhan)) + '</strong></td>'
        + '<td>' + pill(TRANSFER_STATUS, d.trangThai) + (d.maDotChi ? '<br><span class="small text-muted">' + esc(d.maDotChi) + '</span>' : '') + '</td></tr>';
    }).join('');
    return '<tr class="mg-child-table"><td></td><td colspan="7"><div class="table-responsive"><table class="table table-sm mb-0">'
      + '<thead><tr><th></th><th>Đơn</th><th>Ngày giao</th><th class="text-end">Tiền hàng</th><th class="text-end">Giảm giá shop</th>'
      + '<th class="text-end">Hoa hồng</th><th class="text-end">Thực nhận</th><th>Trạng thái</th></tr></thead><tbody>'
      + rows + '</tbody></table></div></td></tr>';
  }

  function selectedGroup() {
    return groups.find(function (g) { return g.maCH === selection.maCH; }) || null;
  }

  function selectedTotal(g) {
    return g.donCon.reduce(function (sum, d) {
      return selection.ids.indexOf(d.maYeuCau) >= 0 ? sum + Number(d.thucNhan) : sum;
    }, 0);
  }

  function renderBar() {
    var g = selectedGroup();
    if (!g || !selection.ids.length) { hide('payoutBar'); return; }
    $('payoutBarStore').textContent = g.tenCuaHang;
    $('payoutBarCount').textContent = selection.ids.length + ' đơn đang chọn';
    $('payoutBarAccount').textContent = g.soTaiKhoan ? Banks.name(g.maNganHang) + ' · ' + g.soTaiKhoan : 'Ảnh mã QR ngân hàng';
    $('payoutBarTotal').textContent = money(selectedTotal(g));
    show('payoutBar');
  }

  function toggleGroup(maCH) {
    var g = groups.find(function (x) { return String(x.maCH) === String(maCH); });
    if (!g) { return; }
    expanded[g.maCH] = !expanded[g.maCH];
    // Mở một cửa hàng: tích sẵn các đơn đang chờ chuyển của cửa hàng đó.
    if (expanded[g.maCH] && g.coTaiKhoan && selection.maCH !== g.maCH) {
      if (selection.maCH !== null) { OB.toast('Mỗi lần chuyển khoản chỉ gồm đơn của một cửa hàng; đã bỏ chọn cửa hàng trước.', 'info'); }
      selection = { maCH: g.maCH, ids: pendingOf(g).map(function (d) { return d.maYeuCau; }) };
    }
    renderPayouts();
  }

  function pick(checkbox) {
    var maCH = Number(checkbox.getAttribute('data-store'));
    var id = Number(checkbox.getAttribute('data-pick'));
    if (selection.maCH !== maCH) {
      if (selection.maCH !== null && selection.ids.length) {
        OB.toast('Mỗi lần chuyển khoản chỉ gồm đơn của một cửa hàng; đã bỏ chọn cửa hàng trước.', 'info');
      }
      selection = { maCH: maCH, ids: [] };
    }
    var at = selection.ids.indexOf(id);
    if (checkbox.checked && at < 0) { selection.ids.push(id); }
    if (!checkbox.checked && at >= 0) { selection.ids.splice(at, 1); }
    if (!selection.ids.length) { selection.maCH = null; }
    renderPayouts();
  }

  function remindStore(maCH) {
    if (busy) { return; }
    busy = true;
    OB.request('POST', BASE + '/stores/' + encodeURIComponent(maCH) + '/remind-bank').then(function (r) {
      OB.toast('Đã gửi thông báo cho chủ cửa hàng', 'success');
      var g = groups.find(function (x) { return String(x.maCH) === String(maCH); });
      if (g && r) { g.daNhacLuc = r.daNhacLuc; }
      renderPayouts();
    }, fail).then(function () { busy = false; });
  }

  function createPayout() {
    var g = selectedGroup();
    if (busy || !g || !selection.ids.length) { return; }
    busy = true;
    OB.request('POST', BASE + '/store-payouts/preview', { maYeuCau: selection.ids }).then(function (p) {
      transfer = { kind: 'payout', ids: selection.ids.slice(), maDotChi: p.maDotChi, soTien: p.tongTien, label: p.maDotChi };
      openModal({
        title: 'Đợt chi ' + p.maDotChi + ' · ' + p.soDon + ' đơn',
        recipient: p.tenCuaHang, bank: p.taiKhoan.maNganHang, account: p.taiKhoan.soTaiKhoan,
        accountName: p.taiKhoan.tenChuTaiKhoan, amount: p.tongTien, code: p.maDotChi,
        qr: p.qrUrl || p.anhQrNganHang, uploadedQr: !p.qrUrl && !!p.anhQrNganHang, canDone: true
      });
    }, fail).then(function () { busy = false; });
  }

  // ---------------------------------------------------------------
  // Hoàn tiền khách
  // ---------------------------------------------------------------

  function loadRefunds() {
    var id = ++ids.refunds;
    hide('refundEmpty');
    show('refundLoading');
    var url = BASE + '/transfers?loai=HOAN_TIEN_KHACH&page=' + refundPage + '&size=' + SIZE
      + ($('refundStatus').value ? '&trangThai=' + $('refundStatus').value : '')
      + ($('refundKeyword').value.trim() ? '&tuKhoa=' + encodeURIComponent($('refundKeyword').value.trim()) : '');
    OB.request('GET', url).then(function (data) {
      if (id !== ids.refunds) { return; }
      data = data || {};
      refundRows = data.content || [];
      hide('refundLoading');
      $('refundRows').innerHTML = refundRows.map(refundRow).join('');
      if (refundRows.length) { hide('refundEmpty'); } else { show('refundEmpty'); }
      var pages = data.totalPages || 0;
      $('refundSummary').textContent = (data.totalElements || 0) + ' khoản' + (pages > 1 ? ' · trang ' + (refundPage + 1) + '/' + pages : '');
      $('refundPrev').disabled = refundPage <= 0;
      $('refundNext').disabled = refundPage + 1 >= pages;
      loadCounts();
      if (OB.managerShell) { OB.managerShell.touch(); }
    }, function (err) {
      if (id !== ids.refunds) { return; }
      hide('refundLoading');
      $('refundRows').innerHTML = '';
      showError(err.message);
    });
  }

  function refundRow(row) {
    var action;
    if (row.trangThai === 'CHO_THONG_TIN') {
      action = '<button type="button" class="btn btn-sm btn-accent" data-remind-refund="' + row.maYeuCau + '">Gửi thông báo cập nhật STK</button>';
    } else if (row.trangThai === 'CHO_XU_LY') {
      action = '<button type="button" class="btn btn-sm btn-accent" data-refund="' + row.maYeuCau + '">Tạo mã VietQR</button>';
    } else {
      action = '<span class="small text-muted">' + esc(dateTime(row.ngayXuLy)) + '</span>';
    }
    var by = row.huyBoi ? (CANCEL_BY[row.huyBoi] || row.huyBoi) : '';
    return '<tr>'
      + '<td><strong>' + esc(row.noiDungChuyenKhoan) + '</strong><br><span class="small text-muted">' + esc(dateTime(row.ngayTao)) + '</span></td>'
      + '<td>' + (row.maDHCH ? 'Đơn #' + row.maDHCH + ' / #' + row.maDH : 'Chưa tạo được đơn')
      + (by ? '<br><span class="small">Bên hủy: ' + esc(by) + '</span>' : '')
      + (row.lyDoHuy || row.lyDo ? '<br><span class="small text-muted">' + esc(row.lyDoHuy || row.lyDo) + '</span>' : '') + '</td>'
      + '<td>' + esc(row.tenNguoiNhan || row.emailNguoiNhan) + '<br><span class="small text-muted">' + esc(row.soDienThoaiNguoiNhan || row.emailNguoiNhan) + '</span></td>'
      + '<td>' + (row.soTaiKhoan || row.qrNganHangUrl ? receiver(row.maNganHang, row.soTaiKhoan, row.qrNganHangUrl) : '<span class="text-muted">Khách chưa nhập</span>') + '</td>'
      + '<td class="text-end price">' + esc(money(row.soTien)) + '</td>'
      + '<td>' + pill(TRANSFER_STATUS, row.trangThai) + '</td>'
      + '<td class="text-end">' + action + '</td></tr>';
  }

  function remindRefund(id) {
    if (busy) { return; }
    busy = true;
    OB.request('POST', BASE + '/refunds/' + encodeURIComponent(id) + '/remind-bank').then(function () {
      OB.toast('Đã nhắc khách cập nhật tài khoản nhận hoàn', 'success');
    }, fail).then(function () { busy = false; });
  }

  function openRefund(id) {
    var row = refundRows.find(function (r) { return String(r.maYeuCau) === String(id); });
    if (!row) { return; }
    transfer = { kind: 'refund', id: row.maYeuCau, soTien: row.soTien, label: row.noiDungChuyenKhoan };
    openModal({
      title: 'Hoàn tiền ' + row.noiDungChuyenKhoan, recipient: row.tenNguoiNhan || row.emailNguoiNhan,
      bank: row.maNganHang, account: row.soTaiKhoan, accountName: row.tenChuTaiKhoan,
      amount: row.soTien, code: row.noiDungChuyenKhoan, qr: row.qrUrl || row.qrNganHangUrl, uploadedQr: !row.qrUrl && !!row.qrNganHangUrl,
      canDone: (!!row.soTaiKhoan || !!row.qrNganHangUrl) && row.trangThai === 'CHO_XU_LY'
    });
  }

  // ---------------------------------------------------------------
  // Modal chuyển khoản (dùng chung cho hoàn tiền và đợt chi)
  // ---------------------------------------------------------------

  function openModal(m) {
    $('transferTitle').textContent = m.title;
    $('transferRecipient').textContent = m.recipient || '';
    $('transferBank').textContent = m.account ? Banks.name(m.bank) : '—';
    $('transferAccount').textContent = m.account || '—';
    $('transferAccountName').textContent = m.accountName || '—';
    $('transferAmount').textContent = money(m.amount);
    $('transferCode').textContent = m.code;
    if (m.qr) { $('transferQr').setAttribute('src', m.qr); show('transferQrBox'); } else { hide('transferQrBox'); }
    // Ảnh QR do người nhận tải lên không mang sẵn số tiền và nội dung: Quản lý tự nhập trong app ngân hàng.
    $('transferUploadedNote').classList.toggle('d-none', !m.uploadedQr);
    if (m.canDone) { hide('transferNoAccount'); show('transferDone'); }
    else {
      $('transferNoAccount').textContent = 'Chưa đủ thông tin tài khoản nhận để chuyển khoản.';
      show('transferNoAccount');
      hide('transferDone');
    }
    modal().show();
  }

  function markDone() {
    if (busy || !transfer) { return; }
    var t = transfer;
    OB.confirm('Xác nhận đã chuyển ' + money(t.soTien) + ' với nội dung ' + t.label + '? Thao tác này không hoàn tác được.', {
      confirmText: 'Đã chuyển'
    }).then(function (ok) {
      if (!ok) { return; }
      busy = true;
      $('transferDone').disabled = true;
      var call = t.kind === 'payout'
        ? OB.request('POST', BASE + '/store-payouts/done', { maDotChi: t.maDotChi, maYeuCau: t.ids })
        : OB.request('POST', BASE + '/transfers/' + encodeURIComponent(t.id) + '/done');
      call.then(function () {
        modal().hide();
        OB.toast('Đã ghi nhận ' + t.label, 'success');
        if (t.kind === 'payout') { selection = { maCH: null, ids: [] }; }
        loadTab(tab);
      }, fail).then(function () { busy = false; $('transferDone').disabled = false; });
    });
  }

  // ---------------------------------------------------------------
  // Giao dịch ngân hàng
  // ---------------------------------------------------------------

  function loadTransactions() {
    var id = ++ids.transactions;
    hide('txEmpty');
    show('txLoading');
    var q = '?page=' + txPage + '&size=' + SIZE;
    if ($('txKeyword').value.trim()) { q += '&tuKhoa=' + encodeURIComponent($('txKeyword').value.trim()); }
    if ($('txResult').value) { q += '&ketQua=' + $('txResult').value; }
    if ($('txFrom').value) { q += '&tuNgay=' + $('txFrom').value; }
    if ($('txTo').value) { q += '&denNgay=' + $('txTo').value; }
    OB.request('GET', BASE + '/transactions' + q).then(function (data) {
      if (id !== ids.transactions) { return; }
      data = data || {};
      var rows = data.content || [];
      hide('txLoading');
      $('txRows').innerHTML = rows.map(txRow).join('');
      if (rows.length) { hide('txEmpty'); } else { show('txEmpty'); }
      var t = data.tomTat || {};
      $('txTotal').textContent = t.tongSo === undefined ? '—' : t.tongSo;
      $('txMatched').textContent = money(t.tongTienKhop || 0);
      $('txMismatch').textContent = money(t.tongTienLech || 0);
      var pages = data.totalPages || 0;
      $('txSummary').textContent = (data.totalElements || 0) + ' giao dịch' + (pages > 1 ? ' · trang ' + (txPage + 1) + '/' + pages : '');
      $('txPrev').disabled = txPage <= 0;
      $('txNext').disabled = txPage + 1 >= pages;
      loadCounts();
      if (OB.managerShell) { OB.managerShell.touch(); }
    }, function (err) {
      if (id !== ids.transactions) { return; }
      hide('txLoading');
      $('txRows').innerHTML = '';
      showError(err.message);
    });
  }

  function txRow(row) {
    return '<tr>'
      + '<td>' + esc(dateTime(row.thoiGianGiaoDich || row.ngayNhan)) + '<br><span class="small text-muted">' + esc(row.maGiaoDichCong) + '</span></td>'
      + '<td class="pay-ledger-text">' + esc(row.noiDung) + '</td>'
      + '<td>' + esc(row.tenNguoiChuyen || '—') + '<br><span class="small text-muted">' + esc(row.soTaiKhoanChuyen) + '</span></td>'
      + '<td class="text-end price">' + esc(money(row.soTien)) + '</td>'
      + '<td>' + pill(MATCH_STATUS, row.ketQua) + (row.maThanhToan ? '<br><span class="small text-muted">' + esc(row.maThanhToan) + '</span>' : '') + '</td>'
      + '<td class="small text-muted">' + esc(row.ghiChu) + '</td></tr>';
  }

  // ---------------------------------------------------------------

  function copyCode() {
    var text = $('transferCode').textContent;
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(function () { OB.toast('Đã chép nội dung chuyển khoản', 'success'); }, function () { });
    }
  }

  function debounced(fn) {
    return function () {
      window.clearTimeout(keywordTimer);
      keywordTimer = window.setTimeout(fn, 350);
    };
  }

  function init() {
    // Chưa đăng nhập: auth.js đang chuyển về trang đăng nhập (body có data-page-requires-login).
    if (!Auth.isLoggedIn()) { return; }
    var user = Auth.getUser();
    if (!user || user.vaiTro !== 'QUAN_LY') { show('paymentsDenied'); return; }
    show('paymentsContent');

    document.querySelectorAll('[data-tab]').forEach(function (button) {
      button.addEventListener('click', function () { selectTab(button.getAttribute('data-tab'), true); });
    });
    $('paymentsRetry').addEventListener('click', function () { hide('paymentsError'); loadTab(tab); });

    $('payoutStatus').addEventListener('change', function () { selection = { maCH: null, ids: [] }; loadPayouts(); });
    $('payoutRows').addEventListener('click', function (event) {
      var t = event.target;
      var remind = t.closest('[data-remind-store]');
      if (remind) { remindStore(remind.getAttribute('data-remind-store')); return; }
      var toggle = t.closest('[data-toggle]');
      if (toggle) { toggleGroup(toggle.getAttribute('data-toggle')); return; }
      var checkbox = t.closest('[data-pick]');
      if (checkbox) { pick(checkbox); return; }
      var row = t.closest('[data-group]');
      if (row && !t.closest('button')) { toggleGroup(row.getAttribute('data-group')); }
    });
    $('payoutClear').addEventListener('click', function () { selection = { maCH: null, ids: [] }; renderPayouts(); });
    $('payoutCreate').addEventListener('click', createPayout);

    $('refundStatus').addEventListener('change', function () { refundPage = 0; loadRefunds(); });
    $('refundKeyword').addEventListener('input', debounced(function () { refundPage = 0; loadRefunds(); }));
    $('refundPrev').addEventListener('click', function () { if (refundPage > 0) { refundPage -= 1; loadRefunds(); } });
    $('refundNext').addEventListener('click', function () { refundPage += 1; loadRefunds(); });
    $('refundRows').addEventListener('click', function (event) {
      var remind = event.target.closest('[data-remind-refund]');
      if (remind) { remindRefund(remind.getAttribute('data-remind-refund')); return; }
      var open = event.target.closest('[data-refund]');
      if (open) { openRefund(open.getAttribute('data-refund')); }
    });

    ['txResult', 'txFrom', 'txTo'].forEach(function (id) {
      $(id).addEventListener('change', function () { txPage = 0; loadTransactions(); });
    });
    $('txKeyword').addEventListener('input', debounced(function () { txPage = 0; loadTransactions(); }));
    $('txPrev').addEventListener('click', function () { if (txPage > 0) { txPage -= 1; loadTransactions(); } });
    $('txNext').addEventListener('click', function () { txPage += 1; loadTransactions(); });

    $('transferDone').addEventListener('click', markDone);
    $('transferCopy').addEventListener('click', copyCode);

    var wanted = new URLSearchParams(window.location.search).get('tab');
    selectTab(wanted, false);

    if (OB.managerRealtime) {
      OB.managerRealtime.on('payouts', function () { if (tab === 'payouts') { loadPayouts(); } else { loadCounts(); } });
      OB.managerRealtime.on('refunds', function () { if (tab === 'refunds') { loadRefunds(); } else { loadCounts(); } });
      OB.managerRealtime.on('transactions', function () { if (tab === 'transactions') { loadTransactions(); } else { loadCounts(); } });
    }
  }

  init();
})();
