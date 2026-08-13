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
            "image/webp",
            "image/heic",
            "image/heif"
    );

    public void validate(MultipartFile file) {
        if(file == null || file.isEmpty()) { // 파일이 없을 때
            throw new ApiException(ErrorCode.NO_IMAGE_ATTACHED);
        }
        if(file.getSize() > MAX_SIZE) { // 파일이 너무 클 때
            throw new ApiException(ErrorCode.TOO_LARGE_PAYLOAD);
        }

        String contentType = file.getContentType(); // 파일 형식이 안 맞을 때
        if(contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApiException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
    }
}
