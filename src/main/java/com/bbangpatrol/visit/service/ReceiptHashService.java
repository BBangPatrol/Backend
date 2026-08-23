package com.bbangpatrol.visit.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class ReceiptHashService {

    public String create(
            String businessNumber,
            String receiptNum,
            LocalDate date,
            Integer totalAmount
    ) {
        try {
            String raw = normalize(businessNumber)
                    + ":"
                    + receiptNum
                    + ":"
                    + date
                    + ":"
                    + totalAmount;

            // 식별 무결성 중복 비교에는 SHA-256 방식이 적합
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    raw.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("영수증 해시 생성 실패", e);
        }
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.replaceAll("[^0-9]", "");
    }
}