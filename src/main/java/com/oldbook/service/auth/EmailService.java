package com.oldbook.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.oldbook.exception.common.BusinessException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailNguoiGui;

    public void sendRegisterOtpEmail(
            String emailNguoiNhan,
            String maOtp
    ) {
        sendOtpEmail(
                emailNguoiNhan,
                maOtp,
                "Mã OTP xác thực tài khoản - OldBook Marketplace",
                "Bạn vừa thực hiện đăng ký tài khoản tại OldBook Marketplace."
        );
    }

    public void sendForgotPasswordOtpEmail(
            String emailNguoiNhan,
            String maOtp
    ) {
        sendOtpEmail(
                emailNguoiNhan,
                maOtp,
                "Mã OTP đặt lại mật khẩu - OldBook Marketplace",
                "Bạn vừa yêu cầu đặt lại mật khẩu tài khoản tại OldBook Marketplace."
        );
    }

    private void sendOtpEmail(
            String emailNguoiNhan,
            String maOtp,
            String tieuDe,
            String moTa
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(emailNguoiGui);
        message.setTo(emailNguoiNhan);
        message.setSubject(tieuDe);

        message.setText(
                "Xin chào,\n\n"
                        + moTa + "\n\n"
                        + "Mã OTP của bạn là: " + maOtp + "\n\n"
                        + "Mã OTP có hiệu lực trong 5 phút.\n"
                        + "Không chia sẻ mã OTP này với người khác.\n\n"
                        + "Trân trọng,\n"
                        + "OldBook Marketplace"
        );

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error(
                    "Không thể gửi email OTP đến {}",
                    emailNguoiNhan,
                    e
            );

            throw new BusinessException(
                    "Không thể gửi email OTP"
            );
        }
    }

    public void sendAccountLockedEmail(
            String emailNguoiNhan,
            String lyDo
    ) {
        sendAccountNotificationEmail(
                emailNguoiNhan,
                "Tài khoản đã bị khóa - OldBook Marketplace",
                "Tài khoản của bạn đã bị Quản trị viên khóa.\n\n"
                        + "Lý do: " + lyDo
        );
    }

    public void sendAccountUnlockedEmail(
            String emailNguoiNhan
    ) {
        sendAccountNotificationEmail(
                emailNguoiNhan,
                "Tài khoản đã được mở khóa - OldBook Marketplace",
                "Tài khoản của bạn đã được Quản trị viên mở khóa."
        );
    }

    private void sendAccountNotificationEmail(
            String emailNguoiNhan,
            String tieuDe,
            String noiDung
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(emailNguoiGui);
        message.setTo(emailNguoiNhan);
        message.setSubject(tieuDe);

        message.setText(
                "Xin chào,\n\n"
                        + noiDung
                        + "\n\nTrân trọng,\n"
                        + "OldBook Marketplace"
        );

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error(
                    "Không thể gửi email thông báo đến {}",
                    emailNguoiNhan,
                    e
            );

            throw new BusinessException(
                    "Không thể gửi email thông báo"
            );
        }
    }

    public void sendAdminAccountCreatedEmail(
            String email,
            String hoTen,
            String vaiTro
    ) {
        String noiDung =
                "Xin chào " + hoTen + ",\n\n"
                        + "Tài khoản của bạn đã được Quản trị viên tạo.\n"
                        + "Vai trò: " + vaiTro + "\n\n"
                        + "Vui lòng đăng nhập để sử dụng hệ thống.";

        sendNormalEmail(
                email,
                "Tài khoản OldBook Marketplace đã được tạo",
                noiDung
        );
    }

    private void sendNormalEmail(
            String emailNguoiNhan,
            String tieuDe,
            String noiDung
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(emailNguoiGui);
        message.setTo(emailNguoiNhan);
        message.setSubject(tieuDe);
        message.setText(
                noiDung
                        + "\n\nTrân trọng,\n"
                        + "OldBook Marketplace"
        );

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error(
                    "Không thể gửi email đến {}",
                    emailNguoiNhan,
                    e
            );

            throw new BusinessException(
                    "Không thể gửi email"
            );
        }
    }
}