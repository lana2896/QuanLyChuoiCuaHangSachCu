package com.oldbook.service.catalog;

import com.oldbook.dto.catalog.ReviewResponse;
import com.oldbook.repository.system.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public List<ReviewResponse> getByBook(Integer maSach) {
        return reviewRepository.findByBookId(maSach).stream()
                .map(review -> {
                    String hoTen = review.getNguoiDung().getHoTen();
                    return new ReviewResponse(review.getMaDanhGia(),
                            hoTen == null || hoTen.isBlank() ? "Khách hàng" : hoTen,
                            review.getSoSao(), review.getBinhLuan(), review.getNgayDanhGia());
                })
                .toList();
    }
}
