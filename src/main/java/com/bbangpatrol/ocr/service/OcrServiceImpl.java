package com.bbangpatrol.ocr.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.client.GeminiClient;
import com.bbangpatrol.ocr.client.OcrClient;
import com.bbangpatrol.ocr.client.ReceiptImageValidator;
import com.bbangpatrol.ocr.dto.OcrResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final ReceiptImageValidator imageValidator;
    private final OcrClient ocrClient;
    private final GeminiClient geminiClient;

    // 영수증에서 정보를 추출하기 위한 메서드
    @Override
    public OcrResponse getInfo(Long userId, String storeId, MultipartFile receipt) {
        log.info("[OCR SERVICE] 영수증에서 정보 꺼내기 시작!!! userId: {}, storedId: {}", userId, storeId);

        // OCR 전에 이미지 검증부터 실시
        imageValidator.validate(receipt);

        // FastAPI 호출해서 이미지 넘기기
        String ocrText = ocrClient.extractText(receipt);
        log.info("[OCR SERVICE] 결과:\n{}", ocrText);

        // 영수증에서 추출한 글자 LLM에 전달
        OcrResponse parsedResult = geminiClient.parseReceipt(ocrText);

        log.info(
                "[OCR SERVICE] LLM 파싱 결과: bakeryName={}, date={}, amount={}, menu={}, receiptNum={}",
                parsedResult.bakeryName(),
                parsedResult.date(),
                parsedResult.amount(),
                parsedResult.menu(),
                parsedResult.receiptNum()
        );

        // 영수증에 필요한 정보가 다 있는지 확인
        validateParsedResult(parsedResult);
        // 날짜가 오래됐으면 등록 기한이 지났다는 에러 반환
        validateReceiptDate(parsedResult.date());

        // storeId를 통해서 영수증 정보에서 해당 가게를 진짜 방문한 건지 확인

        return parsedResult;
    }

    private void validateParsedResult(OcrResponse result) {

        if(result == null) { // 결과 자체가 없을 때
            throw new ApiException(ErrorCode.RECEIPT_PARSE_FAILED);
        }

        if(result.bakeryName() == null || result.bakeryName().isBlank()) { // 빵집 이름을 찾을 수 없을 때
            throw new ApiException(ErrorCode.RECEIPT_BAKERY_NAME_MISSING);
        }

        if(result.date() == null || result.date().isBlank()) { // 날짜를 찾을 수 없을 때
            throw new ApiException(ErrorCode.RECEIPT_DATE_MISSING);
        }

        if(result.amount() == null) { // 금액을 찾을 수 없을 때
            throw new ApiException(ErrorCode.RECEIPT_AMOUNT_MISSING);
        }

        if(result.menu() == null || result.menu().isBlank()) { // 메뉴를 찾을 수 없을 때
            throw new ApiException(ErrorCode.RECEIPT_MENU_MISSING);
        }

        if(result.receiptNum() == null || result.receiptNum().isBlank()) { // 승인번호를 찾을 수 없을 때
            throw new ApiException(ErrorCode.RECEIPT_NUM_MISSING);
        }
    }

    private void validateReceiptDate(String date) {
        try {
            LocalDate receiptDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();

            // 오늘 기준 7일 이전 날짜보다 더 과거면 등록 불가
            if (receiptDate.isBefore(today.minusDays(7))) {
                throw new ApiException(ErrorCode.RECEIPT_TOO_OLD);
            }

        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.RECEIPT_PARSE_FAILED);
        }
    }
}
