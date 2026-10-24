package com.oldbook.service.catalog;

import com.cloudinary.Cloudinary;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.service.media.ImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewMediaUploadService {
    private static final long MIB = 1024 * 1024;
    private static final long MAX_IMAGE_SIZE = 10 * MIB;
    private static final long MAX_VIDEO_SIZE = 50 * MIB;
    private static final long MAX_TOTAL_SIZE = 60 * MIB;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> MP4_BRANDS = Set.of("isom", "iso2", "iso3", "iso4", "iso5",
            "iso6", "iso7", "iso8", "iso9", "mp41", "mp42", "avc1", "dash", "M4V ", "MSNV");
    private static final String INVALID_MEDIA =
            "Vui lòng chọn ảnh JPG, PNG, WebP hoặc video MP4, WebM hợp lệ";

    private final Cloudinary cloudinary;

    public List<UploadedMedia> upload(List<MultipartFile> files, Integer maCTDH) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<ValidatedMedia> validated = validateBatch(files);
        if (cloudinary.config == null || blank(cloudinary.config.cloudName)
                || blank(cloudinary.config.apiKey) || blank(cloudinary.config.apiSecret)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Dịch vụ tải ảnh/video chưa được cấu hình. Vui lòng liên hệ quản trị viên");
        }
        String folder = "oldbook/reviews/" + maCTDH;
        var attempted = new ArrayList<UploadedMedia>();
        var uploaded = new ArrayList<UploadedMedia>();
        try {
            for (ValidatedMedia file : validated) {
                String publicId = UUID.randomUUID().toString();
                String qualifiedId = folder + "/" + publicId;
                attempted.add(new UploadedMedia(file.loaiMedia(), null, qualifiedId, file.resourceType()));
                Map<?, ?> result = cloudinary.uploader().upload(file.content(), Map.of(
                        "resource_type", file.resourceType(),
                        "folder", folder,
                        "public_id", publicId,
                        "overwrite", false,
                        "allowed_formats", file.resourceType().equals("image")
                                ? List.of("jpg", "jpeg", "png", "webp") : List.of("mp4", "webm"),
                        "timeout", 120));
                Object secureUrl = result == null ? null : result.get("secure_url");
                Object returnedId = result == null ? null : result.get("public_id");
                if (!(secureUrl instanceof String url) || !isValidMediaUrl(url)
                        || !qualifiedId.equals(returnedId)) {
                    throw uploadFailed();
                }
                uploaded.add(new UploadedMedia(file.loaiMedia(), url, qualifiedId, file.resourceType()));
            }
            return List.copyOf(uploaded);
        } catch (IOException | RuntimeException exception) {
            cleanup(attempted);
            log.warn("Cloudinary review media upload failed ({})", exception.getClass().getSimpleName());
            throw uploadFailed();
        }
    }

    public void cleanup(List<UploadedMedia> uploaded) {
        for (UploadedMedia asset : uploaded) {
            try {
                cloudinary.uploader().destroy(asset.publicId(), Map.of(
                        "resource_type", asset.resourceType(), "invalidate", true));
            } catch (IOException | RuntimeException exception) {
                log.warn("Cloudinary review media cleanup failed for {} ({})",
                        asset.publicId(), exception.getClass().getSimpleName());
            }
        }
    }

    private List<ValidatedMedia> validateBatch(List<MultipartFile> files) {
        if (files.size() > 5) {
            throw new BusinessException("Mỗi đánh giá chỉ được đính kèm tối đa 5 ảnh/video");
        }
        long totalSize = 0;
        var validated = new ArrayList<ValidatedMedia>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new BusinessException("Ảnh/video đính kèm không được để trống");
            }
            String extension = extension(file.getOriginalFilename());
            boolean image = IMAGE_EXTENSIONS.contains(extension);
            boolean video = "mp4".equals(extension) || "webm".equals(extension);
            if (!image && !video) {
                throw new BusinessException(INVALID_MEDIA);
            }
            long limit = image ? MAX_IMAGE_SIZE : MAX_VIDEO_SIZE;
            if (file.getSize() > limit) {
                throw fileTooLarge(image);
            }
            totalSize += file.getSize();
            if (totalSize > MAX_TOTAL_SIZE) {
                throw totalTooLarge();
            }
            try {
                byte[] content = file.getBytes();
                if (content.length == 0) {
                    throw new BusinessException(INVALID_MEDIA);
                }
                if (content.length > limit) {
                    throw fileTooLarge(image);
                }
                if (image) {
                    ImageUploadService.validateImage(content, file.getContentType());
                } else {
                    validateVideo(content, extension, file.getContentType());
                }
                validated.add(new ValidatedMedia(content, image ? "image" : "video", image ? "IMAGE" : "VIDEO"));
            } catch (IOException exception) {
                throw new BusinessException(INVALID_MEDIA);
            }
        }
        if (validated.stream().mapToLong(media -> media.content().length).sum() > MAX_TOTAL_SIZE) {
            throw totalTooLarge();
        }
        return validated;
    }

    private void validateVideo(byte[] content, String extension, String contentType) {
        String mime = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
        if (!mime.isEmpty() && !"application/octet-stream".equals(mime) && !("video/" + extension).equals(mime)) {
            throw new BusinessException(INVALID_MEDIA);
        }
        if (!("mp4".equals(extension) ? isMp4(content) : isWebm(content))) {
            throw new BusinessException(INVALID_MEDIA);
        }
    }

    private boolean isMp4(byte[] content) {
        if (content.length < 16 || !matches(content, 4, "ftyp")) {
            return false;
        }
        long boxSize = ((content[0] & 255L) << 24) | ((content[1] & 255L) << 16)
                | ((content[2] & 255L) << 8) | (content[3] & 255L);
        if (boxSize < 16 || boxSize > content.length || (boxSize - 16) % 4 != 0) {
            return false;
        }
        if (MP4_BRANDS.contains(new String(content, 8, 4, StandardCharsets.US_ASCII))) {
            return true;
        }
        for (int offset = 16; offset + 4 <= boxSize; offset += 4) {
            if (MP4_BRANDS.contains(new String(content, offset, 4, StandardCharsets.US_ASCII))) {
                return true;
            }
        }
        return false;
    }

    private boolean isWebm(byte[] content) {
        if (content.length < 12 || (content[0] & 255) != 0x1a || (content[1] & 255) != 0x45
                || (content[2] & 255) != 0xdf || (content[3] & 255) != 0xa3) {
            return false;
        }
        int lengthBytes = vintLength(content[4]);
        if (lengthBytes == 0 || 4 + lengthBytes > content.length) {
            return false;
        }
        long headerSize = content[4] & (0xff >> lengthBytes);
        for (int i = 1; i < lengthBytes; i++) {
            headerSize = (headerSize << 8) | (content[4 + i] & 255L);
        }
        long headerEnd = 4L + lengthBytes + headerSize;
        if (headerSize <= 0 || headerEnd > content.length || headerSize > 4096) {
            return false;
        }
        for (int offset = 4 + lengthBytes; offset + 7 <= headerEnd; offset++) {
            if ((content[offset] & 255) == 0x42 && (content[offset + 1] & 255) == 0x82
                    && (content[offset + 2] & 255) == 0x84 && matches(content, offset + 3, "webm")) {
                return true;
            }
        }
        return false;
    }

    private int vintLength(byte first) {
        for (int length = 1; length <= 8; length++) {
            if ((first & (0x80 >> (length - 1))) != 0) {
                return length;
            }
        }
        return 0;
    }

    private boolean matches(byte[] content, int offset, String text) {
        if (offset < 0 || offset + text.length() > content.length) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if ((content[offset + i] & 255) != text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private String extension(String filename) {
        if (filename == null || filename.lastIndexOf('.') < 0) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isValidMediaUrl(String url) {
        if (url == null || url.length() > 500) {
            return false;
        }
        try {
            var uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && "res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)
                    && uri.getFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ResponseStatusException fileTooLarge(boolean image) {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                image ? "Ảnh không được vượt quá 10 MB" : "Video không được vượt quá 50 MB");
    }

    private ResponseStatusException totalTooLarge() {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                "Tổng dung lượng ảnh/video của một đánh giá không được vượt quá 60 MB");
    }

    private ResponseStatusException uploadFailed() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Không tải được ảnh/video lên Cloudinary. Vui lòng thử lại");
    }

    private record ValidatedMedia(byte[] content, String resourceType, String loaiMedia) {
    }

    public record UploadedMedia(String loaiMedia, String mediaUrl, String publicId, String resourceType) {
    }
}
