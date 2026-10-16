package com.example.datn.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenApiClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    public OpenApiClient(
        @Value("${app.dia-chi.open-api-url:https://provinces.open-api.vn}") String baseUrl
    ) {
        this.baseUrl = baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    public String get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "PolyShoes-DATN/1.0")
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalArgumentException(
                    "Không tra cứu được dữ liệu địa chỉ hành chính (HTTP "
                        + response.statusCode() + "). Vui lòng thử lại sau");
            }
            return response.body();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Không tra cứu được dữ liệu địa chỉ hành chính. Vui lòng thử lại sau");
        }
    }
}
