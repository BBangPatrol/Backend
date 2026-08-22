package com.bbangpatrol.ocr.service;

import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.client.GeminiClient;
import com.bbangpatrol.ocr.client.OcrClient;
import com.bbangpatrol.ocr.client.ReceiptImageValidator;
import com.bbangpatrol.ocr.dto.OcrResponse;
import com.bbangpatrol.ocr.dto.ReceiptParseResult;
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
    private final BakeryRepository bakeryRepository;
    private final ReceiptTokenService receiptTokenService;

    // 영수증에서 정보를 추출하기 위한 메서드
    @Override
    public OcrResponse getInfo(Long userId, Long storeId, MultipartFile receipt) {
        log.info("[OCR SERVICE] 영수증에서 정보 꺼내기 시작!!! userId: {}, storedId: {}", userId, storeId);

        // OCR 전에 이미지 검증부터 실시
        imageValidator.validate(receipt);

        // FastAPI 호출해서 이미지 넘기기
        String ocrText = ocrClient.extractText(receipt);
        log.info("[OCR SERVICE] 결과:\n{}", ocrText);

        // 영수증에서 추출한 글자 LLM에 전달
        ReceiptParseResult parsedResult = geminiClient.parseReceipt(ocrText);

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
        log.info("[OCR SERVICE] 영수증 파싱 필수 조건 검증 통과!!");

        // 날짜가 오래됐으면 등록 기한이 지났다는 에러 반환
        validateReceiptDate(parsedResult.date());
        log.info("[OCR SERVICE] 영수증 만료 기한 검증 통과!!");

        // storeId를 통해서 영수증 정보에서 해당 가게를 진짜 방문한 건지 확인
        String businessNumber = bakeryRepository
                .findBusinessNumberByStoreId(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        if(!normalize(businessNumber)
                .equals(normalize(parsedResult.businessNumber()))) {
            throw new ApiException(ErrorCode.RECEIPT_STORE_MISMATCH);
        }
        log.info("[OCR SERVICE] 영수증의 사업자 번호와 사용자가 선택한 가게 일치!!");

        // 여기까지 왔으면 인증도 된 거니까 영수증 승인번호 토큰으로 발급해서 전달(유효기간 10분짜리임)
        String verificationToken = receiptTokenService.issueToken(
                userId,
                parsedResult.receiptNum()
        );
        log.info("[OCR SERVICE] 영수증 인증을 위한 토큰 발행!!");

        return new OcrResponse(
                parsedResult.bakeryName(),
                parsedResult.date(),
                parsedResult.amount(),
                parsedResult.menu(),
                verificationToken
        );
    }


    // ----- 기타 검증을 위한 메서드 ----- //
    private void validateParsedResult(ReceiptParseResult result) {

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

        if (result.businessNumber() == null || result.businessNumber().isBlank()) {
            throw new ApiException(ErrorCode.RECEIPT_BUSINESS_NUMBER_MISSING);
        }
    }

    private void validateReceiptDate(String date) {
        try {
            LocalDate receiptDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();

            // 오늘 기준 7일 이전 날짜보다 더 과거면 등록 불가. 일단 개발을 위해 30일로
            if (receiptDate.isBefore(today.minusDays(30))) {
                throw new ApiException(ErrorCode.RECEIPT_TOO_OLD);
            }

        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.RECEIPT_PARSE_FAILED);
        }
    }

    private String normalize(String value) { // 공백이랑 하이픈 제거하고 숫자만 비교
        return value == null ? null : value.trim().replaceAll("[^0-9]", "");
    }
}
