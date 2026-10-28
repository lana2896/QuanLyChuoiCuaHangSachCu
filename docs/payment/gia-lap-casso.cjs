// Giả lập Casso báo "tiền đã về" khi thử thanh toán chuyển khoản trên máy local.
// Casso thật chỉ gọi được địa chỉ HTTPS công khai, nên trên máy mình script này đóng vai Casso:
// nó gửi một yêu cầu có chữ ký hợp lệ vào webhook của ứng dụng đang chạy.
//
// Chạy từ thư mục gốc của dự án (nơi có file .env), cần Node.js 18+:
//   node docs/payment/gia-lap-casso.cjs <mã thanh toán> <số tiền> [cổng]
// Ví dụ:
//   node docs/payment/gia-lap-casso.cjs OB7K2M9QXA 275000
//
// Khóa ký lấy từ PAYMENT_CASSO_CHECKSUM_KEY trong .env (hoặc biến môi trường cùng tên).
// Cổng mặc định lấy từ SERVER_PORT trong .env, không có thì dùng 8080.

const crypto = require('crypto');
const fs = require('fs');

function docEnv() {
  const env = {};
  if (!fs.existsSync('.env')) { return env; }
  for (const dong of fs.readFileSync('.env', 'utf8').split(/\r?\n/)) {
    const khop = /^\s*([A-Za-z0-9_.-]+)\s*=(.*)$/.exec(dong);
    if (khop) { env[khop[1]] = khop[2].trim(); }
  }
  return env;
}

const env = docEnv();
const giaTri = (...khoa) => khoa.map(k => env[k] || process.env[k]).find(Boolean);
const [ma, soTien, cong = giaTri('SERVER_PORT') || '8080'] = process.argv.slice(2);
const khoaKy = giaTri('PAYMENT_CASSO_CHECKSUM_KEY', 'payment.casso.checksum-key');

if (!ma || !/^\d+$/.test(soTien || '')) {
  console.error('Cách dùng: node docs/payment/gia-lap-casso.cjs <mã thanh toán> <số tiền> [cổng]');
  process.exit(1);
}
if (!khoaKy) {
  console.error('Chưa có PAYMENT_CASSO_CHECKSUM_KEY trong .env (hãy chạy lệnh từ thư mục gốc của dự án).');
  process.exit(1);
}

// Mỗi lần chạy là một giao dịch mới; gửi lại đúng một giao dịch thì ứng dụng sẽ bỏ qua.
const id = Date.now() % 1000000000;
const noiDung = JSON.stringify({
  error: 0,
  data: {
    id,
    reference: 'FT' + id,
    description: 'MBVCB.' + id + '. ' + ma + ' thanh toan don sach cu',
    amount: Number(soTien),
    runningBalance: 25000000,
    transactionDateTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
    accountNumber: giaTri('PAYMENT_BANK_ACCOUNT_NO') || '',
    bankName: 'MBBank',
    bankAbbreviation: 'MB',
    virtualAccountNumber: '',
    virtualAccountName: '',
    counterAccountName: 'NGUYEN VAN KHACH',
    counterAccountNumber: '8888888888',
    counterAccountBankId: '970415',
    counterAccountBankName: 'VietinBank'
  }
});

// Chữ ký Webhook V2 của Casso: HMAC-SHA512 trên "<thời gian>.<JSON đã sắp khóa theo thứ tự chữ cái>".
const sapXep = v => v && typeof v === 'object' && !Array.isArray(v)
  ? Object.fromEntries(Object.keys(v).sort().map(k => [k, sapXep(v[k])]))
  : v;
const thoiGian = Date.now();
const chuKy = crypto.createHmac('sha512', khoaKy)
  .update(thoiGian + '.' + JSON.stringify(sapXep(JSON.parse(noiDung))))
  .digest('hex');

fetch('http://localhost:' + cong + '/api/payment/casso/webhook', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'X-Casso-Signature': 't=' + thoiGian + ',v1=' + chuKy },
  body: noiDung
}).then(async phanHoi => {
  console.log('HTTP ' + phanHoi.status + ' ' + await phanHoi.text());
  if (phanHoi.status === 200) {
    console.log('Ứng dụng đã nhận giao dịch ' + id + '. Xem kết quả ở trang quét mã, hoặc ở Quản lý > Thanh toán > Giao dịch ngân hàng.');
  } else if (phanHoi.status === 401) {
    console.log('Sai chữ ký: khóa trong .env khác khóa ứng dụng đang dùng. Khởi động lại ứng dụng sau khi sửa .env.');
  }
}, loi => {
  console.error('Không gọi được ứng dụng ở cổng ' + cong + ': ' + loi.message);
  process.exit(1);
});
