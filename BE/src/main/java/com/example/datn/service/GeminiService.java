package com.example.datn.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Gọi Gemini API để trả lời câu hỏi về sản phẩm PolyShoes.
 * Key đọc từ env GEMINI_API_KEY (không hardcode).
 */
@Service
@Slf4j
public class GeminiService {

    private static final String URL_DEFAULT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final int TIMEOUT_MS = 15_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String url;

    public GeminiService(
        @Value("${gemini.api.key:}") String apiKey,
        @Value("${gemini.api.url:" + URL_DEFAULT + "}") String url,
        ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.url = url == null || url.isBlank() ? URL_DEFAULT : url.trim();
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(TIMEOUT_MS));
        factory.setReadTimeout(Duration.ofMillis(TIMEOUT_MS));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public boolean daCauHinh() {
        return !apiKey.isEmpty();
    }

    /**
     * Hỏi Gemini; trả về nội dung text hoặc ném RuntimeException nếu lỗi.
     */
    public String hoi(String cauHoi, String boSungSanPham) {
        if (!daCauHinh()) {
            throw new IllegalStateException("Chưa cấu hình GEMINI_API_KEY");
        }
        try {
            String systemPrompt =
                "Bạn là trợ lý tư vấn giày dép của cửa hàng PolyShoes. "
                    + "Trả lời ngắn gọn, thân thiện, bằng tiếng Việt. "
                    + "Chỉ tư vấn dựa trên thông tin sản phẩm được cung cấp bên dưới; "
                    + "nếu không có thông tin phù hợp, hãy khuyên khách liên hệ nhân viên. "
                    + "Không bịa giá hay tồn kho không có trong dữ liệu.\n\n"
                    + "Dữ liệu sản phẩm:\n"
                    + (boSungSanPham == null ? "(chưa có)" : boSungSanPham);

            Map<String, Object> body = Map.of(
                "contents",
                List.of(Map.of("parts", List.of(Map.of("text", cauHoi)))),
                "systemInstruction",
                Map.of("parts", List.of(Map.of("text", systemPrompt)))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            String raw = restClient
                .post()
                .uri(url)
                .headers(h -> h.addAll(headers))
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText("").trim();
                }
            }
            throw new IllegalStateException("Gemini không trả về nội dung hợp lệ");
        } catch (RuntimeException e) {
            log.warn("Gemini loi: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Gemini loi doc phan hoi: {}", e.getMessage());
            throw new IllegalStateException("Không kết nối được Gemini", e);
        }
    }
}
