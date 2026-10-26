package com.oldbook.repository.payment;

import com.oldbook.entity.payment.BankTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Integer> {

    Optional<BankTransaction> findByMaGiaoDichCong(String maGiaoDichCong);

    Page<BankTransaction> findAllByOrderByMaGiaoDichDesc(Pageable pageable);

    Optional<BankTransaction> findFirstByPhien_MaPhienAndKetQuaOrderByMaGiaoDichDesc(Integer maPhien, String ketQua);
}
