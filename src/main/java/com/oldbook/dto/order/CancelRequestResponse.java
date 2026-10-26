package com.oldbook.dto.order;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelRequestResponse {
    private Integer maYeuCau;
    private String lyDo;
    private String lyDoPhanHoi;
    private String trangThaiYeuCau;
    private LocalDateTime ngayGui;
    private LocalDateTime ngayXuLy;
}
