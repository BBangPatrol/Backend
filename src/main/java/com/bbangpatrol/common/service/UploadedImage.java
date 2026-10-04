package com.bbangpatrol.common.service;

/**
 * 이미지 한 장을 올린 결과. 두 값 모두 R2 오브젝트 키이며 public URL 이 아니다.
 *
 * @param key          원본 key
 * @param thumbnailKey 목록용 썸네일 key. 썸네일을 만들지 않았으면 null
 */
public record UploadedImage(
        String key,
        String thumbnailKey
) {
}
