package com.oldbook.service.catalog;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.entity.catalog.RecentlyViewed;
import com.oldbook.entity.catalog.UserBookKey;
import com.oldbook.entity.identity.User;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.catalog.BookSpecifications;
import com.oldbook.repository.catalog.RecentlyViewedRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentlyViewedService {

    private static final int RECENT_BOOK_LIMIT = 20;

    private final RecentlyViewedRepository recentlyViewedRepository;
    private final BookRepository bookRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<BookResponse> getRecentlyViewed(Integer maND) {
        return recentlyViewedRepository.findPublicByUserId(maND,
                        ApprovalStatus.DA_DUYET.name(), SaleStatus.DANG_BAN.name(),
                        PageRequest.of(0, RECENT_BOOK_LIMIT))
                .stream().map(item -> BookMapper.toPublicResponse(item.getSach())).toList();
    }

    @Transactional
    public BookResponse recordView(Integer maND, Integer maSach) {
        if (maSach == null || maSach < 1) {
            throw new BusinessException("Mã sách phải là số nguyên dương");
        }
        var nguoiDung = entityManager.find(User.class, maND, LockModeType.PESSIMISTIC_WRITE);
        if (nguoiDung == null) {
            throw new BusinessException("Người dùng không tồn tại");
        }
        var sach = bookRepository.findOne(BookSpecifications.publicVisible()
                        .and((root, query, cb) -> cb.equal(root.get("maSach"), maSach)))
                .orElseThrow(() -> new BusinessException("Sách không tồn tại hoặc chưa được công khai"));
        var id = new UserBookKey(maND, maSach);
        var viewed = recentlyViewedRepository.findById(id).orElseGet(() ->
                RecentlyViewed.builder().id(id).nguoiDung(nguoiDung).sach(sach).build());
        viewed.setLanXemGanNhat(LocalDateTime.now());
        recentlyViewedRepository.save(viewed);
        return BookMapper.toPublicResponse(sach);
    }
}
