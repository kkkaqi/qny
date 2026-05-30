package com.voicecalendar.model.dto;

import com.voicecalendar.model.RecurringRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringRuleResponse {

    private Long id;
    private String type;
    private Integer intervalCount;
    private String daysOfWeek;
    private String daysOfMonth;

    public static RecurringRuleResponse fromEntity(RecurringRule rule) {
        return RecurringRuleResponse.builder()
                .id(rule.getId())
                .type(rule.getType())
                .intervalCount(rule.getIntervalCount())
                .daysOfWeek(rule.getDaysOfWeek())
                .daysOfMonth(rule.getDaysOfMonth())
                .build();
    }
}
