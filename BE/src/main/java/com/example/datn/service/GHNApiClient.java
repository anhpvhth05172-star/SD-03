package com.example.datn.service;

import com.example.datn.config.GHNConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class GHNApiClient {

    private final GHNConfig cauHinh;
    private final HttpClient httpClient;

    public GHNApiClient(GHNConfig cauHinh) {
        this.cauHinh = cauHinh;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(cauHinh.getTimeoutMs()))
            .build();
    }

    public String get(String path) {
        return gui("GET", path, null);
    }

    public String post(String path, String body) {
        return gui("POST", path, body);
    }

    private String gui(String method, String path, String body) {
        cauHinh.kiemTraCauHinh();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(cauHinh.getApiUrl() + path))
                .timeout(Duration.ofMillis(cauHinh.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Token", cauHinh.getToken())
                .header("ShopId", cauHinh.getShopId());
            if ("POST".equals(method)) {
                builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            } else {
                builder.GET();
            }
            HttpResponse<String> res = httpClient.send(
                builder.build(), HttpResponse.BodyHandlers.ofString()
            );
            if (res.statusCode() == 401 || res.statusCode() == 403) {
                throw new IllegalArgumentException(
                    "GHN từ chối xác thực. Vui lòng kiểm tra GHN_TOKEN và GHN_SHOP_ID");
            }
            if (res.statusCode() >= 500) {
                throw new IllegalArgumentException("Máy chủ GHN gặp lỗi tạm thời. Vui lòng thử lại");
            }
            return res.body();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            throw new IllegalArgumentException("Kết nối GHN quá thời gian chờ. Vui lòng thử lại");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Đã hủy yêu cầu tính phí vận chuyển");
        } catch (IOException e) {
            throw new IllegalArgumentException("Không kết nối được máy chủ GHN. Vui lòng thử lại");
        }
    }
}
