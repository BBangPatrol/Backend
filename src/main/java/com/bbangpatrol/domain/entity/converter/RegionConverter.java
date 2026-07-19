package com.bbangpatrol.domain.entity.converter;

import com.bbangpatrol.domain.entity.enums.Region;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter(autoApply = true)
public class RegionConverter implements AttributeConverter<Region, String> {

    @Override
    public String convertToDatabaseColumn(Region region) {
        return region == null ? null : region.getValue();
    }

    @Override
    public Region convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(Region.values())
                .filter(region -> region.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 지역 값입니다: " + value));
    }
}
