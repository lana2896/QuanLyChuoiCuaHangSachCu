package com.oldbook.entity.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserBookKey implements Serializable {

    @Column(name = "ma_nd")
    private Integer maND;

    @Column(name = "ma_sach")
    private Integer maSach;
}
