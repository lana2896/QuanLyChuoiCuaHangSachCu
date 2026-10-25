package com.oldbook.service.location;

import com.oldbook.dto.location.ProvinceResponse;
import com.oldbook.dto.location.WardResponse;
import com.oldbook.entity.location.PhuongXa;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.location.PhuongXaRepository;
import com.oldbook.repository.location.TinhThanhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final PhuongXaRepository phuongXaRepository;
    private final TinhThanhRepository tinhThanhRepository;

    @Transactional(readOnly = true)
    public List<ProvinceResponse> getProvinces() {
        return tinhThanhRepository.findAllByOrderByTenTinhAsc()
                .stream()
                .map(t -> new ProvinceResponse(t.getMaTinh(), t.getTenTinh()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WardResponse> getWards(Integer maTinh) {
        return phuongXaRepository.findAllByTinh_MaTinhOrderByTenXaAsc(maTinh)
                .stream()
                .map(x -> new WardResponse(x.getMaXa(), x.getTenXa()))
                .toList();
    }

    public PhuongXa resolve(Integer maTinh, Integer maXa) {
        if (maTinh == null || maXa == null) {
            throw new BusinessException("Vui lòng chọn tỉnh/thành phố và phường/xã");
        }
        return phuongXaRepository.findByMaXaAndTinh_MaTinh(maXa, maTinh)
                .orElseThrow(() -> new BusinessException("Phường/xã không thuộc tỉnh/thành phố đã chọn"));
    }
}
