package com.bbangpatrol.visit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VisitRequest {

    private Integer totalAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String verificationToken;
}
