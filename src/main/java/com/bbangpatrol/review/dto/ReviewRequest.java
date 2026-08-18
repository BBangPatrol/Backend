package com.bbangpatrol.review.dto;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public record ReviewRequest(
        Integer rating,
        String content,
        List<Long> keywordIds,
        List<MultipartFile> reviewImages
) {

}
