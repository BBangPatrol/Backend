package com.bbangpatrol.review.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record ReviewUpdatedRequest(
        Integer rating,
        String content,
        List<Long> deleteKeywordIds,
        List<Long> keywordIds,
        List<Long> deleteImages,
        List<MultipartFile> reviewImages
) {
}
