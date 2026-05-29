package com.voicecalendar.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;
    private String location;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private Boolean allDay = false;
    private String color;
    private String category = "other";

    /** 重复规则（可选） */
    private RecurringRuleRequest recurringRule;
}
