package com.bbangpatrol.common.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 썸네일 key 를 원본 key 에서 파생하기 때문에 (DB 에 썸네일 컬럼을 두지 않는다)
 * 이 규칙이 깨지면 목록의 모든 이미지가 404 가 된다.
 */
class R2ServiceThumbnailKeyTest {

    @Test
    @DisplayName("확장자를 _thumb.jpg 로 바꾼다")
    void replacesExtension() {
        assertEquals("reviews/12/abc_thumb.jpg", R2Service.thumbnailKey("reviews/12/abc.jpg"));
        assertEquals("reviews/12/abc_thumb.jpg", R2Service.thumbnailKey("reviews/12/abc.gif"));
        assertEquals("reviews/12/a.b.c_thumb.jpg", R2Service.thumbnailKey("reviews/12/a.b.c.png"));
    }

    @Test
    @DisplayName("확장자가 없으면 뒤에 붙인다")
    void appendsWhenNoExtension() {
        assertEquals("reviews/12/abc_thumb.jpg", R2Service.thumbnailKey("reviews/12/abc"));
    }

    @Test
    @DisplayName("디렉터리 이름에만 점이 있으면 파일 이름 뒤에 붙인다")
    void ignoresDotInDirectoryName() {
        assertEquals("reviews/v1.2/abc_thumb.jpg", R2Service.thumbnailKey("reviews/v1.2/abc"));
    }

    @Test
    @DisplayName("null 과 공백은 그대로 반환한다")
    void passesThroughBlank() {
        assertNull(R2Service.thumbnailKey(null));
        assertEquals("", R2Service.thumbnailKey(""));
    }
}
