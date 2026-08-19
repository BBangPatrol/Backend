package com.bbangpatrol.ocr.client;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.ocr.dto.OcrResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    public OcrResponse parseReceipt(String ocrText) {
        String prompt = """
                다음은 OCR로 추출한 영수증 내용이다.

                영수증에서 다음 정보만 추출하라.

                - bakeryName: 매장 또는 상호명
                - date: 결제 일자
                - amount: 실제 최종 결제 금액
                - menu: 구매한 상품명 목록

                규칙:
                - OCR 내용에 존재하는 정보만 사용한다.
                - 추측하지 않는다.
                - 확인할 수 없는 값은 null로 반환한다.
                - date는 YYYY-MM-DD 형식으로 반환한다.
                - amount는 쉼표 없이 정수로 반환한다.
                - menu는 상품명만 쉼표로 구분한 문자열로 반환한다.
                - 단가, 수량, 금액, 카드명, 승인번호 등은 menu에 포함하지 않는다.
                - 합계, 받은 금액, 승인 금액 중 실제 결제 금액을 판단한다.

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
                                        "date", Map.of(
                                                "type", List.of("string", "null")
                                        ),
                                        "amount", Map.of(
                                                "type", List.of("integer", "null")
                                        ),
                                        "menu", Map.of(
                                                "type", List.of("string", "null")
                                        )
                                ),
                                "required", List.of(
                                        "bakeryName",
                                        "date",
                                        "amount",
                                        "menu"
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
                    OcrResponse.class
            );

        } catch (ApiException e) {
            throw e;

        } catch (Exception e) {
            log.error("[GEMINI] 영수증 파싱 실패", e);
            throw new ApiException(ErrorCode.RECEIPT_PARSE_FAILED);
        }
    }
}
