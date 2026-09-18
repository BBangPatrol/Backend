package com.bbangpatrol.ocr.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.entity.BakeryBusinessNumber;
import com.bbangpatrol.bakery.repository.BakeryBusinessNumberRepository;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.client.GeminiClient;
import com.bbangpatrol.ocr.client.OcrClient;
import com.bbangpatrol.ocr.client.ReceiptImageValidator;
import com.bbangpatrol.ocr.dto.OcrResponse;
import com.bbangpatrol.ocr.dto.ReceiptParseResult;
import com.bbangpatrol.ocr.dto.ReceiptTokenPayload;
import com.bbangpatrol.visit.service.ReceiptDuplicateChecker;
import com.bbangpatrol.visit.service.ReceiptHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final ReceiptImageValidator imageValidator;
    private final OcrClient ocrClient;
    private final GeminiClient geminiClient;
    private final BakeryRepository bakeryRepository;
    private final BakeryBusinessNumberRepository bakeryBusinessNumberRepository;

    private final ReceiptTokenService receiptTokenService;
    private final ReceiptHashService receiptHashService;
    private final ReceiptDuplicateChecker receiptDuplicateChecker;

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

        Bakery bakery = bakeryRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        // 대표 사업자번호 또는 등록해 둔 지점 사업자번호와 맞는지 확인
        BusinessNumberMatch match = matchBusinessNumber(bakery, parsedResult);
        log.info("[OCR SERVICE] 영수증의 사업자 번호와 사용자가 선택한 가게 일치!! 집계 지역: {}", match.region());

        // 해시는 DB 대표번호가 아니라 영수증에 찍힌 번호로 만든다. 지점이 다르면 다른 해시가 나와야 한다
        String receiptHash = receiptHashService.create(
                match.businessNumber(),
                parsedResult.receiptNum(),
                LocalDate.parse(parsedResult.date()),
                parsedResult.amount()
        );

        if (receiptDuplicateChecker.isAlreadyUsed(receiptHash, userId)) {
            log.info("[OCR SERVICE] 영수증 사용 가능 여부 확인 완료 - 이미 사용된 영수증...");
            throw new ApiException(ErrorCode.RECEIPT_ALREADY_USED);
        }
        log.info("[OCR SERVICE] 영수증 사용 가능 여부 확인 완료 - 통과!!");

        // 여기까지 왔으면 인증도 된 거니까 영수증 승인번호 토큰으로 발급해서 전달(유효기간 10분짜리임)
        String verificationToken = receiptTokenService.issueToken(
                userId,
                new ReceiptTokenPayload(
                        parsedResult.receiptNum(),
                        match.businessNumber(),
                        match.region()
                )
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


    // ----- 사업자번호 매칭을 위한 메서드 ----- //

    // 해시에 쓸 사업자번호와, 미션 집계에 쓸 실제 방문 지역
    private record BusinessNumberMatch(String businessNumber, Region region) {
    }

    private BusinessNumberMatch matchBusinessNumber(Bakery bakery, ReceiptParseResult parsed) {
        String receiptNumber = normalize(parsed.businessNumber());

        // 빵산책 지도에 실린 대표 매장
        if (bakery.getBusinessNumber() != null
                && normalize(bakery.getBusinessNumber()).equals(receiptNumber)) {
            return new BusinessNumberMatch(bakery.getBusinessNumber(), bakery.getRegion());
        }

        // 같은 매장으로 인정하기로 등록해 둔 지점
        List<BakeryBusinessNumber> branches = bakeryBusinessNumberRepository.findAllByBakeryId(bakery.getId());
        for (BakeryBusinessNumber branch : branches) {
            if (normalize(branch.getBusinessNumber()).equals(receiptNumber)) {
                log.info("[OCR SERVICE] 지점 영수증으로 인증한다. storeId: {}, 지점: {}",
                        bakery.getId(), branch.getBranchName());
                // 지점의 구가 따로 있으면 그 구로 미션을 집계한다
                return new BusinessNumberMatch(
                        branch.getBusinessNumber(),
                        branch.getRegion() != null ? branch.getRegion() : bakery.getRegion()
                );
            }
        }

        // 대표번호도 지점도 없으면 애초에 인증이 불가능한 빵집이다. 가게가 없다고 하면 원인을 못 찾는다
        if (bakery.getBusinessNumber() == null && branches.isEmpty()) {
            log.warn("[OCR SERVICE] 사업자번호가 등록되지 않은 빵집이다. storeId: {}", bakery.getId());
            throw new ApiException(ErrorCode.BAKERY_BUSINESS_NUMBER_MISSING);
        }

        // 상호가 같은 브랜드로 보이면 미등록 지점일 가능성이 높다.
        // 통과시키지는 않고, 어떤 지점을 등록해야 하는지 로그로 남긴다
        if (looksLikeSameBrand(parsed.bakeryName(), bakery.getName())) {
            log.warn("[OCR SERVICE] 미등록 지점으로 추정된다. storeId: {}, 상호: {}, 사업자번호: {}",
                    bakery.getId(), parsed.bakeryName(), parsed.businessNumber());
            throw new ApiException(ErrorCode.RECEIPT_BRANCH_NOT_REGISTERED);
        }

        throw new ApiException(ErrorCode.RECEIPT_STORE_MISMATCH);
    }

    // 에러 메시지를 고르는 용도로만 쓴다. 여기서 참이 나와도 인증이 통과하지는 않는다
    private boolean looksLikeSameBrand(String receiptName, String bakeryName) {
        if (receiptName == null || bakeryName == null) {
            return false;
        }

        String receipt = receiptName.replaceAll("\\s+", "");
        String registered = bakeryName.replaceAll("\\s+", "");

        if (receipt.isEmpty() || registered.isEmpty()) {
            return false;
        }

        return receipt.contains(registered) || registered.contains(receipt);
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

            // 오늘 기준 7일 이전 날짜보다 더 과거면 등록 불가. 일단 개발을 위해 10년으로
            if (receiptDate.isBefore(today.minusDays(3650))) {
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
