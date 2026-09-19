package com.bbangpatrol.ocr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 타임아웃 설정이 잘못되면 애플리케이션이 뜨다가 죽는다.
 * 기동 경로(@PostConstruct)를 그대로 불러서 properties 의 "5s" / "60s" 표기가 실제로 먹는지 본다.
 */
class GeminiClientTimeoutTest {

    @Test
    @DisplayName("설정값으로 RestClient 를 만든다")
    void buildsRestClientFromProperties() {
        GeminiClient client = new GeminiClient(new ObjectMapper());
        ReflectionTestUtils.setField(client, "connectTimeout", "5s");
        ReflectionTestUtils.setField(client, "readTimeout", "60s");

        assertThatCode(client::initRestClient).doesNotThrowAnyException();
        assertThat(ReflectionTestUtils.getField(client, "restClient")).isNotNull();
    }

    @Test
    @DisplayName("단위 없는 값(밀리초)도 받는다")
    void acceptsPlainMillis() {
        GeminiClient client = new GeminiClient(new ObjectMapper());
        ReflectionTestUtils.setField(client, "connectTimeout", "3000");
        ReflectionTestUtils.setField(client, "readTimeout", "30000");

        assertThatCode(client::initRestClient).doesNotThrowAnyException();
    }
}
