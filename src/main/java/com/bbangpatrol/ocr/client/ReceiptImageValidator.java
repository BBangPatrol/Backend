package com.bbangpatrol.ocr.client;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Component
public class ReceiptImageValidator {

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB 제한

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/jpg",
            "image/webp",
            "image/heic",
            "image/heif"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".heic", ".heif"
    );

    public void validate(MultipartFile file) {
        if(file == null || file.isEmpty()) { // 파일이 없을 때
            throw new ApiException(ErrorCode.NO_IMAGE_ATTACHED);
        }
        if(file.getSize() > MAX_SIZE) { // 파일이 너무 클 때
            throw new ApiException(ErrorCode.TOO_LARGE_PAYLOAD);
        }
        if (!isTypeOk(file) && !isExtensionOk(file)) { // 지원하지 않는 타입일 때
            throw new ApiException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    // 확장자 확인
    private boolean isExtensionOk(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            return false;
        }
        String normalized = filename.toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.stream().anyMatch(normalized::endsWith);
    }

    // Content-Type 확인
    private boolean isTypeOk(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalized = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        return ALLOWED_TYPES.contains(normalized);
    }
}
