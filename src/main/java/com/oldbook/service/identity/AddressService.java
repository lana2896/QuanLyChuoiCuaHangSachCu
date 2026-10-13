package com.oldbook.service.identity;

import com.oldbook.dto.identity.AddressRequest;
import com.oldbook.dto.identity.AddressResponse;
import com.oldbook.entity.identity.Address;
import com.oldbook.entity.identity.User;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.identity.AddressRepository;
import com.oldbook.repository.identity.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getAll(Integer maND) {
        return addressRepository
                .findAllByNguoiDung_MaNDOrderByLaMacDinhDescMaDiaChiDesc(maND)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse create(Integer maND, AddressRequest request) {

        User nguoiDung = getUser(maND);

        List<Address> current =
                addressRepository.findAllByNguoiDung_MaNDOrderByLaMacDinhDescMaDiaChiDesc(maND);

        boolean makeDefault =
                Boolean.TRUE.equals(request.getLaMacDinh()) || current.isEmpty();

        if (makeDefault) {
            clearDefault(current);
        }

        Address diaChi = Address.builder()
                .nguoiDung(nguoiDung)
                .tenNguoiNhan(clean(request.getTenNguoiNhan()))
                .soDienThoai(clean(request.getSoDienThoai()))
                .tinhThanh(clean(request.getTinhThanh()))
                .quanHuyen(clean(request.getQuanHuyen()))
                .phuongXa(clean(request.getPhuongXa()))
                .diaChiChiTiet(clean(request.getDiaChiChiTiet()))
                .laMacDinh(makeDefault)
                .build();

        return toResponse(addressRepository.save(diaChi));
    }

    @Transactional
    public AddressResponse update(Integer maND, Integer maDiaChi, AddressRequest request) {

        Address diaChi = getAddress(maND, maDiaChi);

        boolean wasDefault = Boolean.TRUE.equals(diaChi.getLaMacDinh());
        boolean requestedDefault = Boolean.TRUE.equals(request.getLaMacDinh());

        if (requestedDefault) {
            clearDefault(
                    addressRepository
                            .findAllByNguoiDung_MaNDOrderByLaMacDinhDescMaDiaChiDesc(maND)
            );
            diaChi.setLaMacDinh(true);

        } else if (!wasDefault) {
            diaChi.setLaMacDinh(false);

        } else {
            diaChi.setLaMacDinh(true);
        }

        diaChi.setTenNguoiNhan(clean(request.getTenNguoiNhan()));
        diaChi.setSoDienThoai(clean(request.getSoDienThoai()));
        diaChi.setTinhThanh(clean(request.getTinhThanh()));
        diaChi.setQuanHuyen(clean(request.getQuanHuyen()));
        diaChi.setPhuongXa(clean(request.getPhuongXa()));
        diaChi.setDiaChiChiTiet(clean(request.getDiaChiChiTiet()));

        return toResponse(addressRepository.save(diaChi));
    }

    @Transactional
    public void delete(Integer maND, Integer maDiaChi) {

        Address diaChi = getAddress(maND, maDiaChi);
        boolean wasDefault = Boolean.TRUE.equals(diaChi.getLaMacDinh());

        addressRepository.delete(diaChi);

        if (wasDefault) {
            List<Address> remaining =
                    addressRepository
                            .findAllByNguoiDung_MaNDOrderByLaMacDinhDescMaDiaChiDesc(maND);

            if (!remaining.isEmpty()) {
                Address nextDefault = remaining.get(0);
                nextDefault.setLaMacDinh(true);
                addressRepository.save(nextDefault);
            }
        }
    }

    @Transactional
    public AddressResponse setDefault(Integer maND, Integer maDiaChi) {

        Address target = getAddress(maND, maDiaChi);

        List<Address> all =
                addressRepository
                        .findAllByNguoiDung_MaNDOrderByLaMacDinhDescMaDiaChiDesc(maND);

        clearDefault(all);
        target.setLaMacDinh(true);

        return toResponse(addressRepository.save(target));
    }

    private User getUser(Integer maND) {
        return userRepository.findById(maND)
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));
    }

    private Address getAddress(Integer maND, Integer maDiaChi) {
        return addressRepository.findByMaDiaChiAndNguoiDung_MaND(maDiaChi, maND)
                .orElseThrow(() -> new BusinessException("Địa chỉ không tồn tại"));
    }

    private void clearDefault(List<Address> addresses) {
        for (Address address : addresses) {
            address.setLaMacDinh(false);
        }

        if (!addresses.isEmpty()) {
            addressRepository.saveAll(addresses);
        }
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private AddressResponse toResponse(Address diaChi) {
        return AddressResponse.builder()
                .maDiaChi(diaChi.getMaDiaChi())
                .tenNguoiNhan(diaChi.getTenNguoiNhan())
                .soDienThoai(diaChi.getSoDienThoai())
                .tinhThanh(diaChi.getTinhThanh())
                .quanHuyen(diaChi.getQuanHuyen())
                .phuongXa(diaChi.getPhuongXa())
                .diaChiChiTiet(diaChi.getDiaChiChiTiet())
                .laMacDinh(diaChi.getLaMacDinh())
                .build();
    }
}
