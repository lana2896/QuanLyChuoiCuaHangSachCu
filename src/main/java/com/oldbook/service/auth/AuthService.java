package com.oldbook.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oldbook.constant.auth.TrangThaiTaiKhoan;
import com.oldbook.constant.auth.VaiTro;
import com.oldbook.dto.auth.*;
import com.oldbook.entity.auth.MaXacThuc;
import com.oldbook.entity.identity.NguoiDung;
import com.oldbook.entity.identity.TaiKhoan;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.auth.MaXacThucRepository;
import com.oldbook.repository.identity.NguoiDungRepository;
import com.oldbook.repository.identity.TaiKhoanRepository;
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
    private final NguoiDungRepository nguoiDungRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MaXacThucRepository maXacThucRepository;
    private static final String LOAI_XAC_THUC_DANG_KY = "DANG_KY";
    private final EmailService emailService;
    private static final String LOAI_XAC_THUC_QUEN_MAT_KHAU = "QUEN_MAT_KHAU";
    private final RevokedTokenService revokedTokenService;

    @Transactional
    public void register(RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());
        String soDienThoai = normalizePhone(request.getSoDienThoai());

        // 1. Kiểm tra email đã tồn tại
        if (nguoiDungRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng");
        }

        // 2. Kiểm tra số điện thoại nếu người dùng có nhập
        if (soDienThoai != null
                && nguoiDungRepository.existsBySoDienThoai(soDienThoai)) {

            throw new BusinessException(
                    "Số điện thoại đã được sử dụng"
            );
        }

        // 3. Mã hóa mật khẩu bằng BCrypt
        String matKhauMaHoa =
                passwordEncoder.encode(request.getMatKhau());

        // 4. Tạo NguoiDung
        NguoiDung nguoiDung = NguoiDung.builder()
                .email(email)
                .matKhau(matKhauMaHoa)
                .hoTen(request.getHoTen().trim())
                .soDienThoai(soDienThoai)
                .build();

        nguoiDungRepository.save(nguoiDung);

        // 5. Tạo TaiKhoan nhưng CHƯA cho hoạt động
        TaiKhoan taiKhoan = TaiKhoan.builder()
                .nguoiDung(nguoiDung)
                .vaiTro(VaiTro.KHACH_HANG.name())
                .trangThai(TrangThaiTaiKhoan.CHO_XAC_THUC.name())
                .build();

        taiKhoanRepository.save(taiKhoan);

        // 6. Tạo OTP 6 chữ số
        String maOtp = String.format(
                "%06d",
                ThreadLocalRandom.current().nextInt(1_000_000)
        );

        // 7. Tạo thời gian hết hạn = 5 phút
        LocalDateTime thoiGianHetHan =
                LocalDateTime.now().plusMinutes(5);

        // 8. Lưu OTP
        MaXacThuc maXacThuc = MaXacThuc.builder()
                .nguoiDung(nguoiDung)
                .maOtp(maOtp)
                .loaiXacThuc(LOAI_XAC_THUC_DANG_KY)
                .thoiGianHetHan(thoiGianHetHan)
                .daSuDung(false)
                .build();

        maXacThucRepository.save(maXacThuc);
        emailService.sendRegisterOtpEmail(email, maOtp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        String email = normalizeEmail(request.getEmail());

        NguoiDung nguoiDung = nguoiDungRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException("Email không tồn tại")
                );

        if (!request.getMatKhauMoi()
                .equals(request.getXacNhanMatKhauMoi())) {

            throw new BusinessException(
                    "Xác nhận mật khẩu mới không khớp"
            );
        }

        MaXacThuc maXacThuc = maXacThucRepository
                .findTopByNguoiDung_MaNDAndLoaiXacThucAndDaSuDungFalseOrderByNgayTaoDesc(
                        nguoiDung.getMaND(),
                        LOAI_XAC_THUC_QUEN_MAT_KHAU
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "Không tìm thấy mã OTP đặt lại mật khẩu"
                        )
                );

        if (LocalDateTime.now()
                .isAfter(maXacThuc.getThoiGianHetHan())) {

            throw new BusinessException(
                    "Mã OTP đã hết hạn"
            );
        }

        if (!maXacThuc.getMaOtp()
                .equals(request.getMaOtp())) {

            throw new BusinessException(
                    "Mã OTP không chính xác"
            );
        }

        String matKhauMoiMaHoa =
                passwordEncoder.encode(
                        request.getMatKhauMoi()
                );

        nguoiDung.setMatKhau(matKhauMoiMaHoa);
        maXacThuc.setDaSuDung(true);

        nguoiDungRepository.save(nguoiDung);
        maXacThucRepository.save(maXacThuc);
    }

    @Transactional
    public void resendForgotPasswordOtp(
            ResendForgotPasswordOtpRequest request
    ) {

        String email = normalizeEmail(request.getEmail());

        NguoiDung nguoiDung = nguoiDungRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException("Email không tồn tại")
                );

        Optional<MaXacThuc> otpHienTai =
                maXacThucRepository
                        .findTopByNguoiDung_MaNDAndLoaiXacThucAndDaSuDungFalseOrderByNgayTaoDesc(
                                nguoiDung.getMaND(),
                                LOAI_XAC_THUC_QUEN_MAT_KHAU
                        );

        if (otpHienTai.isPresent()) {

            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(
                    otpHienTai.get().getThoiGianHetHan()
            )) {
                throw new BusinessException(
                        "Mã OTP hiện tại vẫn còn hiệu lực, vui lòng chờ hết hạn"
                );
            }

            otpHienTai.get().setDaSuDung(true);
            maXacThucRepository.save(otpHienTai.get());
        }

        String maOtpMoi = String.format(
                "%06d",
                ThreadLocalRandom.current().nextInt(1_000_000)
        );

        LocalDateTime thoiGianHetHan =
                LocalDateTime.now().plusMinutes(5);

        MaXacThuc maXacThucMoi = MaXacThuc.builder()
                .nguoiDung(nguoiDung)
                .maOtp(maOtpMoi)
                .loaiXacThuc(LOAI_XAC_THUC_QUEN_MAT_KHAU)
                .thoiGianHetHan(thoiGianHetHan)
                .daSuDung(false)
                .build();

        maXacThucRepository.save(maXacThucMoi);

        emailService.sendForgotPasswordOtpEmail(
                email,
                maOtpMoi
        );
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        String email = normalizeEmail(request.getEmail());

        NguoiDung nguoiDung = nguoiDungRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException("Email không tồn tại")
                );

        String maOtp = String.format(
                "%06d",
                ThreadLocalRandom.current().nextInt(1_000_000)
        );

        LocalDateTime thoiGianHetHan =
                LocalDateTime.now().plusMinutes(5);

        MaXacThuc maXacThuc = MaXacThuc.builder()
                .nguoiDung(nguoiDung)
                .maOtp(maOtp)
                .loaiXacThuc(LOAI_XAC_THUC_QUEN_MAT_KHAU)
                .thoiGianHetHan(thoiGianHetHan)
                .daSuDung(false)
                .build();

        maXacThucRepository.save(maXacThuc);

        emailService.sendForgotPasswordOtpEmail(
                email,
                maOtp
        );
    }

    @Transactional
    public void resendRegisterOtp(ResendRegisterOtpRequest request) {

        String email = normalizeEmail(request.getEmail());

        // 1. Tìm người dùng
        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException("Email không tồn tại"));

        // 2. Tìm tài khoản khách hàng
        TaiKhoan taiKhoan = taiKhoanRepository
                .findByNguoiDung_MaNDAndVaiTro(
                        nguoiDung.getMaND(),
                        VaiTro.KHACH_HANG.name()
                )
                .orElseThrow(() ->
                        new BusinessException("Không tìm thấy tài khoản khách hàng"));

        // 3. Tài khoản đã xác thực rồi thì không được resend
        if (TrangThaiTaiKhoan.HOAT_DONG.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản đã được xác thực, không cần gửi lại OTP"
            );
        }

        // 4. Chỉ tài khoản đang chờ xác thực mới được resend
        if (!TrangThaiTaiKhoan.CHO_XAC_THUC.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản không ở trạng thái chờ xác thực"
            );
        }

        // 5. Lấy OTP đăng ký gần nhất chưa sử dụng
        Optional<MaXacThuc> otpHienTai =
                maXacThucRepository
                        .findTopByNguoiDung_MaNDAndLoaiXacThucAndDaSuDungFalseOrderByNgayTaoDesc(
                                nguoiDung.getMaND(),
                                LOAI_XAC_THUC_DANG_KY
                        );

        // 6. Nếu OTP hiện tại vẫn còn hạn thì không cho resend
        if (otpHienTai.isPresent()) {

            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(otpHienTai.get().getThoiGianHetHan())) {

                throw new BusinessException(
                        "Mã OTP hiện tại vẫn còn hiệu lực, vui lòng chờ hết hạn"
                );
            }

            // OTP cũ đã hết hạn → đánh dấu đã sử dụng
            otpHienTai.get().setDaSuDung(true);
            maXacThucRepository.save(otpHienTai.get());
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
        MaXacThuc maXacThucMoi = MaXacThuc.builder()
                .nguoiDung(nguoiDung)
                .maOtp(maOtpMoi)
                .loaiXacThuc(LOAI_XAC_THUC_DANG_KY)
                .thoiGianHetHan(thoiGianHetHan)
                .daSuDung(false)
                .build();

        maXacThucRepository.save(maXacThucMoi);

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
        NguoiDung nguoiDung = nguoiDungRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException(
                                "Email không tồn tại"
                        )
                );

        // 2. Tìm tài khoản Khách hàng
        TaiKhoan taiKhoan = taiKhoanRepository
                .findByNguoiDung_MaNDAndVaiTro(
                        nguoiDung.getMaND(),
                        VaiTro.KHACH_HANG.name()
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "Tài khoản không tồn tại"
                        )
                );

        // 3. Kiểm tra trạng thái tài khoản
        if (TrangThaiTaiKhoan.HOAT_DONG.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản đã được xác thực"
            );
        }

        if (!TrangThaiTaiKhoan.CHO_XAC_THUC.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản không ở trạng thái chờ xác thực"
            );
        }

        // 4. Lấy OTP mới nhất chưa sử dụng
        MaXacThuc maXacThuc = maXacThucRepository
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
                TrangThaiTaiKhoan.HOAT_DONG.name()
        );

        // 8. Đánh dấu OTP đã sử dụng
        maXacThuc.setDaSuDung(true);

        taiKhoanRepository.save(taiKhoan);
        maXacThucRepository.save(maXacThuc);
    }
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        // 1. Tìm NguoiDung theo email
        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
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
        List<TaiKhoan> taiKhoans =
                taiKhoanRepository
                        .findByNguoiDung_MaNDAndTrangThai(
                                nguoiDung.getMaND(),
                                TrangThaiTaiKhoan.HOAT_DONG.name()
                        );

        if (taiKhoans.isEmpty()) {
            throw new BusinessException(
                    "Người dùng không có tài khoản đang hoạt động"
            );
        }

        // 4. Có đúng 1 tài khoản → đăng nhập thẳng
        if (taiKhoans.size() == 1) {

            TaiKhoan taiKhoan = taiKhoans.get(0);

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
                    .taiKhoans(toTaiKhoanResponse(taiKhoans))
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
                .taiKhoans(toTaiKhoanResponse(taiKhoans))
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
        NguoiDung nguoiDung = nguoiDungRepository.findById(maND)
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

        nguoiDungRepository.save(nguoiDung);
    }

    private List<LoginResponse.TaiKhoanResponse> toTaiKhoanResponse(
            List<TaiKhoan> taiKhoans
    ) {
        return taiKhoans.stream()
                .map(taiKhoan ->
                        LoginResponse.TaiKhoanResponse.builder()
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
        Integer maND = jwtService.extractMaND(
                roleSelectionToken
        );

        // 4. Tìm TaiKhoan
        TaiKhoan taiKhoan = taiKhoanRepository.findById(maTK)
                .orElseThrow(() ->
                        new BusinessException(
                                "Tài khoản không tồn tại"
                        )
                );

        // 5. Kiểm tra TaiKhoan có thuộc NguoiDung này không
        if (!taiKhoan.getNguoiDung()
                .getMaND()
                .equals(maND)) {

            throw new BusinessException(
                    "Tài khoản không thuộc người dùng này"
            );
        }

        // 6. Kiểm tra trạng thái
        if (!TrangThaiTaiKhoan.HOAT_DONG.name().equals(
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
                        toTaiKhoanResponse(
                                List.of(taiKhoan)
                        )
                )
                .build();
    }
}