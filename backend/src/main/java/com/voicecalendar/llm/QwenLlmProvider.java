package com.voicecalendar.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 通义千问 LLM 实现（兼容 OpenAI API）
 */
@Component
@Slf4j
public class QwenLlmProvider implements LlmProvider {

    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${voice.llm.qwen.api-key:}")
    private String apiKey;

    @Value("${voice.llm.qwen.model:qwen-plus}")
    private String model;

    @Override
    public String chat(String systemPrompt, String userMessage) throws Exception {
        if (!isAvailable()) {
            throw new RuntimeException("通义千问 API Key 未配置");
        }

        // 用 Map 构建 JSON，避免转义问题
        var sysMsg = Map.of("role", "system", "content", systemPrompt);
        var usrMsg = Map.of("role", "user", "content", userMessage);
        var body = Map.of(
                "model", model,
                "messages", new Object[]{sysMsg, usrMsg},
                "temperature", 0.1,
                "max_tokens", 800
        );

        String json = mapper.writeValueAsString(body);
        log.debug("Qwen 请求: {}", json);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(json, JSON_MEDIA))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                log.error("Qwen API 错误 {}: {}", response.code(), errBody);
                throw new IOException("Qwen API error " + response.code() + ": " + errBody);
            }

            String respBody = response.body().string();
            log.info("Qwen 响应: {}", respBody);

            // 用 Jackson 正确解析 OpenAI 格式响应
            JsonNode root = mapper.readTree(respBody);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                log.error("Qwen 响应无 choices: {}", respBody);
                return null;
            }

            JsonNode contentNode = choices.get(0).get("message").get("content");
            if (contentNode == null) {
                log.error("Qwen 响应无 content: {}", respBody);
                return null;
            }

            return contentNode.asText();
        }
    }

    @Override
    public String getProviderName() {
        return "通义千问 (" + model + ")";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("your-");
    }
}
