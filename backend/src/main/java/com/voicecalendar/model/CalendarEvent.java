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

    /** 事件标题 */
    @NotBlank(message = "事件标题不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    /** 事件描述 */
    @Column(length = 2000)
    private String description;

    /** 事件地点 */
    @Column(length = 500)
    private String location;

    /** 开始时间 */
    @NotNull(message = "开始时间不能为空")
    @Column(nullable = false)
    private LocalDateTime startTime;

    /** 结束时间 */
    @NotNull(message = "结束时间不能为空")
    @Column(nullable = false)
    private LocalDateTime endTime;

    /** 是否全天事件 */
    @Builder.Default
    private Boolean allDay = false;

    /** 事件颜色（前端显示用） */
    @Column(length = 20)
    private String color;

    /** 事件分类：meeting / personal / work / reminder / other */
    @Column(length = 30)
    @Builder.Default
    private String category = "other";

    /** 重复规则 ID，为空表示不重复 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_rule_id")
    private RecurringRule recurringRule;

    /** 创建时间 */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
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
