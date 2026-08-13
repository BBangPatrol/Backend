package com.bbangpatrol.ocr.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "ocr")
public record OcrProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {}