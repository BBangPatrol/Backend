package com.bbangpatrol.visit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VisitResponse {
    private Long visitId;
    private Long visitDetailId;
    private int point;
}
