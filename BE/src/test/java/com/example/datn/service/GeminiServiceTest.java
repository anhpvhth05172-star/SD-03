package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GeminiServiceTest {

    @Test
    void defaultEndpointUsesAvailableFlashLiteModel() throws Exception {
        GeminiService service = new GeminiService("", "", new ObjectMapper());
        Field urlField = GeminiService.class.getDeclaredField("url");
        urlField.setAccessible(true);

        assertThat(urlField.get(service)).isEqualTo(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"
        );
    }
}
