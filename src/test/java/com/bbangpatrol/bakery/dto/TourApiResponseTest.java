package com.bbangpatrol.bakery.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TourApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("반경 내 결과가 0건이면 TourAPI 가 items 를 빈 문자열로 내려주는데, 터지지 않고 null 로 읽힌다")
    void 결과가_없으면_items_가_빈_문자열로_온다() throws Exception {
        String body = """
                {"response": {"header":{"resultCode":"0000","resultMsg":"OK"},
                 "body": {"items": "","numOfRows":0,"pageNo":1,"totalCount":0}}}
                """;

        TourApiResponse response = objectMapper.readValue(body, TourApiResponse.class);

        // 빈 문자열이 null 로 들어와야 TourApiClient 의 null 가드가 빈 목록을 돌려줄 수 있다
        assertThat(response.response().body().items()).isNull();
    }

    @Test
    @DisplayName("결과가 있으면 item 목록이 그대로 채워진다")
    void 결과가_있으면_목록이_채워진다() throws Exception {
        String body = """
                {"response": {"header":{"resultCode":"0000","resultMsg":"OK"},
                 "body": {"items": {"item":[
                   {"contentid":"1622695","contenttypeid":"12","title":"계족산성",
                    "addr1":"대전광역시 대덕구","dist":"1708.06","mapx":"127.42","mapy":"36.35"}
                 ]},"numOfRows":1,"pageNo":1,"totalCount":1}}}
                """;

        TourApiResponse response = objectMapper.readValue(body, TourApiResponse.class);

        assertThat(response.response().body().items().item())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.contentid()).isEqualTo("1622695");
                    assertThat(item.title()).isEqualTo("계족산성");
                });
    }

    @Test
    @DisplayName("응답에 모르는 필드가 늘어나도 읽기에 실패하지 않는다")
    void 모르는_필드가_있어도_읽힌다() {
        String body = """
                {"response": {"header":{"resultCode":"0000","resultMsg":"OK"},
                 "body": {"items": {"item":[{"contentid":"1","newField":"x"}]},
                          "totalCount":1,"anotherNewField":123}}}
                """;

        assertThatCode(() -> objectMapper.readValue(body, TourApiResponse.class))
                .doesNotThrowAnyException();
    }
}
