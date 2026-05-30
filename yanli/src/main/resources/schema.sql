-- ============================================
-- 语音日历 MySQL 数据库初始化脚本
-- 使用方法: mysql -u root -p < schema.sql
-- 或 JPA ddl-auto: update 会自动建表，此脚本作为参考
-- ============================================

CREATE DATABASE IF NOT EXISTS voice_calendar
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE voice_calendar;

-- 重复规则表
CREATE TABLE IF NOT EXISTS recurring_rules (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(20)    NOT NULL COMMENT 'DAILY/WEEKLY/MONTHLY/YEARLY',
    interval_count  INT            DEFAULT 1 COMMENT '间隔数',
    days_of_week    VARCHAR(30)    NULL     COMMENT '星期几,逗号分隔: 1,3,5',
    days_of_month   VARCHAR(60)    NULL     COMMENT '月内第几天,逗号分隔',
    end_date        DATETIME       NULL     COMMENT '重复结束日期',
    max_occurrences INT            NULL     COMMENT '最大重复次数',
    created_at      DATETIME       DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='重复规则';

-- 日历事件表
CREATE TABLE IF NOT EXISTS calendar_events (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    title             VARCHAR(200)  NOT NULL COMMENT '事件标题',
    description       VARCHAR(2000) NULL     COMMENT '事件描述',
    location          VARCHAR(500)  NULL     COMMENT '事件地点',
    start_time        DATETIME      NOT NULL COMMENT '开始时间',
    end_time          DATETIME      NOT NULL COMMENT '结束时间',
    all_day           TINYINT(1)    DEFAULT 0 COMMENT '是否全天事件',
    color             VARCHAR(20)   NULL     COMMENT '前端显示颜色',
    category          VARCHAR(30)   DEFAULT 'other' COMMENT '分类: meeting/personal/work/reminder/other',
    recurring_rule_id BIGINT        NULL     COMMENT '重复规则ID',
    created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time),
    INDEX idx_category (category),
    INDEX idx_recurring (recurring_rule_id),

    CONSTRAINT fk_recurring_rule
        FOREIGN KEY (recurring_rule_id)
        REFERENCES recurring_rules(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日历事件';
