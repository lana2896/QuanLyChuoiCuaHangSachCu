package com.oldbook.dto.notification;

import com.oldbook.entity.notification.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Integer maThongBao,
        String loai,
        String mucDo,
        String tieuDe,
        String noiDung,
        String duongDan,
        boolean daDoc,
        LocalDateTime ngayTao
) {

    public static NotificationResponse from(Notification thongBao) {
        return new NotificationResponse(
                thongBao.getMaThongBao(),
                thongBao.getLoai(),
                thongBao.getMucDo(),
                thongBao.getTieuDe(),
                thongBao.getNoiDung(),
                thongBao.getDuongDan(),
                thongBao.isDaDoc(),
                thongBao.getNgayTao()
        );
    }
}
