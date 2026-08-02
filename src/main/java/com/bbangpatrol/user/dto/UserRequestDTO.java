package com.bbangpatrol.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserRequestDTO {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditNicknameDTO {
        @NotBlank
        @Size(max = 50)
        String nickname;
    }
}
