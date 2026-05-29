package com.voicecalendar.service;

import com.voicecalendar.asr.AsrProvider;
import com.voicecalendar.model.dto.EventResponse;
import com.voicecalendar.model.dto.VoiceCommandResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 语音服务 —— 串联 ASR 识别 → NLP 解析 → 事件操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceService {

    private final AsrProvider asrProvider;
    private final NLPService nlpService;
    private final EventService eventService;

    /**
     * 处理语音音频流：ASR识别 → NLP解析 → 执行操作
     */
    public VoiceCommandResult processAudio(byte[] audioData, String format, int sampleRate) {
        try {
            // Step 1: ASR 语音转文字
            String text = asrProvider.recognize(audioData, format, sampleRate);
            if (text == null || text.isBlank()) {
                return VoiceCommandResult.builder()
                        .originalText(null)
                        .intent("UNKNOWN")
                        .success(false)
                        .errorMessage("语音识别未返回结果，请重试或使用文本输入")
                        .build();
            }

            log.info("ASR 识别结果: {}", text);

            // Step 2: NLP 解析意图并执行
            return processText(text);

        } catch (Exception e) {
            log.error("语音处理异常", e);
            return VoiceCommandResult.builder()
                    .intent("UNKNOWN")
                    .success(false)
                    .errorMessage("语音处理出错: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 处理文字指令（前端 Web Speech API 降级方案 / 手动输入）
     */
    public VoiceCommandResult processText(String text) {
        // Step 1: NLP 解析
        VoiceCommandResult result = nlpService.parse(text);

        if (!result.getSuccess() || "UNKNOWN".equals(result.getIntent())) {
            return result;
        }

        // Step 2: 根据意图执行操作
        try {
            switch (result.getIntent()) {
                case "ADD_EVENT" -> {
                    if (result.getEventData() != null) {
                        EventResponse created = eventService.createEvent(result.getEventData());
                        result.setConfirmationMessage(
                                result.getConfirmationMessage() + " (ID: " + created.getId() + ")");
                    }
                }
                case "DELETE_EVENT" -> {
                    // 有具体 ID 则直接删除
                    if (result.getDeleteEventId() != null) {
                        eventService.deleteEvent(result.getDeleteEventId());
                    } else if (result.getDeleteKeyword() != null) {
                        // 按关键词搜索，返回候选列表让用户选择
                        List<EventResponse> candidates = eventService.searchEvents(result.getDeleteKeyword());
                        result.setEvents(candidates);
                        if (candidates.isEmpty()) {
                            result.setConfirmationMessage("未找到与「" + result.getDeleteKeyword() + "」相关的事件");
                            result.setNeedsConfirmation(false);
                        } else {
                            result.setConfirmationMessage(
                                    "找到 " + candidates.size() + " 个相关事件，请选择要删除的项目");
                        }
                    }
                }
                case "QUERY_EVENTS" -> {
                    List<EventResponse> events;
                    String queryDateStr = result.getQueryDate();
                    if (queryDateStr != null && !queryDateStr.isBlank()) {
                        java.time.LocalDate date = java.time.LocalDate.parse(queryDateStr,
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        events = eventService.getEventsByDate(date);
                    } else {
                        events = eventService.getAllEvents();
                    }
                    result.setEvents(events);
                    if (events.isEmpty()) {
                        result.setConfirmationMessage(result.getConfirmationMessage() + " 暂无事件。");
                    } else {
                        result.setConfirmationMessage(
                                result.getConfirmationMessage() + " 共 " + events.size() + " 个事件。");
                    }
                }
            }
        } catch (Exception e) {
            log.error("执行语音指令失败: {}", result.getIntent(), e);
            result.setSuccess(false);
            result.setErrorMessage("操作执行失败: " + e.getMessage());
        }

        return result;
    }
}
