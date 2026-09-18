package com.bbangpatrol.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record PreviewReceiptRequest(
        @NotBlank(message = "코드를 입력해 주세요.")
        String code
) {
}
