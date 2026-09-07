package com.bbangpatrol.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        long id,
        long writerId,
        String writerName,
        String writerImageUrl,
        int rating,
        String content,
        List<Long> keywords,
        // 원본(최대 1080px). 이미지를 확대해서 볼 때 사용한다
        List<String> images,
        // 목록 렌더링용 썸네일(최대 400px). images 와 같은 순서다
        List<String> thumbnails,
        int likeCount,
        LocalDateTime date
) {

}
