package com.oldbook.service.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oldbook.constant.auth.AccountStatus;
import com.oldbook.constant.auth.Role;
import com.oldbook.dto.identity.AdminAccountResponse;
import com.oldbook.dto.identity.AddRoleRequest;
import com.oldbook.dto.identity.CreateAccountRequest;
import com.oldbook.dto.identity.LockAccountRequest;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.identity.Account;
import com.oldbook.entity.system.SystemLog;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.identity.UserRepository;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.repository.system.SystemLogRepository;
import com.oldbook.service.admin.AdminNotificationService;
import com.oldbook.service.auth.EmailService;
import com.oldbook.service.auth.AccountTokenService;
import com.oldbook.service.notification.BusinessNotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SystemLogRepository systemLogRepository;

    private final PasswordEncoder passwordEncoder;
    private final AccountTokenService accountTokenService;

    private final EmailService emailService;

    private final AdminNotificationService adminNotificationService;
    private final BusinessNotificationService businessNotificationService;

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> getAll(
            String tuKhoa,
            String vaiTro,
            String trangThai
    ) {

        String keyword =
                tuKhoa == null
                        ? ""
                        : tuKhoa.trim().toLowerCase(Locale.ROOT);

        String roleFilter =
                vaiTro == null
                        ? ""
                        : vaiTro.trim();

        String statusFilter =
                trangThai == null
                        ? ""
                        : trangThai.trim();

        return accountRepository.findAll()
                .stream()
                .filter(tk -> matchesKeyword(tk, keyword))
                .filter(tk ->
                        roleFilter.isBlank()
                                || roleFilter.equals(
                                tk.getVaiTro()
                        )
                )
                .filter(tk ->
                        statusFilter.isBlank()
                                || statusFilter.equals(
                                tk.getTrangThai()
                        )
                )
                .map(this::toResponse)
                .toList();
    }

    private boolean matchesKeyword(
            Account taiKhoan,
            String keyword
    ) {

        if (keyword.isBlank()) {
            return true;
        }

        User nd = taiKhoan.getNguoiDung();

        return contains(nd.getEmail(), keyword)
                || contains(nd.getHoTen(), keyword)
                || contains(
                nd.getSoDienThoai(),
                keyword
        )
                || String.valueOf(
                taiKhoan.getMaTK()
        ).contains(keyword)
                || String.valueOf(
                nd.getMaND()
        ).contains(keyword);
    }

    private boolean contains(
            String value,
            String keyword
    ) {

        return value != null
                && value.toLowerCase(
                Locale.ROOT
        ).contains(keyword);
    }

    @Transactional
    public AdminAccountResponse create(
            CreateAccountRequest request,
            Integer maTKAdmin,
            String ip
    ) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        String soDienThoai =
                normalizePhone(
                        request.getSoDienThoai()
                );

        Role vaiTro = parseRole(
                request.getVaiTro()
        );

        // Email không được trùng
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(
                    "Email đã được sử dụng"
            );
        }

        // Số điện thoại nếu có nhập không được trùng
        if (soDienThoai != null
                && userRepository
                .existsBySoDienThoai(soDienThoai)) {

            throw new BusinessException(
                    "Số điện thoại đã được sử dụng"
            );
        }

        User nguoiDung =
                User.builder()
                        .email(email)
                        .matKhau(
                                passwordEncoder.encode(
                                        request.getMatKhau()
                                )
                        )
                        .hoTen(
                                request.getHoTen().trim()
                        )
                        .soDienThoai(soDienThoai)
                        .build();

        nguoiDung =
                userRepository.save(
                        nguoiDung
                );

        Account taiKhoan =
                Account.builder()
                        .nguoiDung(nguoiDung)
                        .vaiTro(vaiTro.name())
                        .trangThai(
                                AccountStatus
                                        .HOAT_DONG.name()
                        )
                        .build();

        taiKhoan =
                accountRepository.save(
                        taiKhoan
                );

        writeSystemLog(
                maTKAdmin,
                "TAO_TAI_KHOAN",
                taiKhoan.getMaTK(),
                "Tạo tài khoản mới với vai trò "
                        + vaiTro.name(),
                ip
        );

        adminNotificationService.accountCreated(maTKAdmin, taiKhoan);

        emailService.sendAdminAccountCreatedEmail(
                nguoiDung.getEmail(),
                nguoiDung.getHoTen(),
                vaiTro.name()
        );


        return toResponse(taiKhoan);
    }

    @Transactional
    public void lock(
            Integer maTKTarget,
            LockAccountRequest request,
            Integer maTKAdmin,
            String ip
    ) {

        // Không được tự khóa
        if (maTKTarget.equals(maTKAdmin)) {
            throw new BusinessException(
                    "Hệ thống không cho phép Quản trị viên tự khóa tài khoản của chính mình"
            );
        }

        Account taiKhoan =
        		findAccount(maTKTarget);

        if (AccountStatus.BI_KHOA.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản đã bị khóa"
            );
        }

        taiKhoan.setTrangThai(
                AccountStatus.BI_KHOA.name()
        );

        accountRepository.save(taiKhoan);

        // Vô hiệu mọi JWT đang tồn tại
        accountTokenService.invalidateAllTokens(
                maTKTarget
        );

        writeSystemLog(
                maTKAdmin,
                "KHOA_TAI_KHOAN",
                maTKTarget,
                "Khóa tài khoản. Lý do: "
                        + request.getLyDo().trim(),
                ip
        );

        adminNotificationService.accountLocked(maTKAdmin, taiKhoan, request.getLyDo().trim());

        emailService.sendAccountLockedEmail(
                taiKhoan.getNguoiDung().getEmail(),
                request.getLyDo().trim()
        );
    }

    @Transactional
    public void unlock(
            Integer maTKTarget,
            Integer maTKAdmin,
            String ip
    ) {

        Account taiKhoan =
        		findAccount(maTKTarget);

        if (!AccountStatus.BI_KHOA.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản hiện không bị khóa"
            );
        }

        taiKhoan.setTrangThai(
                AccountStatus.HOAT_DONG.name()
        );

        accountRepository.save(taiKhoan);

        // Không xóa invalidBefore.
        // Token cũ vẫn không được phép sống lại.
        writeSystemLog(
                maTKAdmin,
                "MO_KHOA_TAI_KHOAN",
                maTKTarget,
                "Mở khóa tài khoản",
                ip
        );

        adminNotificationService.accountUnlocked(maTKAdmin, taiKhoan);

        emailService.sendAccountUnlockedEmail(
                taiKhoan.getNguoiDung().getEmail()
        );
    }

    /**
     * Thêm một vai trò cho người dùng sở hữu tài khoản maTKTarget.
     * Mỗi vai trò là một tài khoản riêng của cùng người dùng, nên tài khoản đang có được giữ nguyên:
     * giỏ hàng, đơn hàng hay cửa hàng gắn với nó không bị ảnh hưởng.
     */
    @Transactional
    public AdminAccountResponse addRole(
            Integer maTKTarget,
            AddRoleRequest request,
            Integer maTKAdmin,
            String ip
    ) {

        Account taiKhoanGoc =
                findAccount(maTKTarget);

        Role vaiTroMoi =
                parseRole(request.getVaiTro());

        // Chưa xác thực email thì chưa được có tài khoản hoạt động nào:
        // nếu không, thêm vai trò sẽ thành đường vòng bỏ qua bước xác thực.
        if (AccountStatus.CHO_XAC_THUC.name()
                .equals(taiKhoanGoc.getTrangThai())) {

            throw new BusinessException(
                    "Người dùng chưa xác thực email nên chưa thể thêm vai trò"
            );
        }

        User nguoiDung =
                taiKhoanGoc.getNguoiDung();

        if (accountRepository
                .existsByNguoiDung_MaNDAndVaiTro(
                        nguoiDung.getMaND(),
                        vaiTroMoi.name()
                )) {

            throw new BusinessException(
                    "Người dùng đã có tài khoản với vai trò "
                            + vaiTroMoi.getDisplayName()
            );
        }

        Account taiKhoanMoi =
                accountRepository.save(
                        Account.builder()
                                .nguoiDung(nguoiDung)
                                .vaiTro(vaiTroMoi.name())
                                .trangThai(
                                        AccountStatus
                                                .HOAT_DONG.name()
                                )
                                .build()
                );

        writeSystemLog(
                maTKAdmin,
                "THEM_VAI_TRO",
                taiKhoanMoi.getMaTK(),
                "Thêm vai trò "
                        + vaiTroMoi.name()
                        + " cho người dùng đang có tài khoản #"
                        + maTKTarget,
                ip
        );

        adminNotificationService.roleAdded(maTKAdmin, taiKhoanMoi);
        businessNotificationService.roleAdded(taiKhoanMoi);

        return toResponse(taiKhoanMoi);
    }

    private Account findAccount(
            Integer maTK
    ) {

        return accountRepository.findById(maTK)
                .orElseThrow(() ->
                        new BusinessException(
                                "Tài khoản không tồn tại"
                        )
                );
    }

    private Role parseRole(
            String vaiTro
    ) {

        Role role;
        try {
            role = Role.valueOf(
                    vaiTro.trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (Exception e) {

            throw new BusinessException(
                    "Vai trò không hợp lệ"
            );
        }

        // Đơn vị vận chuyển không còn dùng tài khoản: truy cập bằng link /shipping/{maCode}.
        if (role == Role.DON_VI_VAN_CHUYEN) {
            throw new BusinessException(
                    "Đơn vị vận chuyển không dùng tài khoản. Hãy thêm đối tác ở mục quản lý đơn vị vận chuyển."
            );
        }

        return role;
    }

    private String normalizePhone(
            String soDienThoai
    ) {

        if (soDienThoai == null) {
            return null;
        }

        String phone =
                soDienThoai.trim();

        return phone.isBlank()
                ? null
                : phone;
    }

    private AdminAccountResponse toResponse(
            Account taiKhoan
    ) {

        User nd =
                taiKhoan.getNguoiDung();

        return AdminAccountResponse.builder()
                .maTK(taiKhoan.getMaTK())
                .maND(nd.getMaND())
                .email(nd.getEmail())
                .hoTen(nd.getHoTen())
                .soDienThoai(nd.getSoDienThoai())
                .vaiTro(taiKhoan.getVaiTro())
                .trangThai(taiKhoan.getTrangThai())
                .ngayTao(taiKhoan.getNgayTao())
                .build();
    }

    private void writeSystemLog(
            Integer maTKAdmin,
            String hanhDong,
            Integer maDoiTuong,
            String moTa,
            String ip
    ) {

        Account admin =
                accountRepository.findById(maTKAdmin)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Tài khoản quản trị không tồn tại"
                                )
                        );

        SystemLog log =
                SystemLog.builder()
                        .taiKhoan(admin)
                        .hanhDong(hanhDong)
                        .loaiDoiTuong("TAI_KHOAN")
                        .maDoiTuong(maDoiTuong)
                        .moTa(moTa)
                        .diaChiIp(ip)
                        .thoiGian(LocalDateTime.now())
                        .build();

        systemLogRepository.save(log);
    }
}