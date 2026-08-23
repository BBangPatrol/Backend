package com.bbangpatrol.bakery.client;

import com.bbangpatrol.bakery.dto.TourApiResponse;
import com.bbangpatrol.bakery.dto.TourApiResponse.TourItem;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
public class TourApiClient {

    // 공공데이터포털에서 발급받은 "디코딩" 인증키를 사용해야 함 (RestClient가 URI 인코딩을 자동으로 처리)
    @Value("${tourapi.service-key}")
    private String serviceKey;

    private static final String LOCATION_BASED_LIST_URI = "https://apis.data.go.kr/B551011/KorService2/locationBasedList2"
            + "?serviceKey={serviceKey}&numOfRows={numOfRows}&pageNo=1&MobileOS=ETC&MobileApp={mobileApp}"
            + "&_type=json&arrange=S&mapX={mapX}&mapY={mapY}&radius={radius}&contentTypeId=12";
    private static final String MOBILE_APP = "BbangPatrol";

    private final RestClient restClient = RestClient.create();

    public List<TourItem> getNearbyAttractions(BigDecimal lat, BigDecimal lng, int radius, int count) {
        TourApiResponse response = restClient.get()
                .uri(LOCATION_BASED_LIST_URI, serviceKey, count, MOBILE_APP, lng, lat, radius)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    log.error("[TourApiClient] 관광지 조회 실패: {}", res.getStatusCode());
                    throw new ApiException(ErrorCode.ATTRACTION_FETCH_FAILED);
                })
                .body(TourApiResponse.class);

        if (response == null
                || response.response() == null
                || response.response().body() == null
                || response.response().body().items() == null
                || response.response().body().items().item() == null) {
            return List.of();
        }
        return response.response().body().items().item();
    }
}
