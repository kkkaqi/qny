package com.voicecalendar.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @NotBlank(message = "事件标题不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String location;

    @NotNull(message = "开始时间不能为空")
    @Column(nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Column(nullable = false)
    private LocalDateTime endTime;

    @Builder.Default
    private Boolean allDay = false;

    @Column(length = 20)
    private String color;

    @Column(length = 30)
    @Builder.Default
    private String category = "other";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_rule_id")
    private RecurringRule recurringRule;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
