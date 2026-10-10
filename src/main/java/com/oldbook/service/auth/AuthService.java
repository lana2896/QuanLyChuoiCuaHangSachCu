package com.oldbook.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oldbook.constant.auth.AccountStatus;
import com.oldbook.constant.auth.Role;
import com.oldbook.dto.auth.*;
import com.oldbook.entity.auth.VerificationCode;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.identity.Account;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.auth.VerificationCodeRepository;
import com.oldbook.repository.identity.UserRepository;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.service.auth.EmailService;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationCodeRepository verificationCodeRepository;
    private static final String LOAI_XAC_THUC_DANG_KY = "DANG_KY";
    private final EmailService emailService;
    private static final String LOAI_XAC_THUC_QUEN_MAT_KHAU = "QUEN_MAT_KHAU";
    private static final long OTP_RESEND_COOLDOWN_SECONDS = 60;

    private final RevokedTokenService revokedTokenService;
    private final AccountTokenService accountTokenService;

    @Transactional
    public void register(RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());
        String soDienThoai = normalizePhone(request.getSoDienThoai());

        // 1. Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(email)) {
            User nguoiDungTonTai = userRepository
                    .findByEmail(email)
                    .orElseThrow(() ->
                            new BusinessException("Không thể tìm thấy người dùng với email này")
                    );

            Optional<Account> taiKhoanKhachHang =
                    accountRepository.findByNguoiDung_MaNDAndVaiTro(
                            nguoiDungTonTai.getMaND(),
                            Role.KHACH_HANG.name()
                    );

            if (taiKhoanKhachHang.isPresent()
                    && AccountStatus.CHO_XAC_THUC.name()
                    .equals(taiKhoanKhachHang.get().getTrangThai())) {
                throw new BusinessException(
                        "Email này đã đăng ký nhưng chưa xác thực. Vui lòng tiếp tục xác thực tài khoản."
                );
            }

            throw new BusinessException("Email đã được sử dụng");
        }

        // 2. Kiểm tra số điện thoại nếu người dùng có nhập
        if (soDienThoai != null
                && userRepository.existsBySoDienThoai(soDienThoai)) {

            throw new BusinessException(
                    "Số điện thoại đã được sử dụng"
            );
        }

        // 3. Mã hóa mật khẩu bằng BCrypt
        String matKhauMaHoa =
                passwordEncoder.encode(request.getMatKhau());

        // 4. Tạo User
        User nguoiDung = User.builder()
                .email(email)
                .matKhau(matKhauMaHoa)
                .hoTen(request.getHoTen().trim())
                .soDienThoai(soDienThoai)
                .build();

        userRepository.save(nguoiDung);

        // 5. Tạo Account nhưng CHƯA cho hoạt động
        Account taiKhoan = Account.builder()
                .nguoiDung(nguoiDung)
                .vaiTro(Role.KHACH_HANG.name())
                .trangThai(AccountStatus.CHO_XAC_THUC.name())
                .build();

        accountRepository.save(taiKhoan);

        // 6. Tạo OTP 6 chữ số
        String maOtp = String.format(
                "%06d",
                ThreadLocalRandom.current().nextInt(1_000_000)
        );

        // 7. Tạo thời gian hết hạn = 5 phút
        LocalDateTime thoiGianHetHan =
                LocalDateTime.now().plusMinutes(5);

        // 8. Lưu OTP
        VerificationCode maXacThuc = VerificationCode.builder()
                .nguoiDung(nguoiDung)
                .maOtp(maOtp)
                .loaiXacThuc(LOAI_XAC_THUC_DANG_KY)
                .thoiGianHetHan(thoiGianHetHan)
                .daSuDung(false)
                .build();

        verificationCodeRepository.save(maXacThuc);
        emailService.sendRegisterOtpEmail(email, maOtp);
    }

    @Transactional
    public void resetPassword(
            ResetPasswordRequest request
    ) {

        String email =
                normalizeEmail(
                        request.getEmail()
                );


        User nguoiDung =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Email không tồn tại"
                                )
                        );


        /*
         * Kiểm tra xác nhận mật khẩu.
         */
        if (!request.getMatKhauMoi()
                .equals(
                        request.getXacNhanMatKhauMoi()
                )) {

            throw new BusinessException(
                    "Xác nhận mật khẩu mới không khớp"
            );
        }


        /*
         * Lấy OTP mới nhất chưa sử dụng.
         */
        VerificationCode maXacThuc =
                verificationCodeRepository
                        .findTopByNguoiDung_MaNDAndLoaiXacThucAndDaSuDungFalseOrderByNgayTaoDesc(
                                nguoiDung.getMaND(),
                                LOAI_XAC_THUC_QUEN_MAT_KHAU
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Không tìm thấy mã OTP đặt lại mật khẩu"
                                )
                        );


        /*
         * Kiểm tra OTP hết hạn.
         */
        if (LocalDateTime.now()
                .isAfter(
                        maXacThuc.getThoiGianHetHan()
                )) {

            throw new BusinessException(
                    "Mã OTP đã hết hạn"
            );
        }


        /*
         * Kiểm tra OTP chính xác.
         */
        if (!maXacThuc.getMaOtp()
                .equals(
                        request.getMaOtp()
                )) {

            throw new BusinessException(
                    "Mã OTP không chính xác"
            );
        }


        /*
         * Mã hóa mật khẩu mới bằng BCrypt.
         */
        String matKhauMoiMaHoa =
                passwordEncoder.encode(
                        request.getMatKhauMoi()
                );


        nguoiDung.setMatKhau(
                matKhauMoiMaHoa
        );


        /*
         * OTP chỉ được dùng một lần.
         */
        maXacThuc.setDaSuDung(true);


        userRepository.save(
                nguoiDung
        );

        verificationCodeRepository.save(
                maXacThuc
        );


        /*
         * Vô hiệu tất cả JWT cũ của người dùng.
         */
        accountRepository
                .findByNguoiDung_MaND(
                        nguoiDung.getMaND()
                )
                .forEach(taiKhoan ->
                        accountTokenService
                                .invalidateAllTokens(
                                        taiKhoan.getMaTK()
                                )
                );
    }

    @Transactional
    public void resendForgotPasswordOtp(
            ResendForgotPasswordOtpRequest request
    ) {

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        User nguoiDung =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Email không tồn tại"
                                )
                        );


        sendForgotPasswordOtp(
                nguoiDung,
                email
        );
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        String email = normalizeEmail(request.getEmail());

        User nguoiDung = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException("Email không tồn tại")
                );

        sendForgotPasswordOtp(
                nguoiDung,
                email
        );
    }
    private void sendForgotPasswordOtp(
            User nguoiDung,
            String email
    ) {

        Optional<VerificationCode> otpHienTai =
                verificationCodeRepository
                        .findTopByNguoiDung_MaNDAndLoaiXacThucAndDaSuDungFalseOrderByNgayTaoDesc(
                                nguoiDung.getMaND(),
                                LOAI_XAC_THUC_QUEN_MAT_KHAU
                        );

        if (otpHienTai.isPresent()) {

            VerificationCode otp =
                    otpHienTai.get();

            LocalDateTime now =
                    LocalDateTime.now();

            if (otp.getNgayTao() != null
                    && now.isBefore(
                    otp.getNgayTao()
                            .plusSeconds(
                                    OTP_RESEND_COOLDOWN_SECONDS
                            )
            )) {

                throw new BusinessException(
                        "Vui lòng chờ 60 giây trước khi gửi lại OTP"
                );
            }

            otp.setDaSuDung(true);

            verificationCodeRepository.save(otp);
        }


        String maOtp =
                String.format(
                        "%06d",
                        ThreadLocalRandom
                                .current()
                                .nextInt(1_000_000)
                );

        LocalDateTime thoiGianHetHan =
                LocalDateTime
                        .now()
                        .plusMinutes(5);


        VerificationCode maXacThuc =
                VerificationCode.builder()
                        .nguoiDung(nguoiDung)
                        .maOtp(maOtp)
                        .loaiXacThuc(
                                LOAI_XAC_THUC_QUEN_MAT_KHAU
                        )
                        .thoiGianHetHan(
                                thoiGianHetHan
                        )
                        .daSuDung(false)
                        .build();


        verificationCodeRepository.save(
                maXacThuc
        );

        emailService.sendForgotPasswordOtpEmail(
                email,
                maOtp
        );
    }

    @Transactional
    public void resendRegisterOtp(ResendRegisterOtpRequest request) {

        String email = normalizeEmail(request.getEmail());

        // 1. Tìm người dùng
        User nguoiDung = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException("Email không tồn tại"));

        // 2. Tìm tài khoản khách hàng
        Account taiKhoan = accountRepository
                .findByNguoiDung_MaNDAndVaiTro(
                        nguoiDung.getMaND(),
                        Role.KHACH_HANG.name()
                )
                .orElseThrow(() ->
                        new BusinessException("Không tìm thấy tài khoản khách hàng"));

        // 3. Tài khoản đã xác thực rồi thì không được resend
        if (AccountStatus.HOAT_DONG.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản đã được xác thực, không cần gửi lại OTP"
            );
        }

        // 4. Chỉ tài khoản đang chờ xác thực mới được resend
        if (!AccountStatus.CHO_XAC_THUC.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản không ở trạng thái chờ xác thực"
            );
        }

        // 5. Lấy OTP đăng ký gần nhất chưa sử dụng
        Optional<VerificationCode> otpHienTai =
                verificationCodeRepository
                        .findTopByNguoiDung_MaNDAndLoaiXacThucAndDaSuDungFalseOrderByNgayTaoDesc(
                                nguoiDung.getMaND(),
                                LOAI_XAC_THUC_DANG_KY
                        );

        // 6. Cho phép resend sau 60 giây, không cần chờ OTP cũ hết hạn.
        if (otpHienTai.isPresent()) {

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime ngayCoTheGuiLai =
                    otpHienTai.get().getNgayTao().plusSeconds(60);

            if (now.isBefore(ngayCoTheGuiLai)) {
                long conLai =
                        java.time.Duration.between(now, ngayCoTheGuiLai)
                                .getSeconds() + 1;

                throw new BusinessException(
                        "Vui lòng chờ " + conLai + " giây trước khi gửi lại OTP"
                );
            }

            // OTP cũ không còn được chấp nhận sau khi resend.
            otpHienTai.get().setDaSuDung(true);
            verificationCodeRepository.save(otpHienTai.get());
        }

        // 7. Sinh OTP mới
        String maOtpMoi = String.format(
                "%06d",
                ThreadLocalRandom.current().nextInt(1_000_000)
        );

        // 8. OTP mới có hiệu lực 5 phút
        LocalDateTime thoiGianHetHan =
                LocalDateTime.now().plusMinutes(5);

        // 9. Lưu OTP mới
        VerificationCode maXacThucMoi = VerificationCode.builder()
                .nguoiDung(nguoiDung)
                .maOtp(maOtpMoi)
                .loaiXacThuc(LOAI_XAC_THUC_DANG_KY)
                .thoiGianHetHan(thoiGianHetHan)
                .daSuDung(false)
                .build();

        verificationCodeRepository.save(maXacThucMoi);

        // 10. Gửi OTP mới qua email
        emailService.sendRegisterOtpEmail(
                email,
                maOtpMoi
        );
    }

    @Transactional
    public void verifyRegisterOtp(
            VerifyRegisterOtpRequest request
    ) {

        String email = normalizeEmail(request.getEmail());

        // 1. Tìm người dùng
        User nguoiDung = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException(
                                "Email không tồn tại"
                        )
                );

        // 2. Tìm tài khoản Khách hàng
        Account taiKhoan = accountRepository
                .findByNguoiDung_MaNDAndVaiTro(
                        nguoiDung.getMaND(),
                        Role.KHACH_HANG.name()
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "Tài khoản không tồn tại"
                        )
                );

        // 3. Kiểm tra trạng thái tài khoản
        if (AccountStatus.HOAT_DONG.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản đã được xác thực"
            );
        }

        if (!AccountStatus.CHO_XAC_THUC.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản không ở trạng thái chờ xác thực"
            );
        }

        // 4. Lấy OTP mới nhất chưa sử dụng
        VerificationCode maXacThuc = verificationCodeRepository
                .findTopByNguoiDung_MaNDAndLoaiXacThucAndDaSuDungFalseOrderByNgayTaoDesc(
                        nguoiDung.getMaND(),
                        LOAI_XAC_THUC_DANG_KY
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "Không tìm thấy mã OTP"
                        )
                );

        // 5. Kiểm tra hết hạn
        if (LocalDateTime.now()
                .isAfter(maXacThuc.getThoiGianHetHan())) {

            throw new BusinessException(
                    "Mã OTP đã hết hạn"
            );
        }

        // 6. Kiểm tra mã OTP
        if (!maXacThuc.getMaOtp()
                .equals(request.getMaOtp())) {

            throw new BusinessException(
                    "Mã OTP không chính xác"
            );
        }

        // 7. Kích hoạt tài khoản
        taiKhoan.setTrangThai(
                AccountStatus.HOAT_DONG.name()
        );

        // 8. Đánh dấu OTP đã sử dụng
        maXacThuc.setDaSuDung(true);

        accountRepository.save(taiKhoan);
        verificationCodeRepository.save(maXacThuc);
    }
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        // 1. Tìm User theo email
        User nguoiDung = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException(
                                "Email hoặc mật khẩu không chính xác"
                        )
                );

        // 2. Kiểm tra mật khẩu bằng BCrypt
        boolean matKhauDung = passwordEncoder.matches(
                request.getMatKhau(),
                nguoiDung.getMatKhau()
        );

        if (!matKhauDung) {
            throw new BusinessException(
                    "Email hoặc mật khẩu không chính xác"
            );
        }

        // 3. Lấy các tài khoản đang hoạt động
        List<Account> taiKhoans =
                accountRepository
                        .findByNguoiDung_MaNDAndTrangThai(
                                nguoiDung.getMaND(),
                                AccountStatus.HOAT_DONG.name()
                        );

        if (taiKhoans.isEmpty()) {
            boolean dangChoXacThuc = accountRepository
                    .findByNguoiDung_MaNDAndVaiTro(
                            nguoiDung.getMaND(),
                            Role.KHACH_HANG.name()
                    )
                    .map(tk -> AccountStatus.CHO_XAC_THUC.name()
                            .equals(tk.getTrangThai()))
                    .orElse(false);

            if (dangChoXacThuc) {
                throw new BusinessException(
                        "Tài khoản chưa được xác thực. Vui lòng xác thực email trước khi đăng nhập."
                );
            }

            throw new BusinessException(
                    "Người dùng không có tài khoản đang hoạt động"
            );
        }

        // 4. Có đúng 1 tài khoản → đăng nhập thẳng
        if (taiKhoans.size() == 1) {

            Account taiKhoan = taiKhoans.get(0);

            String token = jwtService.generateToken(
                    nguoiDung.getMaND(),
                    taiKhoan.getMaTK(),
                    taiKhoan.getVaiTro()
            );

            return LoginResponse.builder()
                    .maND(nguoiDung.getMaND())
                    .maTK(taiKhoan.getMaTK())
                    .vaiTro(taiKhoan.getVaiTro())
                    .token(token)
                    .roleSelectionToken(null)
                    .canChonVaiTro(false)
                    .taiKhoans(toAccountResponse(taiKhoans))
                    .build();
        }

        // 5. Có nhiều tài khoản → cho chọn vai trò
        String roleSelectionToken =
                jwtService.generateRoleSelectionToken(
                        nguoiDung.getMaND()
                );

        return LoginResponse.builder()
                .maND(nguoiDung.getMaND())
                .maTK(null)
                .vaiTro(null)
                .token(null)
                .roleSelectionToken(roleSelectionToken)
                .canChonVaiTro(true)
                .taiKhoans(toAccountResponse(taiKhoans))
                .build();
    }

    @Transactional
    public void logout(String token) {

        if (!jwtService.isTokenValid(token)) {
            throw new BusinessException(
                    "Token không hợp lệ hoặc đã hết hạn"
            );
        }

        String jti = jwtService.extractJti(token);

        if (jti == null) {
            throw new BusinessException(
                    "JWT không có mã định danh"
            );
        }

        Date expiration = jwtService.extractExpiration(token);

        revokedTokenService.revokeToken(
                jti,
                expiration.toInstant()
        );
    }

    @Transactional
    public void changePassword(
            Integer maND,
            ChangePasswordRequest request
    ) {

        // 1. Tìm người dùng
        User nguoiDung = userRepository.findById(maND)
                .orElseThrow(() ->
                        new BusinessException("Người dùng không tồn tại")
                );

        // 2. Kiểm tra mật khẩu hiện tại
        boolean matKhauDung = passwordEncoder.matches(
                request.getMatKhauHienTai(),
                nguoiDung.getMatKhau()
        );

        if (!matKhauDung) {
            throw new BusinessException(
                    "Mật khẩu hiện tại không chính xác"
            );
        }

        // 3. Kiểm tra xác nhận mật khẩu mới
        if (!request.getMatKhauMoi()
                .equals(request.getXacNhanMatKhauMoi())) {

            throw new BusinessException(
                    "Xác nhận mật khẩu mới không khớp"
            );
        }

        // 4. Mã hóa mật khẩu mới bằng BCrypt
        String matKhauMoiMaHoa = passwordEncoder.encode(
                request.getMatKhauMoi()
        );

        // 5. Cập nhật mật khẩu
        nguoiDung.setMatKhau(matKhauMoiMaHoa);

        userRepository.save(nguoiDung);
    }

    private List<LoginResponse.AccountResponse> toAccountResponse(
            List<Account> taiKhoans
    ) {
        return taiKhoans.stream()
                .map(taiKhoan ->
                        LoginResponse.AccountResponse.builder()
                                .maTK(taiKhoan.getMaTK())
                                .vaiTro(taiKhoan.getVaiTro())
                                .trangThai(taiKhoan.getTrangThai())
                                .build()
                )
                .toList();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String soDienThoai) {

        if (soDienThoai == null) {
            return null;
        }

        String phone = soDienThoai.trim();

        if (phone.isEmpty()) {
            return null;
        }

        return phone;
    }

    @Transactional(readOnly = true)
    public LoginResponse selectRole(
            String roleSelectionToken,
            Integer maTK
    ) {

        // 1. Kiểm tra JWT tạm
        if (!jwtService.isTokenValid(roleSelectionToken)) {
            throw new BusinessException(
                    "Phiên chọn vai trò đã hết hạn"
            );
        }

        // 2. Token phải là token dùng cho chọn vai trò
        if (!jwtService.isRoleSelectionToken(roleSelectionToken)) {
            throw new BusinessException(
                    "Token không hợp lệ"
            );
        }

        // 3. Lấy maND từ token
        Integer maND = jwtService.extractUserId(
                roleSelectionToken
        );

        // 4. Tìm Account
        Account taiKhoan = accountRepository.findById(maTK)
                .orElseThrow(() ->
                        new BusinessException(
                                "Tài khoản không tồn tại"
                        )
                );

        // 5. Kiểm tra Account có thuộc User này không
        if (!taiKhoan.getNguoiDung()
                .getMaND()
                .equals(maND)) {

            throw new BusinessException(
                    "Tài khoản không thuộc người dùng này"
            );
        }

        // 6. Kiểm tra trạng thái
        if (!AccountStatus.HOAT_DONG.name().equals(
                taiKhoan.getTrangThai()
        )) {

            throw new BusinessException(
                    "Tài khoản đang bị khóa hoặc không hoạt động"
            );
        }

        // 7. Tạo JWT chính thức
        String token = jwtService.generateToken(
                maND,
                taiKhoan.getMaTK(),
                taiKhoan.getVaiTro()
        );

        return LoginResponse.builder()
                .maND(maND)
                .maTK(taiKhoan.getMaTK())
                .vaiTro(taiKhoan.getVaiTro())
                .token(token)
                .roleSelectionToken(null)
                .canChonVaiTro(false)
                .taiKhoans(
                        toAccountResponse(
                                List.of(taiKhoan)
                        )
                )
                .build();
    }
}