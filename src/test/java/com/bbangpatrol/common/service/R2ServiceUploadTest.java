package com.bbangpatrol.common.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 사용자가 리뷰 사진을 올리는 실제 경로({@code ReviewService.uploadAndSaveImages} →
 * {@code R2Service.uploadImageWithThumbnail})가 원본과 썸네일을 모두 올리는지 검증한다.
 * S3Client 만 목으로 두고 리사이즈는 실제 코드를 그대로 태운다.
 */
class R2ServiceUploadTest {

    private static final String BUCKET = "test-bucket";

    private final S3Client s3Client = mock(S3Client.class);
    private final R2Service r2Service = new R2Service(s3Client);

    R2ServiceUploadTest() {
        ReflectionTestUtils.setField(r2Service, "bucketName", BUCKET);
        ReflectionTestUtils.setField(r2Service, "publicBaseUrl", "https://cdn.test");
    }

    @Test
    @DisplayName("리뷰 사진 업로드는 원본(1080px)과 썸네일(400px)을 함께 올린다")
    void uploadsOriginalAndThumbnail() throws IOException {
        MockMultipartFile file = photo("IMG_0001.JPG", "image/jpeg", 4032, 3024);

        UploadedImage uploaded = r2Service.uploadImageWithThumbnail(file, "reviews/12");

        Captured c = capture(2);

        // 두 key 를 모두 반환한다. DB 에는 이 두 값이 그대로 저장된다
        assertThat(uploaded.key()).matches("reviews/12/[0-9a-f-]{36}[.]jpg");
        assertThat(uploaded.thumbnailKey()).isEqualTo(R2Service.thumbnailKey(uploaded.key()));
        assertThat(uploaded.thumbnailKey()).endsWith("_thumb.jpg");
        assertThat(c.keys()).containsExactly(uploaded.key(), uploaded.thumbnailKey());

        // 둘 다 JPEG + 장기 캐시 헤더
        assertThat(c.requests().get(0).contentType()).isEqualTo("image/jpeg");
        assertThat(c.requests().get(1).contentType()).isEqualTo("image/jpeg");
        assertThat(c.requests()).allSatisfy(r ->
                assertThat(r.cacheControl()).isEqualTo("public, max-age=31536000, immutable"));

        // 실제 픽셀 크기 확인
        BufferedImage origin = read(c.bytes().get(0));
        BufferedImage thumb = read(c.bytes().get(1));
        assertThat(Math.max(origin.getWidth(), origin.getHeight())).isEqualTo(1080);
        assertThat(Math.max(thumb.getWidth(), thumb.getHeight())).isEqualTo(400);

        // 원본 업로드 용량보다 작아져야 한다
        assertThat(c.bytes().get(0).length).isLessThan(file.getBytes().length);
        assertThat(c.bytes().get(1).length).isLessThan(c.bytes().get(0).length);
    }

    @Test
    @DisplayName("사진 여러 장을 올리면 장마다 원본+썸네일 한 쌍이 생긴다")
    void uploadsPairPerFile() throws IOException {
        for (int i = 1; i <= 3; i++) {
            UploadedImage uploaded = r2Service.uploadImageWithThumbnail(
                    photo("p" + i + ".jpg", "image/jpeg", 2048, 1536), "reviews/12");
            assertThat(uploaded.thumbnailKey()).isNotNull();
        }

        Captured c = capture(6);

        assertThat(c.keys()).hasSize(6);
        for (int i = 0; i < 6; i += 2) {
            assertThat(c.keys().get(i + 1)).isEqualTo(R2Service.thumbnailKey(c.keys().get(i)));
        }
        // key 가 전부 달라야 한다 (UUID)
        assertThat(c.keys()).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("gif 는 원본을 그대로 두고 썸네일만 JPEG 로 만든다")
    void keepsGifOriginal() throws IOException {
        byte[] gif = encode(new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB), "gif");
        MockMultipartFile file = new MockMultipartFile("reviewImages", "anim.gif", "image/gif", gif);

        UploadedImage uploaded = r2Service.uploadImageWithThumbnail(file, "reviews/12");

        Captured c = capture(2);

        assertThat(uploaded.key()).endsWith(".gif");
        // 애니메이션이 죽지 않도록 원본 바이트를 손대지 않는다
        assertThat(c.bytes().get(0)).isEqualTo(gif);
        assertThat(c.requests().get(0).contentType()).isEqualTo("image/gif");
        // 썸네일은 JPEG
        assertThat(uploaded.thumbnailKey()).endsWith("_thumb.jpg");
        assertThat(c.keys().get(1)).isEqualTo(uploaded.thumbnailKey());
        assertThat(c.requests().get(1).contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("프로필 이미지는 썸네일 없이 한 장만 올린다")
    void profileUploadsSingleObject() throws IOException {
        // 웹 클라이언트가 확장자 없는 blob 으로 올려도 동작해야 한다
        MockMultipartFile file = photo("blob", "image/jpeg", 3000, 3000);

        String key = r2Service.uploadResizedImage(file, "users/6", 400, 0.8f);

        Captured c = capture(1);

        assertThat(key).matches("users/6/[0-9a-f-]{36}[.]jpg");
        assertThat(c.keys()).containsExactly(key);
        assertThat(Math.max(read(c.bytes().get(0)).getWidth(), read(c.bytes().get(0)).getHeight()))
                .isEqualTo(400);
    }

    @Test
    @DisplayName("썸네일 업로드가 실패하면 이미 올린 원본을 지운다")
    void cleansUpOriginalWhenThumbnailFails() throws IOException {
        // 첫 번째 putObject(원본)는 성공, 두 번째(썸네일)에서 실패시킨다
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(null)
                .thenThrow(S3Exception.builder().message("boom").build());

        assertThatThrownBy(() ->
                r2Service.uploadImageWithThumbnail(photo("x.jpg", "image/jpeg", 2048, 1536), "reviews/12"))
                .isInstanceOf(S3Exception.class);

        // 호출측은 key 를 받지 못하므로 스스로 치우지 못한다. R2Service 가 원본을 지워야 한다
        ArgumentCaptor<DeleteObjectRequest> deleted = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleted.capture());
        assertThat(deleted.getValue().key()).matches("reviews/12/[0-9a-f-]{36}[.]jpg");
    }

    // --- helpers ---

    private record Captured(List<PutObjectRequest> requests, List<byte[]> bytes) {
        List<String> keys() {
            return requests.stream().map(PutObjectRequest::key).toList();
        }
    }

    private Captured capture(int expectedCalls) {
        ArgumentCaptor<PutObjectRequest> req = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client, times(expectedCalls)).putObject(req.capture(), body.capture());

        List<byte[]> bytes = body.getAllValues().stream().map(b -> {
            try {
                return b.contentStreamProvider().newStream().readAllBytes();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }).toList();
        return new Captured(req.getAllValues(), bytes);
    }

    /** 카메라 사진에 가까운 그라데이션 JPEG */
    private MockMultipartFile photo(String name, String contentType, int w, int h) throws IOException {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(240, 200, 150), w, h, new Color(60, 40, 20)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(200, 160, 90));
        for (int i = 0; i < 30; i++) g.fillOval(i * 60, (i * 97) % h, 300, 240);
        g.dispose();
        return new MockMultipartFile("reviewImages", name, contentType, encode(image, "jpg"));
    }

    private byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private BufferedImage read(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
}
