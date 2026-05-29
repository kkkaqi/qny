package com.voicecalendar.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 重复规则：支持每天/每周/每月/每年 及自定义间隔
 */
@Entity
@Table(name = "recurring_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 重复类型: DAILY / WEEKLY / MONTHLY / YEARLY */
    @Column(nullable = false, length = 20)
    private String type;

    /** 间隔（每N天/周/月/年） */
    @Builder.Default
    private Integer intervalCount = 1;

    /** 一周中的哪几天（WEEKLY时使用），逗号分隔: "1,3,5" 表示周一三五 */
    @Column(length = 30)
    private String daysOfWeek;

    /** 一月中的哪几天（MONTHLY时使用），逗号分隔 */
    @Column(length = 60)
    private String daysOfMonth;

    /** 重复结束时间（可选） */
    private LocalDateTime endDate;

    /** 重复次数上限（可选） */
    private Integer maxOccurrences;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
