package com.voicecalendar.llm;

/**
 * LLM 服务提供者接口
 * 支持通义千问、DeepSeek 等兼容 OpenAI API 的大模型
 */
public interface LlmProvider {

    /**
     * 发送对话请求，返回 LLM 响应文本
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return LLM 返回的文本
     */
    String chat(String systemPrompt, String userMessage) throws Exception;

    /** 服务商名称 */
    String getProviderName();

    /** 是否可用（配置了有效 API Key） */
    boolean isAvailable();
}
