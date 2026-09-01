package com.bbangpatrol.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MeResponse {

    private Long id;

    private String userNickname;

    private String imageUrl;
}
