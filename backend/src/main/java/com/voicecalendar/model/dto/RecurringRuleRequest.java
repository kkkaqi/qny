package com.voicecalendar.model.dto;

import lombok.Data;

@Data
public class RecurringRuleRequest {

    /** DAILY / WEEKLY / MONTHLY / YEARLY */
    private String type;
    private Integer intervalCount = 1;
    private String daysOfWeek;
    private String daysOfMonth;
    private String endDate;    // ISO datetime string
    private Integer maxOccurrences;
}
