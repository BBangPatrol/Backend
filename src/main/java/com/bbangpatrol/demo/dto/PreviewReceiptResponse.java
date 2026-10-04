package com.bbangpatrol.demo.dto;

/**
 * @param imageUrl R2 public URL. 클라이언트가 내려받아 영수증 인증에 그대로 올린다.
 * @param fileName 내려받은 파일을 다시 업로드할 때 쓸 이름. 확장자가 없으면 R2Service 확장자 검증에 걸린다.
 */
public record PreviewReceiptResponse(
        String imageUrl,
        String fileName
) {
}
