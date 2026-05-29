package com.voicecalendar.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicecalendar.model.dto.EventRequest;
import com.voicecalendar.model.dto.RecurringRuleRequest;
import com.voicecalendar.model.dto.VoiceCommandResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * LLM 驱动的语音指令解析服务
 *
 * 替代 NLPService 的规则解析，用大模型理解用户意图并提取结构化数据。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {

    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        你是一个日历助手。根据用户的语音输入，解析出日历操作意图和事件信息。
        当前日期是 %s（星期%s）。如果用户说"明天"就是 %s，"后天"就是 %s。

        请严格按以下JSON格式返回（不要包含markdown代码块标记）：
        {
          "intent": "ADD_EVENT",
          "title": "事件标题（提取核心内容，如'上课''会议'等）",
          "startTime": "2026-05-30T14:00:00",
          "endTime": "2026-05-30T15:00:00",
          "location": "地点（没有则为null）",
          "category": "meeting/personal/work/reminder/other",
          "allDay": false,
          "recurring": null,
          "confirmation": "自然语言确认消息"
        }

        intent 取值：
        - ADD_EVENT: 添加事件
        - DELETE_EVENT: 删除事件
        - QUERY_EVENTS: 查询事件
        - UNKNOWN: 无法识别

        规则：
        - 没有明确结束时间时，默认开始时间+1小时
        - "下午3点"=15:00，"晚上8点"=20:00
        - 只说了时间段没具体到几点的：早上→08:00，上午→09:00，中午→12:00，下午→14:00，晚上→18:00
        - **如果完全没有时间信息，startTime和endTime都设为null**
        - category: 开会/会议→meeting，上课/学习→work，生日/聚会→personal，提醒/别忘了→reminder
        - 提到"每天"→recurring:"DAILY"，"每周"→"WEEKLY"，"每月"→"MONTHLY"，否则为null
        - 删除意图时，title填要删除的关键词
        - 查询意图时，startTime填查询日期（00:00:00）
        - confirmation用自然语言回应用户，如"已添加明天下午2点的上课事件"或"明天暂无安排"
        """;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @SuppressWarnings("unchecked")
    public VoiceCommandResult parse(String text) {
        if (!llmProvider.isAvailable()) {
            return VoiceCommandResult.builder()
                    .originalText(text)
                    .intent("UNKNOWN")
                    .success(false)
                    .errorMessage("LLM 未配置，请在 application.yml 中设置 voice.llm.qwen.api-key")
                    .build();
        }

        try {
            LocalDate today = LocalDate.now();
            String dayOfWeek = today.getDayOfWeek().toString();
            // 翻译星期
            String[] cnWeeks = {"日", "一", "二", "三", "四", "五", "六"};
            String cnWeek = cnWeeks[today.getDayOfWeek().getValue() % 7];

            String tomorrow = today.plusDays(1).format(DateTimeFormatter.ofPattern("M月d日"));
            String dayAfter = today.plusDays(2).format(DateTimeFormatter.ofPattern("M月d日"));

            String prompt = String.format(SYSTEM_PROMPT,
                    today.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                    cnWeek, tomorrow, dayAfter);

            String response = llmProvider.chat(prompt, text);
            log.info("LLM 解析结果: {} → {}", text, response);

            if (response == null || response.isBlank()) {
                return failResult(text, "LLM 未返回有效结果");
            }

            // 提取 JSON（处理 LLM 可能包裹的 markdown 代码块）
            String json = extractJson(response);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            String intent = (String) map.getOrDefault("intent", "UNKNOWN");
            String title = (String) map.getOrDefault("title", "未命名事件");
            String confirmation = (String) map.getOrDefault("confirmation", "");

            VoiceCommandResult result = VoiceCommandResult.builder()
                    .originalText(text)
                    .intent(intent)
                    .confirmationMessage(confirmation)
                    .success(true)
                    .build();

            if ("ADD_EVENT".equals(intent) || "UPDATE_EVENT".equals(intent)) {
                EventRequest eventReq = buildEventRequest(map, title);
                result.setEventData(eventReq);
            } else if ("DELETE_EVENT".equals(intent)) {
                result.setDeleteKeyword(title);
                result.setNeedsConfirmation(true);
            } else if ("QUERY_EVENTS".equals(intent)) {
                String queryDate = (String) map.get("startTime");
                if (queryDate != null) {
                    result.setQueryDate(queryDate.substring(0, 10));
                }
            }

            return result;

        } catch (Exception e) {
            log.error("LLM 解析失败: {}", text, e);
            return failResult(text, "指令理解失败，请换种说法试试");
        }
    }

    private EventRequest buildEventRequest(Map<String, Object> map, String title) {
        EventRequest req = new EventRequest();
        req.setTitle(title);

        // 解析时间
        LocalDateTime startTime = parseTime((String) map.get("startTime"));
        LocalDateTime endTime = parseTime((String) map.get("endTime"));

        if (startTime != null) {
            req.setStartTime(startTime);
            req.setEndTime(endTime != null ? endTime : startTime.plusHours(1));
        } else {
            // 没有具体时间 → 设为今天全天事件
            req.setStartTime(LocalDate.now().atStartOfDay());
            req.setEndTime(LocalDate.now().atTime(LocalTime.MAX));
            req.setAllDay(true);
        }

        req.setLocation((String) map.get("location"));
        req.setCategory((String) map.getOrDefault("category", "other"));
        req.setAllDay(Boolean.TRUE.equals(map.get("allDay")));

        // 重复规则
        String recurring = (String) map.get("recurring");
        if (recurring != null && !recurring.isEmpty() && !"null".equals(recurring)) {
            RecurringRuleRequest rr = new RecurringRuleRequest();
            rr.setType(recurring);
            rr.setIntervalCount(1);
            req.setRecurringRule(rr);
        }

        return req;
    }

    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return null;
        try {
            return LocalDateTime.parse(timeStr, DT_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJson(String text) {
        // 去掉可能的 markdown 代码块标记
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private VoiceCommandResult failResult(String text, String msg) {
        return VoiceCommandResult.builder()
                .originalText(text).intent("UNKNOWN").success(false)
                .errorMessage(msg).build();
    }
}
