package com.oldbook.repository.identity;

import com.oldbook.entity.identity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    List<Address> findAllByNguoiDung_MaNDOrderByLaMacDinhDescMaDiaChiDesc(
            Integer maND
    );

    Optional<Address> findByMaDiaChiAndNguoiDung_MaND(
            Integer maDiaChi,
            Integer maND
    );
}