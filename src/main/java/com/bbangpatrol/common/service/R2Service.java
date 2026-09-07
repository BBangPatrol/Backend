package com.bbangpatrol.common.service;

import lombok.RequiredArgsConstructor;
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
     * 반환값은 원본 key 이며 썸네일 key 는 {@link #thumbnailKey(String)} 로 파생한다.
     * 즉 DB 에는 원본 key 만 저장하면 되므로 스키마 변경이 필요 없다.
     */
    public String uploadImageWithThumbnail(MultipartFile file, String folder) throws IOException {
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

        putBytes(thumbnailKey(key), ImageResizer.toJpeg(source, THUMBNAIL_MAX_DIMENSION, THUMBNAIL_QUALITY), "image/jpeg");
        return key;
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
     * 원본 key 에서 썸네일 key 를 파생한다.
     * 썸네일 없이 올라간 과거 이미지는 이 key 가 404 이므로 클라이언트에서 원본으로 폴백해야 한다.
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

    // 원본과 함께 올라간 썸네일까지 지운다. 썸네일이 없으면 R2 삭제는 그냥 성공 처리된다
    public void deleteImageWithThumbnail(String key) {
        deleteFile(key);
        deleteFile(thumbnailKey(key));
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

    /**
     * 목록용 썸네일의 public URL. 썸네일이 아직 없는 key 는 404 이므로
     * 클라이언트에서 원본으로 폴백해야 한다.
     */
    public String getThumbnailPublicUrl(String key) {
        return getPublicUrl(thumbnailKey(key));
    }

    // Public 버킷 + CDN
    public String getPublicUrl(String key) {
        if (key == null || key.isBlank()) return null;
        return publicBaseUrl.replaceAll("/$", "") + "/" + key.replaceAll("^/", "");
    }
}