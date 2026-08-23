package com.bbangpatrol.bakery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiResponse(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Body body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<TourItem> item) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TourItem(
            String contentid,
            String contenttypeid,
            String title,
            String addr1,
            String addr2,
            String firstimage,
            String firstimage2,
            String mapx,
            String mapy,
            String dist,
            String tel
    ) {}
}
