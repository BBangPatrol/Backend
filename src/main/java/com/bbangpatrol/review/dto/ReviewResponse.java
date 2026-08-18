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
        List<Integer> keywords,
        List<String> images,
        int likeCount,
        LocalDateTime date
) {

}
