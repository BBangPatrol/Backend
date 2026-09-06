package com.bbangpatrol.bakery.service;

import com.bbangpatrol.bakery.cache.AttractionCache;
import com.bbangpatrol.bakery.client.TourApiClient;
import com.bbangpatrol.bakery.dto.BakeryDetailResponse;
import com.bbangpatrol.bakery.dto.BakeryFavoriteResponse;
import com.bbangpatrol.bakery.dto.BakerySearchRequest;
import com.bbangpatrol.bakery.dto.BakerySearchResponse;
import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.entity.SignatureImage;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.bookmark.entity.Bookmark;
import com.bbangpatrol.bookmark.repository.BookmarkRepository;
import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.service.R2Service;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import com.bbangpatrol.visit.repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BakeryServiceTest {

    @Mock
    private BakeryRepository bakeryRepository;
    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private AttractionCache attractionCache;
    @Mock
    private TourApiClient tourApiClient;
    @Mock
    private R2Service r2Service;

    @InjectMocks
    private BakeryService bakeryService;

    @Test
    void 평점순으로_검색하고_방문수와_즐겨찾기_여부를_반환한다() {
        Bakery lowerRated = bakery(2L, "빵집 B", "3.5");
        Bakery higherRated = bakery(1L, "빵집 A", "4.8");
        when(bakeryRepository.findBakeryIdsForSearch(
                any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(1L, 2L));
        when(bakeryRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(lowerRated, higherRated));
        when(visitRepository.sumVisitCountsByBakeryIds(List.of(1L, 2L)))
                .thenReturn(List.of(new Object[]{1L, 12L}, new Object[]{2L, 3L}));
        when(bookmarkRepository.findBakeryIdsByUserIdAndBakeryIds(10L, List.of(1L, 2L)))
                .thenReturn(List.of(1L));

        BakerySearchResponse response = bakeryService.searchBakeries(
                10L, new BakerySearchRequest("rating", null, null, null, null));

        assertThat(response.result()).extracting(item -> item.bakery().id())
                .containsExactly(1L, 2L);
        assertThat(response.result().get(0).visitCnt()).isEqualTo(12L);
        assertThat(response.result().get(0).likes()).isTrue();
        assertThat(response.pageInfo().hasNext()).isFalse();
    }

    @Test
    void 거리순_검색은_위치가_없으면_실패한다() {
        BakerySearchRequest request = new BakerySearchRequest(
                "distance", null, null, null, null);

        assertThatThrownBy(() -> bakeryService.searchBakeries(null, request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 검색결과가_20개를_넘으면_마지막_빵집_ID를_다음_커서로_반환한다() {
        List<Long> searchedIds = LongStream.rangeClosed(1, 21).boxed().toList();
        List<Long> pageIds = searchedIds.subList(0, 20);
        List<Bakery> bakeries = pageIds.stream()
                .map(id -> bakery(id, "빵집 " + id, "4.0"))
                .toList();

        when(bakeryRepository.findBakeryIdsForSearch(
                any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(searchedIds);
        when(bakeryRepository.findAllById(pageIds)).thenReturn(bakeries);
        when(visitRepository.sumVisitCountsByBakeryIds(pageIds)).thenReturn(new ArrayList<>());

        BakerySearchResponse response = bakeryService.searchBakeries(
                null, new BakerySearchRequest("rating", null, null, null, null));

        assertThat(response.result()).hasSize(20);
        assertThat(response.pageInfo().hasNext()).isTrue();
        assertThat(response.pageInfo().nextCursor()).isEqualTo(20L);
    }

    @Test
    void 등록되지_않은_즐겨찾기를_추가한다() {
        User user = User.builder().id(10L).build();
        Bakery bakery = bakery(1L, "빵집 A", "4.8");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(bakeryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(bakery));
        when(bookmarkRepository.findByUserIdAndBakeryId(10L, 1L)).thenReturn(Optional.empty());

        BakeryFavoriteResponse response = bakeryService.toggleFavorite(10L, 1L);

        assertThat(response.likes()).isTrue();
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    void 가게_상세정보를_반환한다() {
        Bakery bakery = Bakery.builder()
                .id(1L)
                .name("빵집 A")
                .region(Region.YUSEONG)
                .summary("AI 요약")
                .content("가게 상세 설명")
                .signatureImages(List.of(SignatureImage.builder().imageUrl("bakeries/1/signature_menu.jpg").build()))
                .build();
        when(bakeryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(bakery));
        when(visitRepository.sumVisitCountByBakeryId(1L)).thenReturn(7L);
        when(bookmarkRepository.existsByUserIdAndBakeryId(10L, 1L)).thenReturn(true);
        when(r2Service.getPublicUrl("bakeries/1/signature_menu.jpg")).thenReturn("https://cdn.test/bakeries/1/signature_menu.jpg");

        BakeryDetailResponse response = bakeryService.getBakeryDetail(10L, 1L);

        assertThat(response.bakery().region()).isEqualTo("유성구");
        assertThat(response.bakery().summary()).isEqualTo("AI 요약");
        assertThat(response.bakery().content()).isEqualTo("가게 상세 설명");
        assertThat(response.bakery().image())
                .isEqualTo("https://cdn.test/bakeries/1/signature_menu.jpg");
        assertThat(response.visitCnt()).isEqualTo(7L);
        assertThat(response.likes()).isTrue();
    }

    @Test
    void 목록_썸네일은_시그니처_이미지_첫_장을_사용한다() {
        Bakery bakery = Bakery.builder()
                .id(1L)
                .name("빵집 A")
                .avgRating(new BigDecimal("4.5"))
                .signatureImages(List.of(SignatureImage.builder().imageUrl("bakeries/1/signature_menu.jpg").build()))
                .build();
        when(bakeryRepository.findBakeryIdsForSearch(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(bakeryRepository.findAllById(List.of(1L))).thenReturn(List.of(bakery));
        when(visitRepository.sumVisitCountsByBakeryIds(List.of(1L))).thenReturn(List.of());
        when(r2Service.getPublicUrl("bakeries/1/signature_menu.jpg")).thenReturn("https://cdn.test/bakeries/1/signature_menu.jpg");

        BakerySearchResponse response = bakeryService.searchBakeries(null,
                new BakerySearchRequest("rating", null, null, null, null));

        assertThat(response.result().get(0).bakery().image())
                .isEqualTo("https://cdn.test/bakeries/1/signature_menu.jpg");
    }

    private Bakery bakery(Long id, String name, String rating) {
        return Bakery.builder()
                .id(id)
                .name(name)
                .avgRating(new BigDecimal(rating))
                .build();
    }
}
