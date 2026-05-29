package com.voicecalendar.asr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock ASR 提供者 —— 用于开发和演示，不需要配置云服务密钥
 *
 * 前端可以选择使用浏览器内置 Web Speech API 作为替代方案，
 * 直接将识别文字通过 /api/voice/text 接口发送到后端做 NLP 解析。
 */
@Component
@ConditionalOnProperty(name = "voice.asr.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockAsrProvider implements AsrProvider {

    @Override
    public String recognize(byte[] audioData, String format, int sampleRate) {
        log.info("Mock ASR: 收到 {} 字节音频数据 ({} / {}Hz)，返回 null 以触发前端降级",
                audioData.length, format, sampleRate);
        // 返回 null 表示 ASR 不可用，前端应降级使用 Web Speech API
        return null;
    }

    @Override
    public String getProviderName() {
        return "Mock (演示模式 —— 请在前端使用语音输入文本)";
    }
}
