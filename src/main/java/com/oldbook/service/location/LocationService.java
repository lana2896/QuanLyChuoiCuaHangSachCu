package com.oldbook.service.location;

import com.oldbook.entity.location.PhuongXa;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.location.PhuongXaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final PhuongXaRepository phuongXaRepository;

    public PhuongXa resolve(Integer maTinh, Integer maXa) {
        if (maTinh == null || maXa == null) {
            throw new BusinessException("Vui lòng chọn tỉnh/thành phố và phường/xã");
        }
        return phuongXaRepository.findByMaXaAndTinh_MaTinh(maXa, maTinh)
                .orElseThrow(() -> new BusinessException("Phường/xã không thuộc tỉnh/thành phố đã chọn"));
    }
}
