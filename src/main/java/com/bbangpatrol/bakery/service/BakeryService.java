package com.bbangpatrol.bakery.service;

import com.bbangpatrol.bakery.client.TourApiClient;
import com.bbangpatrol.bakery.dto.*;
import com.bbangpatrol.bakery.dto.TourApiResponse.TourItem;
import com.bbangpatrol.bakery.cache.AttractionCache;
import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.enums.AttractionCategory;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.bookmark.entity.Bookmark;
import com.bbangpatrol.bookmark.repository.BookmarkRepository;
import com.bbangpatrol.common.dto.CursorPageInfo;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BakeryService {

    // 빵집 주변 관광지 API 고정 요청값
    private static final int ATTRACTION_RADIUS = 2000;
    private static final int ATTRACTION_COUNT = 3;
    private static final int SEARCH_PAGE_SIZE = 20;

    private final BakeryRepository bakeryRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;
    private final AttractionCache attractionCache;
    private final TourApiClient tourApiClient;
    private final R2Service r2Service;

    @Transactional(readOnly = true)
    public BakerySearchResponse searchBakeries(Long userId, BakerySearchRequest request) {
        validateSearchRequest(request);

        String name = StringUtils.hasText(request.name()) ? request.name().trim() : null;
        BigDecimal lat = request.lat() == null ? BigDecimal.ZERO : request.lat();
        BigDecimal lon = request.lon() == null ? BigDecimal.ZERO : request.lon();

        List<Long> bakeryIds = bakeryRepository.findBakeryIdsForSearch(
                request.sort(), name, lat, lon, request.cursor(),
                PageRequest.of(0, SEARCH_PAGE_SIZE + 1));

        boolean hasNext = bakeryIds.size() > SEARCH_PAGE_SIZE;
        List<Long> pageIds = hasNext ? bakeryIds.subList(0, SEARCH_PAGE_SIZE) : bakeryIds;
        Map<Long, Bakery> bakeryMap = bakeryRepository.findAllById(pageIds).stream()
                .collect(Collectors.toMap(Bakery::getId, bakery -> bakery));
        Map<Long, Long> visitCounts = getVisitCounts(pageIds);
        Set<Long> favoriteBakeryIds = getFavoriteBakeryIds(userId, pageIds);

        List<BakerySearchItemResponse> result = pageIds.stream()
                .map(bakeryMap::get)
                .filter(Objects::nonNull)
                .map(bakery -> new BakerySearchItemResponse(
                        BakerySummaryResponse.from(bakery, r2Service::getPublicUrl),
                        visitCounts.getOrDefault(bakery.getId(), 0L),
                        favoriteBakeryIds.contains(bakery.getId())
                ))
                .toList();

        Long nextCursor = hasNext ? pageIds.get(pageIds.size() - 1) : null;
        return new BakerySearchResponse(result, new CursorPageInfo(result.size(), hasNext, nextCursor));
    }

    @Transactional(readOnly = true)
    public HotBakeryResponse.BakeryListDTO getHotBakery() {
        return HotBakeryResponse.BakeryListDTO.builder()
                .stores(bakeryRepository.findHotBakeries()
                        .stream()
                        .map(bakery -> HotBakeryResponse.BakerySimpleDTO.builder()
                                .storeId(bakery.getId())
                                .storeName(bakery.getName())
                                .image(r2Service.getPublicUrl(bakery.getSignatureImages().stream()
                                        .findFirst()
                                        .orElse(null).getImageUrl()))
                                .rating(bakery.getAvgRating())
                                .region(bakery.getRegion() == null ? null : bakery.getRegion().getValue())
                                .build())
                        .toList())
                .build();
    }

    @Transactional
    public BakeryFavoriteResponse toggleFavorite(Long userId, Long storeId) {
        if (userId == null) throw new ApiException(ErrorCode.UNAUTHORIZED_401);

        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Bakery bakery = bakeryRepository.findByIdAndDeletedAtIsNull(storeId).orElseThrow(() -> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        Optional<Bookmark> bookmark = bookmarkRepository.findByUserIdAndBakeryId(userId, storeId);
        if (bookmark.isPresent()) {
            bookmarkRepository.delete(bookmark.get());
            return new BakeryFavoriteResponse(false);
        }

        bookmarkRepository.save(Bookmark.builder()
                .user(user)
                .bakery(bakery)
                .createdAt(LocalDateTime.now())
                .build());
        return new BakeryFavoriteResponse(true);
    }

    @Transactional(readOnly = true)
    public BakeryDetailResponse getBakeryDetail(Long userId, Long storeId) {
        Bakery bakery = bakeryRepository.findByIdAndDeletedAtIsNull(storeId).orElseThrow(() -> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        long visitCount = visitRepository.sumVisitCountByBakeryId(storeId);
        boolean likes = userId != null && bookmarkRepository.existsByUserIdAndBakeryId(userId, storeId);

        return new BakeryDetailResponse(BakeryDetail.from(bakery, r2Service::getPublicUrl), visitCount, likes);
    }

    public AttractionListResponse getNearbyAttractions(Long storeId) {
        Bakery bakery = bakeryRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAKERY_NOT_FOUND));

        return attractionCache.find(storeId, bakery.getLat(), bakery.getLng())
                .orElseGet(() -> fetchAndCacheAttractions(storeId, bakery));
    }

    private void validateSearchRequest(BakerySearchRequest request) {
        if (!List.of("distance", "rating", "visit").contains(request.sort())) throw new ApiException(ErrorCode.BAD_REQUEST);
        if ("distance".equals(request.sort()) && (request.lat() == null || request.lon() == null)) throw new ApiException(ErrorCode.BAD_REQUEST);
        if (request.lat() != null && (request.lat().compareTo(BigDecimal.valueOf(-90)) < 0 || request.lat().compareTo(BigDecimal.valueOf(90)) > 0)) throw new ApiException(ErrorCode.BAD_REQUEST);
        if (request.lon() != null && (request.lon().compareTo(BigDecimal.valueOf(-180)) < 0 || request.lon().compareTo(BigDecimal.valueOf(180)) > 0)) throw new ApiException(ErrorCode.BAD_REQUEST);
    }

    private Map<Long, Long> getVisitCounts(List<Long> bakeryIds) {
        if (bakeryIds.isEmpty()) return Collections.emptyMap();

        return visitRepository.sumVisitCountsByBakeryIds(bakeryIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }

    private Set<Long> getFavoriteBakeryIds(Long userId, List<Long> bakeryIds) {
        if (userId == null || bakeryIds.isEmpty()) return Collections.emptySet();
        return new HashSet<>(bookmarkRepository.findBakeryIdsByUserIdAndBakeryIds(userId, bakeryIds));
    }

    private AttractionListResponse fetchAndCacheAttractions(Long storeId, Bakery bakery) {
        List<TourItem> items = tourApiClient.getNearbyAttractions(
                bakery.getLat(), bakery.getLng(), ATTRACTION_RADIUS, ATTRACTION_COUNT);

        List<AttractionResponse> attractions = items.stream()
                .map(this::toAttractionResponse)
                .toList();

        AttractionListResponse response = new AttractionListResponse(attractions);
        attractionCache.save(storeId, bakery.getLat(), bakery.getLng(), response);
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
