package com.voicecalendar.model.dto;

import com.voicecalendar.model.CalendarEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allDay;
    private String color;
    private String category;
    private RecurringRuleResponse recurringRule;

    public static EventResponse fromEntity(CalendarEvent event) {
        EventResponseBuilder builder = EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .allDay(event.getAllDay())
                .color(event.getColor())
                .category(event.getCategory());

        if (event.getRecurringRule() != null) {
            builder.recurringRule(RecurringRuleResponse.fromEntity(event.getRecurringRule()));
        }

        return builder.build();
    }
}
