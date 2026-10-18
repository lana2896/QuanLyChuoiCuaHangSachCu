package com.oldbook.service.catalog;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.entity.catalog.UserBookKey;
import com.oldbook.entity.catalog.Wishlist;
import com.oldbook.entity.identity.User;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.catalog.BookSpecifications;
import com.oldbook.repository.catalog.WishlistRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final BookRepository bookRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<BookResponse> getWishlist(Integer maND) {
        return wishlistRepository.findPublicByUserId(maND,
                        ApprovalStatus.DA_DUYET.name(), SaleStatus.DANG_BAN.name())
                .stream().map(item -> BookMapper.toPublicResponse(item.getSach())).toList();
    }

    @Transactional
    public BookResponse addBook(Integer maND, Integer maSach) {
        validateBookId(maSach);
        User nguoiDung = lockOwner(maND);
        var sach = bookRepository.findOne(BookSpecifications.publicVisible()
                        .and((root, query, cb) -> cb.equal(root.get("maSach"), maSach)))
                .orElseThrow(() -> new BusinessException("Sách không tồn tại hoặc chưa được công khai"));
        var id = new UserBookKey(maND, maSach);
        if (!wishlistRepository.existsById(id)) {
            wishlistRepository.save(Wishlist.builder().id(id).nguoiDung(nguoiDung).sach(sach).build());
        }
        return BookMapper.toPublicResponse(sach);
    }

    @Transactional
    public void removeBook(Integer maND, Integer maSach) {
        validateBookId(maSach);
        lockOwner(maND);
        wishlistRepository.findById(new UserBookKey(maND, maSach)).ifPresent(wishlistRepository::delete);
    }

    private User lockOwner(Integer maND) {
        var nguoiDung = entityManager.find(User.class, maND, LockModeType.PESSIMISTIC_WRITE);
        if (nguoiDung == null) {
            throw new BusinessException("Người dùng không tồn tại");
        }
        return nguoiDung;
    }

    private void validateBookId(Integer maSach) {
        if (maSach == null || maSach < 1) {
            throw new BusinessException("Mã sách phải là số nguyên dương");
        }
    }
}
