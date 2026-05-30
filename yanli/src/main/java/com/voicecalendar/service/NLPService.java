package com.voicecalendar.service;

import com.voicecalendar.model.dto.EventRequest;
import com.voicecalendar.model.dto.RecurringRuleRequest;
import com.voicecalendar.model.dto.VoiceCommandResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中文语音指令 NLP 解析服务
 *
 * 支持的指令格式：
 * - 添加事件: "添加[明天][下午三点][开会][在会议室]" / "新建日程..."
 * - 删除事件: "删除[开会]" / "取消[明天下午的会议]"
 * - 修改事件: "修改[开会]时间[改为下午四点]" / "把[会议]改成..."
 * - 查询事件: "查看[今天]的日程" / "明天有什么安排" / "这周有什么"
 */
@Service
@Slf4j
public class NLPService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 解析语音文本，提取意图和结构化数据
     */
    public VoiceCommandResult parse(String text) {
        if (text == null || text.isBlank()) {
            return VoiceCommandResult.builder()
                    .originalText(text)
                    .intent("UNKNOWN")
                    .success(false)
                    .errorMessage("未识别到有效语音内容")
                    .build();
        }

        text = text.trim();
        log.info("NLP 解析: {}", text);

        // 1. 识别意图
        String intent = detectIntent(text);

        return switch (intent) {
            case "ADD_EVENT" -> parseAddEvent(text);
            case "DELETE_EVENT" -> parseDeleteEvent(text);
            case "UPDATE_EVENT" -> parseUpdateEvent(text);
            case "QUERY_EVENTS" -> parseQueryEvents(text);
            default -> VoiceCommandResult.builder()
                    .originalText(text)
                    .intent("UNKNOWN")
                    .success(false)
                    .errorMessage("未能理解您的指令，请尝试说「添加明天下午三点的会议」或「查看今天的日程」")
                    .build();
        };
    }

    /**
     * 识别指令意图
     */
    private String detectIntent(String text) {
        // 添加类的关键词
        String[] addPatterns = {"添加", "新建", "创建", "增加", "安排", "设置", "加入", "定一个", "帮我记"};
        for (String p : addPatterns) {
            if (text.startsWith(p) || text.contains(p)) {
                // 排除修改变更类
                if (!text.contains("修改") && !text.contains("改") && !text.contains("变为")) {
                    return "ADD_EVENT";
                }
            }
        }

        // 删除类的关键词
        String[] delPatterns = {"删除", "取消", "移除", "去掉", "删掉", "不要"};
        for (String p : delPatterns) {
            if (text.startsWith(p) || text.contains(p)) {
                return "DELETE_EVENT";
            }
        }

        // 修改类的关键词
        if (text.contains("修改") || text.contains("改成") || text.contains("改为")
                || text.contains("变更") || text.contains("调整为") || text.contains("改到")) {
            return "UPDATE_EVENT";
        }

        // 查询类的关键词
        String[] queryPatterns = {"查看", "查询", "看一下", "看看", "有什么", "有哪些",
                "日程", "安排", "今天的", "明天的", "这周的", "下周的", "这个月的"};
        for (String p : queryPatterns) {
            if (text.startsWith(p) || text.contains(p)) {
                return "QUERY_EVENTS";
            }
        }

        return "UNKNOWN";
    }

    /**
     * 解析添加事件: "添加明天下午三点到四点半在301开项目周会"
     */
    private VoiceCommandResult parseAddEvent(String text) {
        try {
            // 提取时间
            LocalDateTime[] times = extractDateTime(text);
            LocalDateTime startTime = times[0];
            LocalDateTime endTime = times[1];

            // 提取地点
            String location = extractLocation(text);

            // 提取标题
            String title = extractEventTitle(text);
            if (title == null || title.isBlank()) {
                title = "新建事件";
            }

            // 提取分类
            String category = extractCategory(text);

            // 提取重复规则
            RecurringRuleRequest recurring = extractRecurringRule(text);

            // 默认时间处理
            if (startTime == null) {
                startTime = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
            }
            if (endTime == null) {
                endTime = startTime.plusHours(1);
            }

            EventRequest eventReq = new EventRequest();
            eventReq.setTitle(title);
            eventReq.setLocation(location);
            eventReq.setStartTime(startTime);
            eventReq.setEndTime(endTime);
            eventReq.setCategory(category);
            eventReq.setAllDay(extractAllDay(text));
            if (recurring != null) {
                eventReq.setRecurringRule(recurring);
            }

            String timeDesc = formatTimeDesc(startTime, endTime);
            return VoiceCommandResult.builder()
                    .originalText(text)
                    .intent("ADD_EVENT")
                    .eventData(eventReq)
                    .confirmationMessage(String.format("已添加事件：「%s」%s%s",
                            title, timeDesc,
                            location != null ? "，地点：" + location : ""))
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("解析添加事件失败: {}", text, e);
            return VoiceCommandResult.builder()
                    .originalText(text)
                    .intent("ADD_EVENT")
                    .success(false)
                    .errorMessage("未能理解事件详情，请说得更具体一些，例如「添加明天下午三点开会」")
                    .build();
        }
    }

    /**
     * 解析删除事件
     */
    private VoiceCommandResult parseDeleteEvent(String text) {
        // 提取要删除的关键词
        String keyword = null;
        Pattern p = Pattern.compile("(?:删除|取消|移除|去掉|删掉|不要)(?:了?)(.+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            keyword = m.group(1).trim();
            // 清理多余修饰词
            keyword = keyword.replaceAll("(?:那个|这个|一个)的?", "").trim();
        }

        return VoiceCommandResult.builder()
                .originalText(text)
                .intent("DELETE_EVENT")
                .deleteKeyword(keyword)
                .confirmationMessage(keyword != null
                        ? "正在搜索与「" + keyword + "」相关的事件，请选择要删除的项目"
                        : "请告诉我要删除哪个事件")
                .needsConfirmation(true)
                .success(true)
                .build();
    }

    /**
     * 解析修改事件
     */
    private VoiceCommandResult parseUpdateEvent(String text) {
        return VoiceCommandResult.builder()
                .originalText(text)
                .intent("UPDATE_EVENT")
                .success(false)
                .errorMessage("修改功能请在界面上操作，语音修改功能持续优化中")
                .build();
    }

    /**
     * 解析查询事件
     */
    private VoiceCommandResult parseQueryEvents(String text) {
        LocalDate queryDate = LocalDate.now();
        String period = "day";
        String dateDesc = "今天";

        if (text.contains("明天") || text.contains("明日")) {
            queryDate = LocalDate.now().plusDays(1);
            dateDesc = "明天";
        } else if (text.contains("后天")) {
            queryDate = LocalDate.now().plusDays(2);
            dateDesc = "后天";
        } else if (text.contains("大后天")) {
            queryDate = LocalDate.now().plusDays(3);
            dateDesc = "大后天";
        }

        // 提取具体日期（格式: X月X日 / X月X号）
        Pattern datePattern = Pattern.compile("(\\d{1,2})月(\\d{1,2})[日号]");
        Matcher dm = datePattern.matcher(text);
        if (dm.find()) {
            int month = Integer.parseInt(dm.group(1));
            int day = Integer.parseInt(dm.group(2));
            queryDate = LocalDate.of(LocalDate.now().getYear(), month, day);
            dateDesc = month + "月" + day + "日";
        }

        if (text.contains("周") || text.contains("星期")) {
            period = "week";
        } else if (text.contains("月")) {
            period = "month";
        }

        return VoiceCommandResult.builder()
                .originalText(text)
                .intent("QUERY_EVENTS")
                .queryDate(queryDate.format(DATE_FMT))
                .queryPeriod(period)
                .confirmationMessage("正在查询" + dateDesc + "的日程安排...")
                .success(true)
                .build();
    }

    // ========== 辅助解析方法 ==========

    /**
     * 从文本中提取时间范围，返回 [startTime, endTime]
     */
    private LocalDateTime[] extractDateTime(String text) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        LocalDate baseDate = LocalDate.now();

        // 日期偏移
        if (text.contains("明天") || text.contains("明日")) {
            baseDate = LocalDate.now().plusDays(1);
        } else if (text.contains("后天")) {
            baseDate = LocalDate.now().plusDays(2);
        } else if (text.contains("大后天")) {
            baseDate = LocalDate.now().plusDays(3);
        }

        // 提取具体日期 "5月20日" / "5月20号" / "5-20" / "5/20"
        Pattern datePattern = Pattern.compile("(\\d{1,2})[月\\-\\/](\\d{1,2})[日号]?");
        Matcher dm = datePattern.matcher(text);
        if (dm.find()) {
            try {
                int month = Integer.parseInt(dm.group(1));
                int day = Integer.parseInt(dm.group(2));
                baseDate = LocalDate.of(LocalDate.now().getYear(), month, day);
            } catch (Exception ignored) {}
        }

        // 提取时间 "下午3点" / "15:00" / "三点半" / "三点"
        LocalTime startTime = extractTime(text);
        LocalTime endTime = null;

        // 尝试提取结束时间 "到四点" / "到下午四点" / "至五点"
        Pattern endPattern = Pattern.compile("[到至](?:了?)([^，,。.\\s]{2,12})");
        Matcher em = endPattern.matcher(text);
        if (em.find()) {
            String endStr = em.group(1);
            endTime = extractTimeFromString(endStr);
        }

        if (startTime != null) {
            start = LocalDateTime.of(baseDate, startTime);
        }
        if (endTime != null) {
            end = LocalDateTime.of(baseDate, endTime);
        } else if (start != null) {
            end = start.plusHours(1); // 默认持续时间1小时
        }

        return new LocalDateTime[]{start, end};
    }

    /**
     * 从文本中提取时间
     */
    private LocalTime extractTime(String text) {
        // "下午3点" / "上午10点30" / "15:00" / "三点半" / "三点"
        // 24小时制
        Pattern h24 = Pattern.compile("(\\d{1,2}):(\\d{2})");
        Matcher m24 = h24.matcher(text);
        if (m24.find()) {
            int h = Integer.parseInt(m24.group(1));
            int min = Integer.parseInt(m24.group(2));
            return LocalTime.of(h, min);
        }

        // 12小时制带上午/下午
        boolean isPM = text.contains("下午") || text.contains("晚上") || text.contains("傍晚");
        boolean isAM = text.contains("上午") || text.contains("早上") || text.contains("凌晨");

        Pattern h12 = Pattern.compile("(\\d{1,2})点(?:([半\\d]+))?");
        Matcher m12 = h12.matcher(text);
        if (m12.find()) {
            int h = Integer.parseInt(m12.group(1));
            int min = 0;
            if (m12.group(2) != null) {
                String minStr = m12.group(2);
                if ("半".equals(minStr)) {
                    min = 30;
                } else {
                    try { min = Integer.parseInt(minStr); } catch (Exception ignored) {}
                }
            }
            if (isPM && h != 12) h += 12;
            if (isAM && h == 12) h = 0;
            return LocalTime.of(h, min);
        }

        // 中文数字: 一点/两点/三点...十二点
        String[] cnNums = {"零", "一", "二", "两", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二"};
        for (int i = 0; i < cnNums.length; i++) {
            Pattern cnP = Pattern.compile(cnNums[i] + "点(?:([半\\d]+))?");
            Matcher cnM = cnP.matcher(text);
            if (cnM.find()) {
                int h = i;
                int min = 0;
                if (cnM.group(1) != null && "半".equals(cnM.group(1))) {
                    min = 30;
                }
                if (isPM && h != 12) h += 12;
                if (isAM && h == 12) h = 0;
                return LocalTime.of(h, min);
            }
        }

        return null;
    }

    private LocalTime extractTimeFromString(String str) {
        return extractTime(str);
    }

    /**
     * 提取地点: "在XXX" / "于XXX" / "地点XXX"
     */
    private String extractLocation(String text) {
        Pattern p = Pattern.compile("(?:在|于|地点|位置)[：:]?(.{1,20}?)(?:开会|见面|讨论|办|举行|，|。|$)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String loc = m.group(1).trim();
            // 清理混入的时间信息
            loc = loc.replaceAll("(?:上午|下午|晚上|早上)", "").trim();
            return loc.isEmpty() ? null : loc;
        }
        return null;
    }

    /**
     * 提取事件标题: 从文本中提取核心描述
     */
    private String extractEventTitle(String text) {
        // 移除指令前缀
        String title = text.replaceFirst("^(?:添加|新建|创建|增加|安排|设置|加入|定一个|帮我记)[了]?", "");
        // 移除时间描述
        title = title.replaceAll("(?:明天|后天|大后天|今天|上午|下午|晚上|早上|凌晨)", "");
        title = title.replaceAll("\\d{1,2}[月\\-\\/]\\d{1,2}[日号]?", "");
        title = title.replaceAll("[在到至于](?:了?)[^，,。.\\s]{1,20}?", "");
        title = title.replaceAll("\\d{1,2}点(?:半|[\\d]+)?", "");
        title = title.replaceAll("[在到至]\\d{1,2}:\\d{2}", "");
        // 移除会议/活动后缀词
        title = title.replaceAll("(?:开会|见面|讨论|办|举行)", "");
        title = title.trim();

        return title.isEmpty() ? "新建事件" : title;
    }

    /**
     * 提取分类
     */
    private String extractCategory(String text) {
        if (text.contains("会议") || text.contains("开会") || text.contains("讨论")) return "meeting";
        if (text.contains("生日") || text.contains("纪念日") || text.contains("聚会")) return "personal";
        if (text.contains("任务") || text.contains("截止") || text.contains("报告")) return "work";
        if (text.contains("提醒") || text.contains("别忘了") || text.contains("记得")) return "reminder";
        return "other";
    }

    /**
     * 提取重复规则
     */
    private RecurringRuleRequest extractRecurringRule(String text) {
        RecurringRuleRequest rule = null;
        if (text.contains("每天")) {
            rule = new RecurringRuleRequest();
            rule.setType("DAILY");
            rule.setIntervalCount(1);
        } else if (text.contains("每周") || text.contains("每个星期")) {
            rule = new RecurringRuleRequest();
            rule.setType("WEEKLY");
            rule.setIntervalCount(1);
            // 提取星期几
            String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日",
                    "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
            for (int i = 0; i < weekDays.length; i++) {
                if (text.contains(weekDays[i])) {
                    rule.setDaysOfWeek(String.valueOf((i % 7) + 1));
                    break;
                }
            }
        } else if (text.contains("每月") || text.contains("每个月")) {
            rule = new RecurringRuleRequest();
            rule.setType("MONTHLY");
            rule.setIntervalCount(1);
        }
        return rule;
    }

    /**
     * 是否全天事件
     */
    private Boolean extractAllDay(String text) {
        return text.contains("全天") || text.contains("一整天");
    }

    /**
     * 格式化时间描述
     */
    private String formatTimeDesc(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("M月d日 HH:mm");
        String startStr = start.format(timeFmt);
        String endStr = end.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (start.toLocalDate().equals(end.toLocalDate())) {
            return "，" + startStr + " 至 " + endStr;
        } else {
            return "，" + startStr + " 至 " + end.format(timeFmt);
        }
    }
}
