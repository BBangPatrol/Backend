package com.bbangpatrol.review.controller;

import com.bbangpatrol.review.dto.ReviewCreatedRequest;
import com.bbangpatrol.review.entity.Review;
import com.bbangpatrol.review.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사용자가 리뷰를 올릴 때 multipart/form-data 가 {@link ReviewCreatedRequest} 로 제대로 바인딩되는지 검증한다.
 * 사진이 서비스까지 도달하지 않으면 썸네일도 만들어지지 않으므로 업로드 경로의 출발점이다.
 * 보안 필터는 태우지 않는다(standaloneSetup) — 여기서 볼 것은 바인딩이다.
 */
class ReviewUploadBindingTest {

    private static final long USER_ID = 6L;

    private final ReviewService reviewService = mock(ReviewService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ReviewController(reviewService))
            // @AuthenticationPrincipal 을 해석해준다. 없으면 컨트롤러가 null 을 long 으로 언박싱하다 NPE
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    @BeforeEach
    void setUpPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("사진 여러 장과 키워드가 요청 DTO 까지 그대로 전달된다")
    void bindsMultipartRequest() throws Exception {
        when(reviewService.createReview(anyLong(), anyLong(), any()))
                .thenReturn(Review.builder().id(104L).build());

        mockMvc.perform(multipart("/api/v1/stores/1/reviews")
                        .file(new MockMultipartFile("reviewImages", "a.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .file(new MockMultipartFile("reviewImages", "b.jpg", "image/jpeg", new byte[]{4, 5}))
                        .param("rating", "5")
                        .param("content", "소금빵이 진짜 맛있어요")
                        .param("keywordIds", "1", "3"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reviewId").value(104));

        ArgumentCaptor<ReviewCreatedRequest> captor = ArgumentCaptor.forClass(ReviewCreatedRequest.class);
        // 토큰에서 꺼낸 userId 와 경로의 storeId 가 그대로 전달된다
        verify(reviewService).createReview(eq(USER_ID), eq(1L), captor.capture());
        ReviewCreatedRequest request = captor.getValue();

        assertThat(request.rating()).isEqualTo(5);
        assertThat(request.content()).isEqualTo("소금빵이 진짜 맛있어요");
        assertThat(request.keywordIds()).containsExactly(1L, 3L);
        // 사진이 서비스까지 도달해야 썸네일이 생성된다
        assertThat(request.reviewImages()).hasSize(2);
        assertThat(request.reviewImages().get(0).getOriginalFilename()).isEqualTo("a.jpg");
        assertThat(request.reviewImages().get(1).getOriginalFilename()).isEqualTo("b.jpg");
    }

    @Test
    @DisplayName("사진 없이 올리면 reviewImages 는 비어 있고 업로드를 시도하지 않는다")
    void bindsRequestWithoutImages() throws Exception {
        when(reviewService.createReview(anyLong(), anyLong(), any()))
                .thenReturn(Review.builder().id(105L).build());

        mockMvc.perform(multipart("/api/v1/stores/1/reviews")
                        .param("rating", "4")
                        .param("content", "웨이팅이 좀 길어요"))
                .andExpect(status().isCreated());

        ArgumentCaptor<ReviewCreatedRequest> captor = ArgumentCaptor.forClass(ReviewCreatedRequest.class);
        verify(reviewService).createReview(anyLong(), anyLong(), captor.capture());
        ReviewCreatedRequest request = captor.getValue();

        // null 이거나 빈 목록이어야 한다. ReviewService 는 두 경우 모두 업로드를 건너뛴다
        assertThat(request.reviewImages() == null || request.reviewImages().isEmpty()).isTrue();
        assertThat(request.keywordIds() == null || request.keywordIds().isEmpty()).isTrue();
    }
}
