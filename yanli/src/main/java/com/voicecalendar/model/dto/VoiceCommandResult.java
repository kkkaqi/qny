package com.voicecalendar.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 语音指令解析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCommandResult {

    /** 原始语音转文字 */
    private String originalText;

    /** 指令意图: ADD_EVENT / DELETE_EVENT / UPDATE_EVENT / QUERY_EVENTS / UNKNOWN */
    private String intent;

    /** 解析后的事件数据（ADD / UPDATE 时有值） */
    private EventRequest eventData;

    /** 查询条件（QUERY 时有值） */
    private String queryDate;      // "today" / "tomorrow" / "2024-01-15"
    private String queryPeriod;    // "day" / "week" / "month"

    /** 删除时的事件 ID 或关键词 */
    private Long deleteEventId;
    private String deleteKeyword;

    /** 用户确认消息 */
    private String confirmationMessage;

    /** 是否需要用户二次确认 */
    @Builder.Default
    private Boolean needsConfirmation = false;

    /** 错误信息 */
    private String errorMessage;

    /** 操作是否成功 */
    @Builder.Default
    private Boolean success = true;

    /** 返回的事件列表（QUERY 时有值） */
    private List<EventResponse> events;
}
