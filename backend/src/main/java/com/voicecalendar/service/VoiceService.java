package com.voicecalendar.service;

import com.voicecalendar.asr.AsrProvider;
import com.voicecalendar.model.dto.EventResponse;
import com.voicecalendar.model.dto.VoiceCommandResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceService {

    private final AsrProvider asrProvider;
    private final NLPService nlpService;
    private final EventService eventService;

    public VoiceCommandResult processAudio(Long userId, byte[] audioData, String format, int sampleRate) {
        try {
            String text = asrProvider.recognize(audioData, format, sampleRate);
            if (text == null || text.isBlank()) {
                return VoiceCommandResult.builder().intent("UNKNOWN").success(false).errorMessage("语音识别未返回结果，请重试").build();
            }
            return processText(userId, text);
        } catch (Exception e) {
            log.error("语音处理异常", e);
            return VoiceCommandResult.builder().intent("UNKNOWN").success(false).errorMessage("语音处理出错: " + e.getMessage()).build();
        }
    }

    public VoiceCommandResult processText(Long userId, String text) {
        VoiceCommandResult result = nlpService.parse(text);
        if (!result.getSuccess() || "UNKNOWN".equals(result.getIntent())) return result;
        try {
            switch (result.getIntent()) {
                case "ADD_EVENT" -> {
                    if (result.getEventData() != null) {
                        EventResponse created = eventService.createEvent(userId, result.getEventData());
                        result.setConfirmationMessage(result.getConfirmationMessage() + " (ID:" + created.getId() + ")");
                    }
                }
                case "DELETE_EVENT" -> {
                    if (result.getDeleteEventId() != null) {
                        eventService.deleteEvent(userId, result.getDeleteEventId());
                    } else if (result.getDeleteKeyword() != null) {
                        List<EventResponse> candidates = eventService.searchEvents(userId, result.getDeleteKeyword());
                        result.setEvents(candidates);
                        if (candidates.isEmpty()) { result.setConfirmationMessage("未找到相关事件"); result.setNeedsConfirmation(false); }
                        else { result.setConfirmationMessage("找到 " + candidates.size() + " 个相关事件，请选择"); }
                    }
                }
                case "QUERY_EVENTS" -> {
                    List<EventResponse> events;
                    String qd = result.getQueryDate();
                    events = (qd != null && !qd.isBlank()) ? eventService.getEventsByDate(userId, java.time.LocalDate.parse(qd)) : eventService.getAllEvents(userId);
                    result.setEvents(events);
                    result.setConfirmationMessage(result.getConfirmationMessage() + (events.isEmpty() ? " 暂无事件" : " 共" + events.size() + "个"));
                }
            }
        } catch (Exception e) { log.error("执行语音指令失败", e); result.setSuccess(false); result.setErrorMessage("操作失败: " + e.getMessage()); }
        return result;
    }
}
