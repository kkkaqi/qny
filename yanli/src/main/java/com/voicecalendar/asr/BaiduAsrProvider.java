package com.voicecalendar.asr;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 百度语音识别服务实现
 *
 * 使用文档: https://ai.baidu.com/tech/speech
 * 需要在百度AI开放平台创建应用，获取 AppID / API Key / Secret Key
 */
@Component
@ConditionalOnProperty(name = "voice.asr.provider", havingValue = "baidu")
@Slf4j
public class BaiduAsrProvider implements AsrProvider {

    private static final String ASR_URL = "https://vop.baidu.com/server_api";
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";

    private final OkHttpClient client = new OkHttpClient();

    @Value("${voice.baidu.app-id}")
    private String appId;

    @Value("${voice.baidu.api-key}")
    private String apiKey;

    @Value("${voice.baidu.secret-key}")
    private String secretKey;

    private String cachedToken;
    private long tokenExpireTime;

    @Override
    public String recognize(byte[] audioData, String format, int sampleRate) throws Exception {
        if (isMockKey()) {
            log.warn("百度 ASR 未配置有效密钥，返回空结果");
            return null;
        }

        String token = getAccessToken();
        String base64Audio = Base64.getEncoder().encodeToString(audioData);

        // 构建请求 JSON
        String json = String.format(
                "{\"format\":\"%s\",\"rate\":%d,\"channel\":1,\"cuid\":\"voice-calendar\"," +
                "\"token\":\"%s\",\"speech\":\"%s\",\"len\":%d,\"dev_pid\":1537}",
                format, sampleRate, token, base64Audio, audioData.length);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(ASR_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("百度 ASR 请求失败: {}", response.code());
                return null;
            }
            String respBody = response.body().string();
            log.info("百度 ASR 返回: {}", respBody);

            // 从返回 JSON 中提取 result (简单提取，生产环境建议用 Jackson 完整解析)
            if (respBody.contains("\"result\":[\"")) {
                int start = respBody.indexOf("\"result\":[\"") + 11;
                int end = respBody.indexOf("\"]", start);
                if (end > start) {
                    return respBody.substring(start, end);
                }
            }
            if (respBody.contains("\"err_msg\":\"success.\"")) {
                return null; // 识别为空
            }
        }

        return null;
    }

    @Override
    public String getProviderName() {
        return "百度语音识别";
    }

    /**
     * 获取百度 API Access Token
     */
    private String getAccessToken() throws IOException {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedToken;
        }

        String url = TOKEN_URL + "?grant_type=client_credentials&client_id="
                + apiKey + "&client_secret=" + secretKey;

        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String body = response.body().string();
                if (body.contains("\"access_token\":\"")) {
                    int start = body.indexOf("\"access_token\":\"") + 17;
                    int end = body.indexOf("\"", start);
                    cachedToken = body.substring(start, end);
                    tokenExpireTime = System.currentTimeMillis() + 25 * 24 * 3600 * 1000L;
                    log.info("百度 ASR Token 获取成功");
                    return cachedToken;
                }
            }
        }
        throw new IOException("获取百度 Access Token 失败");
    }

    private boolean isMockKey() {
        return apiKey == null || apiKey.startsWith("your-") || apiKey.isBlank();
    }
}
