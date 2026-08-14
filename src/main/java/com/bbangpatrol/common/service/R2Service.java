package com.bbangpatrol.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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

    // 이미지 전용 업로드: 확장자 검증 후 R2에 업로드
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("올바르지 않은 파일 이름입니다.");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }
        return uploadFile(file, folder);
    }

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String key = folder + "/" + UUID.randomUUID() + "_" +file.getOriginalFilename();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );
        return key;
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