package com.bbangpatrol.ocr.client;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.dto.OcrResponseFromFast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrClient {

    private final RestClient ocrRestClient;

    public String extractText(MultipartFile image) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", toResource(image));

        try {
            OcrResponseFromFast response = ocrRestClient.post()
                    .uri("/api/receipts/extract")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(OcrResponseFromFast.class);

            if (response == null || response.text() == null || response.text().isBlank()) {
                throw new ApiException(ErrorCode.OCR_EMPTY_RESULT);
            }
            return response.text();

        } catch (HttpClientErrorException e) { // FastAPI랑 통신 중에 발생할 수 있는 에러들
            log.warn("[OCR] 요청 오류: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(ErrorCode.OCR_INVALID_REQUEST);
        } catch (HttpServerErrorException e) {
            log.error("[OCR] 서버 내부 오류: {}", e.getResponseBodyAsString());
            throw new ApiException(ErrorCode.OCR_SERVER_ERROR);
        } catch (ResourceAccessException e) {
            log.error("[OCR] 연결 실패", e);
            throw new ApiException(ErrorCode.OCR_UNAVAILABLE);
        }
    }

    private Resource toResource(MultipartFile image) {
        try {
            String filename = Optional.ofNullable(image.getOriginalFilename())
                    .filter(n -> !n.isBlank())
                    .orElse("receipt.jpg");

            return new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (IOException e) {
            throw new ApiException(ErrorCode.IMAGE_READ_FAILED);
        }
    }
}