package com.bbangpatrol.bakery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiResponse(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Body body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            @JsonDeserialize(using = ItemsDeserializer.class) Items items
    ) {}

    /**
     * 반경 내 결과가 0건이면 TourAPI 는 items 를 객체가 아니라 빈 문자열로 내려준다.
     * <pre>
     *   결과 있음: "items": {"item":[ ... ]}
     *   0건      : "items": ""
     * </pre>
     * 기본 설정으로는 여기서 InvalidFormatException 이 나고, 그러면
     * {@code TourApiClient} 의 {@code items == null} 가드까지 가지도 못한 채
     * RestClientException 으로 빠져나간다.
     * <p>
     * 전역 ObjectMapper 설정({@code ACCEPT_EMPTY_STRING_AS_NULL_OBJECT})으로도 되지만,
     * RestClient 가 쓰는 매퍼는 별개라 설정이 닿지 않는다. 그래서 DTO 에 붙여 둔다.
     */
    static class ItemsDeserializer extends JsonDeserializer<Items> {
        @Override
        public Items deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            // 객체가 아니면 결과 없음으로 본다
            if (parser.currentToken() == JsonToken.VALUE_STRING) return null;
            // Items 자체에는 이 디시리얼라이저가 걸려 있지 않으므로 기본 처리로 돌아간다 (재귀 아님)
            return parser.readValueAs(Items.class);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<TourItem> item) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TourItem(
            String contentid,
            String contenttypeid,
            String title,
            String addr1,
            String addr2,
            String firstimage,
            String firstimage2,
            String mapx,
            String mapy,
            String dist,
            String tel
    ) {}
}
