package com.bbangpatrol.ocr.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.client.GeminiClient;
import com.bbangpatrol.ocr.client.OcrClient;
import com.bbangpatrol.ocr.client.ReceiptImageValidator;
import com.bbangpatrol.ocr.dto.OcrResponse;
import com.bbangpatrol.ocr.dto.ReceiptMatchResponse;
import com.bbangpatrol.ocr.dto.ReceiptParseResult;
import com.bbangpatrol.visit.service.ReceiptDuplicateChecker;
import com.bbangpatrol.visit.service.ReceiptHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final ReceiptImageValidator imageValidator;
    private final OcrClient ocrClient;
    private final GeminiClient geminiClient;
    private final BakeryRepository bakeryRepository;
    private final ReceiptStoreMatcher storeMatcher;
    private final StoreResolver storeResolver;

    // 영수증 등록 기한. 시연 기간에 찍은 영수증을 계속 쓸 수 있도록 90일로 뒀다
    private static final int RECEIPT_VALID_DAYS = 90;
    // 서버가 UTC 로 돌아도 영업일 기준은 한국 날짜다
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReceiptTokenService receiptTokenService;
    private final ReceiptHashService receiptHashService;
    private final ReceiptDuplicateChecker receiptDuplicateChecker;

    // 영수증에서 정보를 추출하기 위한 메서드
    @Override
    public OcrResponse getInfo(Long userId, Long storeId, MultipartFile receipt) {
        log.info("[OCR SERVICE] 영수증에서 정보 꺼내기 시작!!! userId: {}, storedId: {}", userId, storeId);

        ReceiptParseResult parsedResult = readReceipt(receipt);

        // storeId를 통해서 영수증 정보에서 해당 가게를 진짜 방문한 건지 확인
        Bakery bakery = bakeryRepository
                .findByIdAndDeletedAtIsNull(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        verifyStore(bakery, parsedResult);

        String verificationToken = checkDuplicateAndIssueToken(userId, bakery, parsedResult);

        return new OcrResponse(
                parsedResult.bakeryName(),
                parsedResult.date(),
                parsedResult.amount(),
                parsedResult.menu(),
                verificationToken
        );
    }



    /**
     * 가게를 고르지 않고 영수증만 올리는 경로.
     * 영수증에서 읽은 값으로 가게를 찾아내고, 찾은 가게로 나머지 검증을 그대로 태운다.
     * 발급하는 토큰에 storeId 가 담기므로 방문 등록(2단계)은 기존 API 를 그대로 쓴다.
     */
    @Override
    public ReceiptMatchResponse getInfoByReceipt(Long userId, MultipartFile receipt) {
        log.info("[OCR SERVICE] 영수증만으로 가게 찾기 시작!!! userId: {}", userId);

        ReceiptParseResult parsedResult = readReceipt(receipt);

        StoreResolver.Resolution resolution = storeResolver.resolve(parsedResult);

        // 후보가 여럿이면 고르지 않는다. 틀린 가게에 방문을 붙이는 것보다 가게를 고르는 기존 흐름으로 보내는 편이 낫다
        if (resolution.isAmbiguous()) {
            log.info("[OCR SERVICE] 가게 후보가 여럿이다. 수=" + resolution.candidates().size());
            throw new ApiException(ErrorCode.RECEIPT_STORE_AMBIGUOUS);
        }
        if (!resolution.isMatched()) {
            throw new ApiException(ErrorCode.RECEIPT_STORE_NOT_REGISTERED);
        }

        Bakery bakery = resolution.matched();
        log.info("[OCR SERVICE] 영수증으로 찾은 가게: {}({})", bakery.getName(), bakery.getId());

        String verificationToken = checkDuplicateAndIssueToken(userId, bakery, parsedResult);

        return new ReceiptMatchResponse(
                bakery.getId(),
                bakery.getName(),
                parsedResult.bakeryName(),
                parsedResult.date(),
                parsedResult.amount(),
                parsedResult.menu(),
                verificationToken
        );
    }

    /** 이미지 검증 → OCR → LLM 파싱 → 필수값·기한 검증. 두 경로가 공유한다. */
    private ReceiptParseResult readReceipt(MultipartFile receipt) {
        // OCR 전에 이미지 검증부터 실시
        imageValidator.validate(receipt);

        // FastAPI 호출해서 이미지 넘기기
        String ocrText = ocrClient.extractText(receipt);
        log.info("[OCR SERVICE] 결과:\n{}", ocrText);

        // 영수증에서 추출한 글자 LLM에 전달
        ReceiptParseResult parsedResult = geminiClient.parseReceipt(ocrText);

        log.info(
                "[OCR SERVICE] LLM 파싱 결과: bakeryName={}, address={}, date={}, amount={}, menu={}, receiptNum={}",
                parsedResult.bakeryName(),
                parsedResult.address(),
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

        return parsedResult;
    }

    /**
     * 중복 사용을 확인하고 방문 등록용 토큰을 발급한다.
     *
     * 해시는 "이 영수증"을 가리키는 값이므로 가게에 저장된 번호가 아니라 영수증에서 읽은 번호로 만든다
     * (사업자번호를 모르는 가게는 저장된 값이 NULL 이라 모든 가게의 해시가 한 자리에서 뭉개진다).
     * 토큰에는 방문 등록(2단계)이 쓸 값을 통째로 담는다. 2단계가 요청 본문의 금액·날짜를 쓰면
     * 여기서 무엇을 검증하든 다른 값으로 저장할 수 있다.
     */
    private String checkDuplicateAndIssueToken(Long userId, Bakery bakery, ReceiptParseResult parsedResult) {
        LocalDate receiptDate = LocalDate.parse(parsedResult.date());

        String receiptHash = receiptHashService.create(
                parsedResult.businessNumber(),
                parsedResult.receiptNum(),
                receiptDate,
                parsedResult.amount()
        );

        if (receiptDuplicateChecker.isAlreadyUsed(receiptHash, userId)) {
            log.info("[OCR SERVICE] 영수증 사용 가능 여부 확인 완료 - 이미 사용된 영수증...");
            throw new ApiException(ErrorCode.RECEIPT_ALREADY_USED);
        }
        log.info("[OCR SERVICE] 영수증 사용 가능 여부 확인 완료 - 통과!!");

        String verificationToken = receiptTokenService.issueToken(
                new ReceiptTokenService.ReceiptTicket(
                        userId,
                        bakery.getId(),
                        parsedResult.receiptNum(),
                        parsedResult.businessNumber(),
                        parsedResult.amount(),
                        receiptDate
                )
        );
        log.info("[OCR SERVICE] 영수증 인증을 위한 토큰 발행!!");
        return verificationToken;
    }

    /**
     * 영수증이 사용자가 고른 가게의 것인지 본다.
     *
     * 사업자번호를 아는 가게는 번호로 거른다. 다만 번호는 지점을 구분하지 못해서
     * (여러 지점이 한 법인 번호를 쓰면 다른 지점 영수증도 통과한다) 주소가 명백히 다르면 막는다.
     *
     * 번호를 모르는 가게는 가이드북에만 실려 있어 대조할 번호가 없다. 이때는 상호와 주소를 함께 본다.
     * 근거가 약한 경로이므로 둘 다 확인돼야 통과다 — 주소를 못 읽었으면(UNKNOWN) 통과시키지 않는다.
     */
    private void verifyStore(Bakery bakery, ReceiptParseResult parsedResult) {
        ReceiptStoreMatcher.AddressMatch addressMatch =
                storeMatcher.matchAddress(bakery.getAddress(), parsedResult.address());

        String storedNumber = bakery.getBusinessNumber();
        boolean knownNumber = storedNumber != null && !storedNumber.isBlank();

        boolean sameName = storeMatcher.matchesName(bakery.getName(), parsedResult.bakeryName());

        if (knownNumber) {
            if (!normalize(storedNumber).equals(normalize(parsedResult.businessNumber()))) {
                // 상호가 같은데 번호가 다르면 같은 브랜드의 다른 지점이다 (지점마다 사업자가 따로인 경우)
                log.info("[OCR SERVICE] 사업자번호 불일치. bakeryId={}, 상호일치={}", bakery.getId(), sameName);
                throw new ApiException(sameName
                        ? ErrorCode.RECEIPT_STORE_BRANCH_MISMATCH
                        : ErrorCode.RECEIPT_STORE_MISMATCH);
            }
            if (addressMatch == ReceiptStoreMatcher.AddressMatch.MISMATCH) {
                // 번호는 같은데 주소가 다르다 = 여러 지점이 한 법인 번호를 쓰는 경우
                log.info("[OCR SERVICE] 사업자번호는 같지만 주소가 다르다 - 다른 지점 영수증으로 보인다. bakeryId={}", bakery.getId());
                throw new ApiException(ErrorCode.RECEIPT_STORE_BRANCH_MISMATCH);
            }
            log.info("[OCR SERVICE] 영수증의 사업자 번호와 사용자가 선택한 가게 일치!!");
            return;
        }

        if (!sameName) {
            log.info("[OCR SERVICE] 사업자번호를 모르는 가게 - 상호 불일치. bakeryId={}", bakery.getId());
            throw new ApiException(ErrorCode.RECEIPT_STORE_MISMATCH);
        }
        if (addressMatch == ReceiptStoreMatcher.AddressMatch.MISMATCH) {
            log.info("[OCR SERVICE] 사업자번호를 모르는 가게 - 상호는 같지만 주소가 다르다. bakeryId={}", bakery.getId());
            throw new ApiException(ErrorCode.RECEIPT_STORE_BRANCH_MISMATCH);
        }
        if (addressMatch != ReceiptStoreMatcher.AddressMatch.MATCH) {
            log.info("[OCR SERVICE] 사업자번호를 모르는 가게 - 주소 확인 실패({}). bakeryId={}", addressMatch, bakery.getId());
            throw new ApiException(ErrorCode.RECEIPT_STORE_MISMATCH);
        }
        log.info("[OCR SERVICE] 사업자번호를 모르는 가게 - 상호와 주소로 확인 통과!! bakeryId={}", bakery.getId());
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

            // 서버 타임존이 지정돼 있지 않아 컨테이너에서는 UTC 로 돈다.
            // 그대로 LocalDate.now() 를 쓰면 한국 시간 오전(= UTC 전날)에 산 영수증이
            // "내일 날짜"로 보여 미래 영수증으로 걸린다. 그래서 기준을 한국 날짜로 고정한다.
            LocalDate today = LocalDate.now(KST);

            if (receiptDate.isBefore(today.minusDays(RECEIPT_VALID_DAYS))) {
                throw new ApiException(ErrorCode.RECEIPT_TOO_OLD);
            }

            // 미래 날짜는 영수증일 수 없다. 상한이 없으면 날짜를 앞당겨 적은 영수증이 그대로 통과한다
            if (receiptDate.isAfter(today)) {
                log.info("[OCR SERVICE] 미래 날짜 영수증이다. date={}, 오늘={}", receiptDate, today);
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
