package com.oldbook.service.media;

import com.cloudinary.Cloudinary;
import com.oldbook.exception.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String INVALID_IMAGE = "Vui lòng chọn ảnh JPG, PNG hoặc WebP hợp lệ";

    private final Cloudinary cloudinary;

    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ảnh từ máy tính");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh không được vượt quá 10 MB");
        }
        byte[] content;
        try {
            content = file.getBytes();
            validateImage(content, file.getContentType());
        } catch (IOException exception) {
            throw new BusinessException(INVALID_IMAGE);
        }
        if (cloudinary.config == null || blank(cloudinary.config.cloudName)
                || blank(cloudinary.config.apiKey) || blank(cloudinary.config.apiSecret)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Dịch vụ tải ảnh chưa được cấu hình. Vui lòng liên hệ quản trị viên");
        }
        Map<?, ?> result;
        try {
            result = cloudinary.uploader().upload(content, Map.of(
                    "resource_type", "image",
                    "folder", folder,
                    "allowed_formats", List.of("jpg", "jpeg", "png", "webp"),
                    "timeout", 30));
        } catch (IOException | RuntimeException exception) {
            log.warn("Cloudinary upload failed ({}): {}", exception.getClass().getSimpleName(),
                    safeDiagnostic(exception));
            throw uploadFailed();
        }
        Object secureUrl = result == null ? null : result.get("secure_url");
        if (!(secureUrl instanceof String url) || url.length() > 500 || !isHttpsUrl(url)) {
            log.warn("Cloudinary upload response contains no valid HTTPS secure_url");
            throw uploadFailed();
        }
        return url;
    }

    public static void validateImage(byte[] content, String contentType) throws IOException {
        String format;
        if (isWebp(content)) {
            // The JDK has no WebP reader; Cloudinary validates the complete image on upload.
            format = "webp";
        } else {
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) {
                    throw new BusinessException(INVALID_IMAGE);
                }
                var reader = readers.next();
                try {
                    format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!List.of("jpeg", "png").contains(format)) {
                        throw new BusinessException(INVALID_IMAGE);
                    }
                    reader.setInput(input);
                    if (reader.getWidth(0) <= 0 || reader.getHeight(0) <= 0) {
                        throw new BusinessException(INVALID_IMAGE);
                    }
                } finally {
                    reader.dispose();
                }
            }
        }
        String mime = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
        if (!mime.isEmpty() && !"application/octet-stream".equals(mime)
                && !("image/" + format).equals(mime)) {
            throw new BusinessException(INVALID_IMAGE);
        }
    }

    private static boolean isWebp(byte[] content) {
        if (content.length < 20 || !matches(content, 0, "RIFF") || !matches(content, 8, "WEBP")
                || !(matches(content, 12, "VP8 ") || matches(content, 12, "VP8L") || matches(content, 12, "VP8X"))) {
            return false;
        }
        long size = (content[4] & 255L) | ((content[5] & 255L) << 8)
                | ((content[6] & 255L) << 16) | ((content[7] & 255L) << 24);
        return size + 8 == content.length;
    }

    private static boolean matches(byte[] content, int offset, String signature) {
        for (int i = 0; i < signature.length(); i++) {
            if (content[offset + i] != signature.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isHttpsUrl(String url) {
        try {
            var uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private ResponseStatusException uploadFailed() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Không tải được ảnh lên Cloudinary. Vui lòng thử lại");
    }

    private String safeDiagnostic(Throwable exception) {
        String detail = exception.getMessage() == null ? "No error message" : exception.getMessage();
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root != exception) {
            detail += " | " + root.getClass().getSimpleName() + ": " + root.getMessage();
        }
        for (String value : List.of(cloudinary.config.cloudName, cloudinary.config.apiKey,
                cloudinary.config.apiSecret)) {
            if (!blank(value)) {
                detail = detail.replace(value, "[REDACTED]");
            }
        }
        detail = detail.replaceAll("[\\r\\n\\t]", " ");
        return detail.length() > 1000 ? detail.substring(0, 1000) : detail;
    }
}
