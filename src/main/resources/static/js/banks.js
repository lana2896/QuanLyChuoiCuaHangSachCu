// Danh sách ngân hàng dùng cho các ô chọn tài khoản nhận tiền. Mã là BIN 6 số của Napas, đúng thứ VietQR cần.
(function () {
  'use strict';

  var BANKS = [
    { bin: '970436', name: 'Vietcombank' },
    { bin: '970415', name: 'VietinBank' },
    { bin: '970418', name: 'BIDV' },
    { bin: '970405', name: 'Agribank' },
    { bin: '970407', name: 'Techcombank' },
    { bin: '970422', name: 'MB Bank' },
    { bin: '970416', name: 'ACB' },
    { bin: '970432', name: 'VPBank' },
    { bin: '970423', name: 'TPBank' },
    { bin: '970403', name: 'Sacombank' },
    { bin: '970437', name: 'HDBank' },
    { bin: '970441', name: 'VIB' },
    { bin: '970443', name: 'SHB' },
    { bin: '970431', name: 'Eximbank' },
    { bin: '970426', name: 'MSB' },
    { bin: '970448', name: 'OCB' },
    { bin: '970440', name: 'SeABank' },
    { bin: '970449', name: 'LPBank' },
    { bin: '970428', name: 'Nam A Bank' },
    { bin: '970409', name: 'Bac A Bank' }
  ];

  function name(bin) {
    for (var i = 0; i < BANKS.length; i++) {
      if (BANKS[i].bin === String(bin)) { return BANKS[i].name; }
    }
    // Mã lạ (ví dụ ngân hàng của người chuyển không có trong danh sách) thì hiện nguyên mã.
    return bin ? 'Ngân hàng ' + bin : '';
  }

  // Điền các lựa chọn vào một thẻ <select>; giữ lại mã đang chọn kể cả khi nó không nằm trong danh sách.
  function fill(select, selected) {
    var current = selected ? String(selected) : '';
    var html = '<option value="">Chọn ngân hàng</option>';
    var found = false;
    BANKS.forEach(function (bank) {
      if (bank.bin === current) { found = true; }
      html += '<option value="' + bank.bin + '">' + bank.name + '</option>';
    });
    if (current && !found) {
      html += '<option value="' + current.replace(/[^0-9]/g, '') + '">' + name(current.replace(/[^0-9]/g, '')) + '</option>';
    }
    select.innerHTML = html;
    select.value = current.replace(/[^0-9]/g, '');
  }

  window.OldBookBanks = { list: BANKS, name: name, fill: fill };
})();
