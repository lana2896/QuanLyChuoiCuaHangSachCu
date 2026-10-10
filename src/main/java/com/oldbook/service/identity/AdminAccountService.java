package com.oldbook.service.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oldbook.constant.auth.TrangThaiTaiKhoan;
import com.oldbook.constant.auth.VaiTro;
import com.oldbook.dto.identity.AdminTaiKhoanResponse;
import com.oldbook.dto.identity.ChangeRoleRequest;
import com.oldbook.dto.identity.CreateTaiKhoanRequest;
import com.oldbook.dto.identity.LockTaiKhoanRequest;
import com.oldbook.entity.identity.NguoiDung;
import com.oldbook.entity.identity.TaiKhoan;
import com.oldbook.entity.system.NhatKyHeThong;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.identity.NguoiDungRepository;
import com.oldbook.repository.identity.TaiKhoanRepository;
import com.oldbook.repository.system.NhatKyHeThongRepository;
import com.oldbook.service.auth.EmailService;
import com.oldbook.service.auth.TaiKhoanTokenService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminTaiKhoanService {

    private final NguoiDungRepository nguoiDungRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final NhatKyHeThongRepository nhatKyHeThongRepository;

    private final PasswordEncoder passwordEncoder;
    private final TaiKhoanTokenService taiKhoanTokenService;

    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<AdminTaiKhoanResponse> getAll(
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

        return taiKhoanRepository.findAll()
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
            TaiKhoan taiKhoan,
            String keyword
    ) {

        if (keyword.isBlank()) {
            return true;
        }

        NguoiDung nd = taiKhoan.getNguoiDung();

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
    public AdminTaiKhoanResponse create(
            CreateTaiKhoanRequest request,
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

        VaiTro vaiTro = parseRole(
                request.getVaiTro()
        );

        // Email không được trùng
        if (nguoiDungRepository.existsByEmail(email)) {
            throw new BusinessException(
                    "Email đã được sử dụng"
            );
        }

        // Số điện thoại nếu có nhập không được trùng
        if (soDienThoai != null
                && nguoiDungRepository
                .existsBySoDienThoai(soDienThoai)) {

            throw new BusinessException(
                    "Số điện thoại đã được sử dụng"
            );
        }

        NguoiDung nguoiDung =
                NguoiDung.builder()
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
                nguoiDungRepository.save(
                        nguoiDung
                );

        TaiKhoan taiKhoan =
                TaiKhoan.builder()
                        .nguoiDung(nguoiDung)
                        .vaiTro(vaiTro.name())
                        .trangThai(
                                TrangThaiTaiKhoan
                                        .HOAT_DONG.name()
                        )
                        .build();

        taiKhoan =
                taiKhoanRepository.save(
                        taiKhoan
                );

        ghiNhatKy(
                maTKAdmin,
                "TAO_TAI_KHOAN",
                taiKhoan.getMaTK(),
                "Tạo tài khoản mới với vai trò "
                        + vaiTro.name(),
                ip
        );

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
            LockTaiKhoanRequest request,
            Integer maTKAdmin,
            String ip
    ) {

        // Không được tự khóa
        if (maTKTarget.equals(maTKAdmin)) {
            throw new BusinessException(
                    "Hệ thống không cho phép Quản trị viên tự khóa tài khoản của chính mình"
            );
        }

        TaiKhoan taiKhoan =
                getTaiKhoan(maTKTarget);

        if (TrangThaiTaiKhoan.BI_KHOA.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản đã bị khóa"
            );
        }

        taiKhoan.setTrangThai(
                TrangThaiTaiKhoan.BI_KHOA.name()
        );

        taiKhoanRepository.save(taiKhoan);

        // Vô hiệu mọi JWT đang tồn tại
        taiKhoanTokenService.invalidateAllTokens(
                maTKTarget
        );

        ghiNhatKy(
                maTKAdmin,
                "KHOA_TAI_KHOAN",
                maTKTarget,
                "Khóa tài khoản. Lý do: "
                        + request.getLyDo().trim(),
                ip
        );

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

        TaiKhoan taiKhoan =
                getTaiKhoan(maTKTarget);

        if (!TrangThaiTaiKhoan.BI_KHOA.name()
                .equals(taiKhoan.getTrangThai())) {

            throw new BusinessException(
                    "Tài khoản hiện không bị khóa"
            );
        }

        taiKhoan.setTrangThai(
                TrangThaiTaiKhoan.HOAT_DONG.name()
        );

        taiKhoanRepository.save(taiKhoan);

        // Không xóa invalidBefore.
        // Token cũ vẫn không được phép sống lại.
        ghiNhatKy(
                maTKAdmin,
                "MO_KHOA_TAI_KHOAN",
                maTKTarget,
                "Mở khóa tài khoản",
                ip
        );
        emailService.sendAccountUnlockedEmail(
                taiKhoan.getNguoiDung().getEmail()
        );
    }

    @Transactional
    public AdminTaiKhoanResponse changeRole(
            Integer maTKTarget,
            ChangeRoleRequest request,
            Integer maTKAdmin,
            String ip
    ) {

        TaiKhoan taiKhoan =
                getTaiKhoan(maTKTarget);

        VaiTro vaiTroMoi =
                parseRole(request.getVaiTro());

        String vaiTroCu =
                taiKhoan.getVaiTro();

        if (vaiTroCu.equals(vaiTroMoi.name())) {
            throw new BusinessException(
                    "Tài khoản đã có vai trò này"
            );
        }

        Integer maND =
                taiKhoan.getNguoiDung().getMaND();

        if (taiKhoanRepository
                .existsByNguoiDung_MaNDAndVaiTroAndMaTKNot(
                        maND,
                        vaiTroMoi.name(),
                        maTKTarget
                )) {

            throw new BusinessException(
                    "Người dùng đã có tài khoản với vai trò "
                            + vaiTroMoi.name()
            );
        }

        taiKhoan.setVaiTro(
                vaiTroMoi.name()
        );

        taiKhoanRepository.save(taiKhoan);

        ghiNhatKy(
                maTKAdmin,
                "THAY_DOI_VAI_TRO",
                maTKTarget,
                "Thay đổi vai trò từ "
                        + vaiTroCu
                        + " sang "
                        + vaiTroMoi.name(),
                ip
        );

        return toResponse(taiKhoan);
    }

    private TaiKhoan getTaiKhoan(
            Integer maTK
    ) {

        return taiKhoanRepository.findById(maTK)
                .orElseThrow(() ->
                        new BusinessException(
                                "Tài khoản không tồn tại"
                        )
                );
    }

    private VaiTro parseRole(
            String vaiTro
    ) {

        VaiTro role;
        try {
            role = VaiTro.valueOf(
                    vaiTro.trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (Exception e) {

            throw new BusinessException(
                    "Vai trò không hợp lệ"
            );
        }

        // Đơn vị vận chuyển không còn dùng tài khoản: truy cập bằng link /shipping/{maCode}.
        if (role == VaiTro.DON_VI_VAN_CHUYEN) {
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

    private AdminTaiKhoanResponse toResponse(
            TaiKhoan taiKhoan
    ) {

        NguoiDung nd =
                taiKhoan.getNguoiDung();

        return AdminTaiKhoanResponse.builder()
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

    private void ghiNhatKy(
            Integer maTKAdmin,
            String hanhDong,
            Integer maDoiTuong,
            String moTa,
            String ip
    ) {

        TaiKhoan admin =
                taiKhoanRepository.findById(maTKAdmin)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Tài khoản quản trị không tồn tại"
                                )
                        );

        NhatKyHeThong log =
                NhatKyHeThong.builder()
                        .taiKhoan(admin)
                        .hanhDong(hanhDong)
                        .loaiDoiTuong("TAI_KHOAN")
                        .maDoiTuong(maDoiTuong)
                        .moTa(moTa)
                        .diaChiIp(ip)
                        .thoiGian(LocalDateTime.now())
                        .build();

        nhatKyHeThongRepository.save(log);
    }
}