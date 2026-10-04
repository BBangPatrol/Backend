package com.bbangpatrol.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import com.bbangpatrol.common.util.ImageResizer;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class R2Service {

    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Value("${cloudflare.r2.account-id}")
    private String accountId;

    @Value("${cloudflare.r2.public-base-url}")
    private String publicBaseUrl;

    // 이미지로 허용할 확장자 (기존 FileService의 검증 로직을 이관)
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    // 리사이즈하지 않고 원본을 유지할 확장자. gif 는 리사이즈하면 애니메이션이 첫 프레임으로 죽는다
    private static final Set<String> SKIP_RESIZE_EXTENSIONS = Set.of(".gif");

    // 원본은 1080px, 목록용 썸네일은 400px 로 줄인다
    private static final int ORIGIN_MAX_DIMENSION = 1080;
    private static final int THUMBNAIL_MAX_DIMENSION = 400;
    private static final float ORIGIN_QUALITY = 0.8f;
    private static final float THUMBNAIL_QUALITY = 0.75f;

    private static final String THUMBNAIL_SUFFIX = "_thumb.jpg";

    // key 에 UUID 가 들어가 내용이 바뀌지 않으므로 최대 기간 캐싱 + immutable 이 안전하다
    private static final String IMMUTABLE_CACHE_CONTROL = "public, max-age=31536000, immutable";

    /**
     * 이미지 전용 업로드: 확장자 검증 후 R2 에 원본 그대로 업로드.
     *
     * @deprecated 원본을 그대로 올리면 조회 때 목록 로딩이 느려진다.
     *             {@link #uploadImageWithThumbnail(MultipartFile, String)} 또는
     *             {@link #uploadResizedImage(MultipartFile, String, int, float)} 를 쓸 것.
     */
    @Deprecated
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        validateImageExtension(file);
        return uploadFile(file, folder);
    }

    /**
     * 이미지 업로드: 원본을 1080px JPEG 로 줄여 올리고, 목록용 400px 썸네일을 함께 올린다.
     * 두 key 를 모두 반환하므로 호출측이 DB 에 그대로 저장한다.
     * (키 규칙으로 파생하지 않는 이유: 썸네일이 없는 경우를 표현할 수 없어 404 URL 이 나가게 된다.)
     */
    public UploadedImage uploadImageWithThumbnail(MultipartFile file, String folder) throws IOException {
        String extension = validateImageExtension(file);
        byte[] source = file.getBytes();

        String key;
        if (SKIP_RESIZE_EXTENSIONS.contains(extension)) {
            key = folder + "/" + UUID.randomUUID() + extension;
            putBytes(key, source, file.getContentType());
        } else {
            key = folder + "/" + UUID.randomUUID() + ".jpg";
            putBytes(key, ImageResizer.toJpeg(source, ORIGIN_MAX_DIMENSION, ORIGIN_QUALITY), "image/jpeg");
        }

        String thumbnailKey = thumbnailKey(key);
        try {
            putBytes(thumbnailKey, ImageResizer.toJpeg(source, THUMBNAIL_MAX_DIMENSION, THUMBNAIL_QUALITY), "image/jpeg");
        } catch (Exception e) {
            // 원본은 이미 올라갔는데 호출측은 key 를 받지 못한다. 여기서 치우지 않으면 고아 객체로 남는다
            try {
                deleteFile(key);
            } catch (Exception cleanupFailure) {
                log.error("썸네일 실패 후 원본 정리 실패. key={}", key, cleanupFailure);
            }
            throw e;
        }
        return new UploadedImage(key, thumbnailKey);
    }

    /**
     * 썸네일 없이 리사이즈해서 한 장만 올린다. 프로필 이미지처럼 원래 작게만 쓰이는 경우에 쓴다.
     * 결과는 항상 JPEG 이므로 파일 이름의 확장자를 보지 않는다.
     * (웹 클라이언트는 확장자 없는 blob 으로 업로드하는 경우가 있어 Content-Type 검증은 호출측 책임이다.)
     */
    public String uploadResizedImage(MultipartFile file, String folder, int maxDimension, float quality) throws IOException {
        String key = folder + "/" + UUID.randomUUID() + ".jpg";
        putBytes(key, ImageResizer.toJpeg(file.getBytes(), maxDimension, quality), "image/jpeg");
        return key;
    }

    /**
     * 업로드 시 썸네일에 붙일 key 를 만든다. 조회는 DB 에 저장된 값을 쓰므로 여기서만 사용한다.
     * (V7 마이그레이션이 기존 sig_image 를 채울 때도 같은 규칙을 SQL 로 재현한다.)
     */
    public static String thumbnailKey(String key) {
        if (key == null || key.isBlank()) return key;

        int dot = key.lastIndexOf('.');
        int slash = key.lastIndexOf('/');
        // 확장자가 없거나 디렉터리 이름에만 점이 있는 경우
        if (dot < 0 || dot < slash) return key + THUMBNAIL_SUFFIX;
        return key.substring(0, dot) + THUMBNAIL_SUFFIX;
    }

    // 확장자를 검증하고 소문자 확장자(예: ".jpg")를 반환한다
    private String validateImageExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("올바르지 않은 파일 이름입니다.");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }
        return extension;
    }

    private void putBytes(String key, byte[] bytes, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .cacheControl(IMMUTABLE_CACHE_CONTROL)
                        .build(),
                RequestBody.fromBytes(bytes)
        );
    }

    /**
     * 원본 파일을 변환 없이 업로드한다. 이미지가 아닌 파일에만 쓸 것.
     *
     * @deprecated 이미지라면 리사이즈하는 쪽을 쓸 것. {@link #uploadImageWithThumbnail(MultipartFile, String)} 참고.
     */
    @Deprecated
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String key = folder + "/" + UUID.randomUUID() + "_" +file.getOriginalFilename();
        putBytes(key, file.getBytes(), file.getContentType());
        return key;
    }

    /** 원본과 썸네일을 함께 지운다. thumbnailKey 가 null 이면 원본만 지운다 */
    public void deleteImage(String key, String thumbnailKey) {
        deleteFile(key);
        if (thumbnailKey != null && !thumbnailKey.isBlank()) {
            deleteFile(thumbnailKey);
        }
    }

    public void deleteFile(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()
        );
    }

    public String getFileUrl(String key) {
        return "https://" + accountId + ".r2.cloudflarestorage.com/" + bucketName + "/" + key;
    }

    // Public 버킷 + CDN
    public String getPublicUrl(String key) {
        if (key == null || key.isBlank()) return null;
        return publicBaseUrl.replaceAll("/$", "") + "/" + key.replaceAll("^/", "");
    }
}