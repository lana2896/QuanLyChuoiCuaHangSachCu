package com.oldbook.config;

import com.oldbook.entity.location.TinhThanh;
import com.oldbook.repository.location.TinhThanhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocationDataInitializer implements ApplicationRunner {

    private static final List<String> THANH_PHO = List.of(
            "Hà Nội", "TP. Hồ Chí Minh", "Hải Phòng", "Đà Nẵng", "Cần Thơ", "Huế");

    private static final List<String> TINH = List.of(
            "Tuyên Quang", "Lào Cai", "Thái Nguyên", "Phú Thọ", "Bắc Ninh", "Hưng Yên",
            "Ninh Bình", "Quảng Trị", "Quảng Ngãi", "Gia Lai", "Khánh Hòa", "Lâm Đồng",
            "Đắk Lắk", "Đồng Nai", "Tây Ninh", "Vĩnh Long", "Đồng Tháp", "Cà Mau", "An Giang",
            "Cao Bằng", "Lạng Sơn", "Lai Châu", "Điện Biên", "Sơn La", "Quảng Ninh",
            "Thanh Hóa", "Nghệ An", "Hà Tĩnh");

    private final TinhThanhRepository tinhThanhRepository;

    @Override
    public void run(ApplicationArguments args) {
        THANH_PHO.forEach(ten -> seed(ten, "THANH_PHO"));
        TINH.forEach(ten -> seed(ten, "TINH"));
    }

    private void seed(String ten, String loai) {
        if (!tinhThanhRepository.existsByTenTinh(ten)) {
            tinhThanhRepository.save(TinhThanh.builder().tenTinh(ten).loai(loai).build());
        }
    }
}
