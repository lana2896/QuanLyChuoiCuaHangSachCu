package com.oldbook.service.catalog;

import com.oldbook.constant.order.OrderStatus;
import com.oldbook.dto.catalog.CreateReviewRequest;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.catalog.ReviewResponse;
import com.oldbook.dto.catalog.ReviewMediaResponse;
import com.oldbook.entity.system.Review;
import com.oldbook.entity.system.ReviewMedia;
import com.oldbook.repository.order.OrderItemRepository;
import com.oldbook.repository.system.ReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewMediaUploadService reviewMediaUploadService;

    public ReviewService(ReviewRepository reviewRepository,
                         OrderItemRepository orderItemRepository,
                         ReviewMediaUploadService reviewMediaUploadService) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.reviewMediaUploadService = reviewMediaUploadService;
    }

    public PageResponse<ReviewResponse> getByBook(Integer maSach, int page, int size) {
        var result = reviewRepository.findByBookId(maSach, PageRequest.of(Math.max(page, 0), size));
        if (result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages()) {
            result = reviewRepository.findByBookId(maSach, PageRequest.of(result.getTotalPages() - 1, size));
        }
        return PageResponse.of(result.map(ReviewService::toResponse));
    }

    @Transactional
    public ReviewResponse createReview(Integer maND, Integer maCTDH, CreateReviewRequest request) {
        return createReview(maND, maCTDH, request, List.of());
    }

    @Transactional
    public ReviewResponse createReview(Integer maND, Integer maCTDH, CreateReviewRequest request,
                                       List<MultipartFile> files) {
        var item = orderItemRepository.findByIdForUpdate(maCTDH)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy sách trong đơn hàng"));
        var storeOrder = item.getDonHangCuaHang();
        var customer = storeOrder.getDonHang().getNguoiMua();
        if (maND == null || !Objects.equals(customer.getMaND(), maND)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sách trong đơn hàng");
        }
        if (!OrderStatus.DA_GIAO.name().equals(storeOrder.getTrangThaiDonHang())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Chỉ được đánh giá sau khi sách đã giao thành công");
        }
        if (reviewRepository.existsByChiTietDonHang_MaCTDH(maCTDH)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã đánh giá sách trong đơn hàng này");
        }

        var uploaded = reviewMediaUploadService.upload(files, maCTDH);
        boolean cleanupOnCompletion = !uploaded.isEmpty()
                && TransactionSynchronizationManager.isSynchronizationActive();
        if (cleanupOnCompletion) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        reviewMediaUploadService.cleanup(uploaded);
                    }
                }
            });
        }

        try {
            var review = Review.builder()
                .chiTietDonHang(item)
                .nguoiDung(customer)
                .soSao(request.soSao())
                .binhLuan(request.binhLuan().strip())
                .build();
            for (var asset : uploaded) {
                review.getMedia().add(ReviewMedia.builder()
                        .danhGia(review).loaiMedia(asset.loaiMedia()).mediaUrl(asset.mediaUrl())
                        .cloudinaryPublicId(asset.publicId()).build());
            }
            return toResponse(reviewRepository.saveAndFlush(review));
        } catch (RuntimeException exception) {
            if (!cleanupOnCompletion) {
                reviewMediaUploadService.cleanup(uploaded);
            }
            throw exception;
        }
    }

    public static ReviewResponse toResponse(Review review) {
        String hoTen = review.getNguoiDung().getHoTen();
        return new ReviewResponse(review.getMaDanhGia(),
                hoTen == null || hoTen.isBlank() ? "Khách hàng" : hoTen,
                review.getSoSao(), review.getBinhLuan(), review.getNgayDanhGia(),
                review.getMedia() == null ? List.of() : review.getMedia().stream()
                        .filter(media -> "IMAGE".equals(media.getLoaiMedia()) || "VIDEO".equals(media.getLoaiMedia()))
                        .filter(media -> ReviewMediaUploadService.isValidMediaUrl(media.getMediaUrl()))
                        .map(media -> new ReviewMediaResponse(media.getMaMedia(), media.getLoaiMedia(), media.getMediaUrl()))
                        .toList());
    }
}
