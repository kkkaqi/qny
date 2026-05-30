package com.voicecalendar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicecalendar.model.dto.EventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${voice.llm.qwen.api-key:}")
    private String apiKey;

    @Value("${voice.llm.qwen.model:qwen-plus}")
    private String model;

    private final OkHttpClient client = new OkHttpClient();
    private final EventService eventService;

    private static final int MAX_TURNS = 20;
    private static final String PREFIX = "chat:history:";
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json");

    public List<Map<String, String>> getRoles() {
        return List.of(
                Map.of("id", "default", "name", "AI助手", "prompt", "你是用户的AI智能助手，友好、热情地回答用户的问题。"),
                Map.of("id", "friend", "name", "老朋友", "prompt", "你是用户认识多年的老朋友，说话随意亲切，可以开玩笑、吐槽，像朋友一样聊天。"),
                Map.of("id", "counselor", "name", "心理顾问", "prompt", "你是一位温和的心理咨询师，善于倾听和共情，提供情感支持和疏导。"),
                Map.of("id", "teacher", "name", "老师", "prompt", "你是一位知识渊博的老师，用通俗易懂的方式解答问题，耐心细致。"),
                Map.of("id", "custom", "name", "自定义", "prompt", "")
        );
    }

    @SuppressWarnings("unchecked")
    public String send(Long userId, String roleId, String message, String rolePrompt) throws Exception {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            return "请先配置通义千问 API Key（在 application.yml 中设置 voice.llm.qwen.api-key）";
        }

        String key = PREFIX + userId + ":" + (roleId != null ? roleId : "default");
        List<Map<String, String>> history = loadHistory(key);

        // 构建 messages 数组
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", rolePrompt != null && !rolePrompt.isBlank() ? rolePrompt : "你是用户的AI智能助手。"));
        for (Map<String, String> msg : history) {
            messages.add(Map.of("role", msg.get("role"), "content", msg.get("content")));
        }
        messages.add(Map.of("role", "user", "content", message));

        // 调用 LLM
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.8);
        body.put("max_tokens", 1000);

        String reqJson = mapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(reqJson, JSON_MEDIA))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        String reply;
        try (Response resp = client.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                log.error("Chat API error: {}", resp.code());
                return "抱歉，服务暂时不可用。";
            }
            String respBody = resp.body() != null ? resp.body().string() : "";
            reply = mapper.readTree(respBody).get("choices").get(0).get("message").get("content").asText();
        }

        // 保存历史
        history.add(Map.of("role", "user", "content", message));
        history.add(Map.of("role", "assistant", "content", reply));
        while (history.size() > MAX_TURNS * 2) { history.remove(0); history.remove(0); }
        redis.opsForValue().set(key, mapper.writeValueAsString(history), 7, TimeUnit.DAYS);

        return reply;
    }

    /** AI 周/月总结 */
    public String summarize(Long userId, String period, String date) throws Exception {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            return "请先配置千问 API Key";
        }

        java.time.LocalDate end = java.time.LocalDate.parse(date.substring(0, 10));
        java.time.LocalDate start = "week".equals(period) ? end.minusDays(7) : end.minusDays(30);
        List<EventResponse> events = eventService.getEventsByRange(userId, start.atStartOfDay(), end.atTime(java.time.LocalTime.MAX));

        if (events.isEmpty()) return "暂无事件";

        StringBuilder eventList = new StringBuilder();
        for (EventResponse e : events) {
            eventList.append("- ").append(e.getStartTime().toLocalDate()).append(" ").append(e.getTitle()).append("\n");
        }

        String prompt = "根据以下" + ("week".equals(period) ? "一周" : "一个月") + "的事件列表，用一句话概括（20字以内，口语化）：\n" + eventList;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", 0.7);
        body.put("max_tokens", 100);

        String reqJson = mapper.writeValueAsString(body);
        Request request = new Request.Builder().url(API_URL)
                .post(RequestBody.create(reqJson, JSON_MEDIA))
                .addHeader("Authorization", "Bearer " + apiKey).build();

        try (Response resp = client.newCall(request).execute()) {
            if (!resp.isSuccessful()) return "生成失败";
            String respBody = resp.body() != null ? resp.body().string() : "";
            return mapper.readTree(respBody).get("choices").get(0).get("message").get("content").asText();
        }
    }

    public void reset(Long userId, String roleId) {
        redis.delete(PREFIX + userId + ":" + (roleId != null ? roleId : "default"));
    }

    private List<Map<String, String>> loadHistory(String key) {
        try {
            String raw = redis.opsForValue().get(key);
            return raw != null ? mapper.readValue(raw, new TypeReference<List<Map<String, String>>>() {}) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
