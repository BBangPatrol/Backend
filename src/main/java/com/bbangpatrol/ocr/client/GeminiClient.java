package com.bbangpatrol.ocr.client;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.dto.OcrResponse;
import com.bbangpatrol.ocr.dto.ReceiptParseResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final ObjectMapper objectMapper;

    @Value("${gemini.model}")
    private String MODEL;
    @Value("${gemini.api-key}")
    private String apiKey;

    // 타임아웃이 없으면 Gemini 가 한 번 느려질 때 요청이 영원히 매달린다.
    // 프론트는 영수증 분석 요청을 무제한으로 기다리므로(timeout: 0) 끊어줄 쪽은 서버뿐이다.
    // 값은 OCR 클라이언트(ocr.connect-timeout / ocr.read-timeout)와 같은 기준으로 넉넉하게 잡았다.
    @Value("${gemini.connect-timeout:5s}")
    private String connectTimeout;
    @Value("${gemini.read-timeout:60s}")
    private String readTimeout;

    private RestClient restClient;

    @PostConstruct
    void initRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(DurationStyle.detectAndParse(connectTimeout));
        factory.setReadTimeout(DurationStyle.detectAndParse(readTimeout));

        restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(factory)
                .build();
    }

    public ReceiptParseResult parseReceipt(String ocrText) {
        String prompt = """
                    다음은 OCR로 추출한 영수증 내용이다.
            
                    영수증에서 아래 정보만 추출하라.
            
                    - bakeryName: 매장명 또는 상호명
                    - address: 매장 주소
                    - date: 실제 결제 일자
                    - amount: 실제 최종 결제 금액
                    - menu: 실제 구매한 상품명 목록
                    - receiptNum: 카드 결제 승인번호
                    - businessNumber: 사업자등록번호
            
                    규칙:
            
                    [공통 규칙]
                    - 반드시 OCR 내용에 존재하는 정보만 사용한다.
                    - OCR 내용에 근거가 없는 값은 추측하거나 생성하지 않는다.
                    - 값을 확실하게 식별할 수 없는 경우 null을 반환한다.
            
                    [bakeryName]
                    - 매장명, 상호명, 업체명에 해당하는 값을 사용한다.
                    - 주소, 대표자명, 사업자번호는 bakeryName으로 사용하지 않는다.

                    [address]
                    - 영수증에 표시된 매장(가맹점)의 주소를 반환한다.
                    - 도로명 주소가 있으면 도로명 주소를 우선 사용하고, 없으면 지번 주소를 사용한다.
                    - 시/도, 구, 도로명, 건물번호가 모두 포함되도록 한 줄의 문자열로 반환한다.
                    - 층수나 호수처럼 상세 주소가 함께 있으면 포함해도 된다.
                    - 카드사 주소, 본사 주소로 명시된 값은 사용하지 않는다.
                    - 확실하게 식별할 수 없으면 null을 반환한다.

                    [date]
                    - 실제 결제가 이루어진 날짜를 사용한다.
                    - YYYY-MM-DD 형식으로 반환한다.
                    - 시간이 함께 표시되어 있어도 날짜만 반환한다.
                    - 승인일시와 매출일시가 모두 존재하고 같은 거래를 의미한다면 결제 날짜를 사용한다.
            
                    [amount]
                    - 고객이 실제로 결제한 최종 금액을 반환한다.
                    - 합계, 받은 금액, 신용카드 결제금액, 승인금액 등의 항목을 참고한다.
                    - 공급가액, 부가세, 개별 상품 가격은 amount로 사용하지 않는다.
                    - 쉼표와 통화 기호를 제거한 정수로 반환한다.
            
                    [menu]
                    - 실제 구매한 상품명만 추출한다.
                    - 상품이 여러 개라면 쉼표로 구분한 하나의 문자열로 반환한다.
                    - 단가, 수량, 금액, 카드명, 승인번호, 세금 정보는 포함하지 않는다.
                    - 상품명에 수량이 붙어 있더라도 가능하면 상품명만 반환한다.
            
                    [receiptNum]
                    - 카드 결제의 승인번호를 반환한다.
                    - "승인번호", "승인 번호", "승인No" 등 승인번호임을 나타내는 항목과 함께 있는 값을 우선 사용한다.
                    - [66778654]처럼 대괄호로 감싸진 8자리 숫자가 승인번호 영역에 존재하면 승인번호로 사용할 수 있다.
                    - 8자리 숫자가 여러 개 존재하더라도 승인번호라고 확실하게 판단되는 값만 사용한다.
                    - 사업자번호, 전화번호, 영수증번호, 가맹점번호, 카드번호는 receiptNum으로 사용하지 않는다.
                    - receiptNum은 숫자가 아니라 문자열로 반환한다.
                    - 승인번호를 확실하게 찾을 수 없다면 null을 반환한다.
                    
                    [businessNumber]
                    - 영수증에 표시된 사업자등록번호를 반환한다.
                    - "사업자", "사업자번호", "사업자등록번호", "사업자등록 번호" 등의 항목과 함께 있는 값을 우선 사용한다.
                    - 대한민국 사업자등록번호는 일반적으로 10자리이며 보통 000-00-00000 형식으로 표시된다.
                    - OCR에서 하이픈이 누락되거나 공백으로 분리되어 있어도 같은 번호로 판단할 수 있다.
                    - 전화번호, 카드번호, 승인번호, 가맹점번호, 영수증번호는 businessNumber로 사용하지 않는다.
                    - 확실하게 식별할 수 없으면 null을 반환한다.
                    - businessNumber는 문자열로 반환한다.
            
                    OCR 내용:
                    """ + ocrText;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "bakeryName", Map.of(
                                                "type", List.of("string", "null")
                                        ),
                                        "address", Map.of(
                                                "type", List.of("string", "null")
                                        ),
                                        "date", Map.of(
                                                "type", List.of("string", "null")
                                        ),
                                        "amount", Map.of(
                                                "type", List.of("integer", "null")
                                        ),
                                        "menu", Map.of(
                                                "type", List.of("string", "null")
                                        ),
                                        "receiptNum", Map.of(
                                                "type", List.of("string", "null")
                                        ),
                                        "businessNumber", Map.of(
                                                "type", List.of("string", "null")
                                        )
                                ),
                                "required", List.of(
                                        "bakeryName",
                                        "address",
                                        "date",
                                        "amount",
                                        "menu",
                                        "receiptNum",
                                        "businessNumber"
                                )
                        )
                )
        ); // 필요한 정보를 담을 body 만들기


        try {

            JsonNode response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", MODEL)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) { // 응답이 없으면 에러 던지기
                throw new ApiException(ErrorCode.RECEIPT_PARSE_FAILED);
            }

            String resultText = response // 응답에서 text 원문만 추출
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            if (resultText.isBlank()) {
                throw new ApiException(ErrorCode.RECEIPT_PARSE_FAILED);
            }

            log.info("[GEMINI] 영수증 파싱 결과: {}", resultText);

            return objectMapper.readValue( // ObjectMapper를 통해 OcrResponse 형식으로 변환
                    resultText,
                    ReceiptParseResult.class
            );

        } catch (ApiException e) {
            throw e;

        } catch (Exception e) {
            log.error("[GEMINI] 영수증 파싱 실패", e);
            throw new ApiException(ErrorCode.RECEIPT_PARSE_FAILED);
        }
    }
}
