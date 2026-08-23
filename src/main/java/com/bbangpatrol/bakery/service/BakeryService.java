package com.bbangpatrol.bakery.service;

import com.bbangpatrol.bakery.client.TourApiClient;
import com.bbangpatrol.bakery.dto.AttractionListResponse;
import com.bbangpatrol.bakery.dto.AttractionResponse;
import com.bbangpatrol.bakery.dto.TourApiResponse.TourItem;
import com.bbangpatrol.bakery.cache.AttractionCache;
import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.enums.AttractionCategory;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BakeryService {

    // 빵집 주변 관광지 API 고정 요청값
    private static final int ATTRACTION_RADIUS = 2000;
    private static final int ATTRACTION_COUNT = 3;

    private final BakeryRepository bakeryRepository;
    private final AttractionCache attractionCache;
    private final TourApiClient tourApiClient;

    public AttractionListResponse getNearbyAttractions(Long storeId) {
        Bakery bakery = bakeryRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        return attractionCache.find(storeId)
                .orElseGet(() -> fetchAndCacheAttractions(storeId, bakery));
    }

    private AttractionListResponse fetchAndCacheAttractions(Long storeId, Bakery bakery) {
        List<TourItem> items = tourApiClient.getNearbyAttractions(
                bakery.getLat(), bakery.getLng(), ATTRACTION_RADIUS, ATTRACTION_COUNT);

        List<AttractionResponse> attractions = items.stream()
                .map(this::toAttractionResponse)
                .toList();

        AttractionListResponse response = new AttractionListResponse(attractions);
        attractionCache.save(storeId, response);
        return response;
    }

    private AttractionResponse toAttractionResponse(TourItem item) {
        String imageUrl = StringUtils.hasText(item.firstimage()) ? item.firstimage()
                : StringUtils.hasText(item.firstimage2()) ? item.firstimage2()
                : null;

        Integer distance = StringUtils.hasText(item.dist())
                ? (int) Math.round(Double.parseDouble(item.dist()))
                : null;

        BigDecimal lat = StringUtils.hasText(item.mapy()) ? new BigDecimal(item.mapy()) : null;
        BigDecimal lng = StringUtils.hasText(item.mapx()) ? new BigDecimal(item.mapx()) : null;

        return new AttractionResponse(
                item.contentid(),
                AttractionCategory.fromContentTypeId(item.contenttypeid()).getLabel(),
                item.title(),
                item.addr1(),
                imageUrl,
                lat,
                lng,
                distance,
                item.tel()
        );
    }
}
