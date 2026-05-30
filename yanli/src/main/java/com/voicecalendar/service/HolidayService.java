package com.voicecalendar.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class HolidayService {

    /** 节日标语库（按名称索引，不限年份） */
    private static final Map<String, String> SLOGANS = new LinkedHashMap<>();
    static {
        SLOGANS.put("元旦", "新年新气象，万象更新！");
        SLOGANS.put("情人节", "愿得一人心，白首不相离。");
        SLOGANS.put("妇女节", "致敬每一位了不起的她！");
        SLOGANS.put("植树节", "绿水青山就是金山银山。");
        SLOGANS.put("愚人节", "今天说的话，信不信由你！");
        SLOGANS.put("清明节", "清明时节雨纷纷，缅怀先人。");
        SLOGANS.put("劳动节", "劳动最光荣，致敬每一位劳动者！");
        SLOGANS.put("青年节", "青春须早为，岂能长少年。");
        SLOGANS.put("儿童节", "童心未泯，快乐永驻。");
        SLOGANS.put("端午节", "粽叶飘香，端午安康！");
        SLOGANS.put("建党节", "不忘初心，牢记使命。");
        SLOGANS.put("七夕", "金风玉露一相逢，便胜却人间无数。");
        SLOGANS.put("建军节", "铁血军魂，保家卫国。");
        SLOGANS.put("中元节", "慎终追远，缅怀故人。");
        SLOGANS.put("教师节", "桃李满天下，师恩似海深。");
        SLOGANS.put("中秋节", "但愿人长久，千里共婵娟。");
        SLOGANS.put("国庆节", "锦绣中华，盛世华诞！");
        SLOGANS.put("重阳节", "每逢佳节倍思亲，敬老爱老。");
        SLOGANS.put("万圣节", "不给糖就捣蛋！");
        SLOGANS.put("圣诞节", "Merry Christmas！平安喜乐。");
        SLOGANS.put("春节", "爆竹声中一岁除，春风送暖入屠苏。");
        SLOGANS.put("元宵节", "灯火万家，团圆美满。");
    }

    /** 公历固定节日（月日→名称，不限年份） */
    private static final Map<String, String> SOLAR_HOLIDAYS = Map.ofEntries(
            Map.entry("01-01", "元旦"), Map.entry("02-14", "情人节"),
            Map.entry("03-08", "妇女节"), Map.entry("03-12", "植树节"),
            Map.entry("04-01", "愚人节"), Map.entry("04-04", "清明节"),
            Map.entry("05-01", "劳动节"), Map.entry("05-04", "青年节"),
            Map.entry("06-01", "儿童节"), Map.entry("07-01", "建党节"),
            Map.entry("08-01", "建军节"), Map.entry("09-10", "教师节"),
            Map.entry("10-01", "国庆节"), Map.entry("10-31", "万圣节"),
            Map.entry("12-25", "圣诞节")
    );

    /** 农历节日每年的公历日期（硬编码 2026-2030） */
    private static final List<LunarHoliday> LUNAR_HOLIDAYS = new ArrayList<>();
    static {
        // 2026
        L(2026, 2, 17, "春节"); L(2026, 3, 3, "元宵节");
        L(2026, 6, 19, "端午节"); L(2026, 7, 29, "七夕");
        L(2026, 8, 27, "中元节"); L(2026, 9, 25, "中秋节"); L(2026, 10, 18, "重阳节");
        // 2027
        L(2027, 2, 6, "春节"); L(2027, 2, 20, "元宵节");
        L(2027, 6, 9, "端午节"); L(2027, 8, 15, "七夕");
        L(2027, 9, 15, "中秋节"); L(2027, 10, 8, "重阳节");
        // 2028
        L(2028, 1, 26, "春节"); L(2028, 2, 9, "元宵节");
        L(2028, 5, 28, "端午节"); L(2028, 8, 4, "七夕");
        L(2028, 10, 3, "中秋节"); L(2028, 10, 26, "重阳节");
        // 2029
        L(2029, 2, 13, "春节"); L(2029, 2, 27, "元宵节");
        L(2029, 6, 16, "端午节"); L(2029, 8, 21, "七夕");
        L(2029, 9, 22, "中秋节"); L(2029, 10, 16, "重阳节");
        // 2030
        L(2030, 2, 3, "春节"); L(2030, 2, 17, "元宵节");
        L(2030, 6, 5, "端午节"); L(2030, 8, 10, "七夕");
        L(2030, 9, 11, "中秋节"); L(2030, 10, 5, "重阳节");
    }

    public Map<String, Object> getMonthHolidays(int year, int month) {
        Map<String, String> holidayNames = new LinkedHashMap<>();
        Map<String, String> holidaySlogans = new LinkedHashMap<>();

        // 公历节日（每年都有）
        for (var entry : SOLAR_HOLIDAYS.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int m = Integer.parseInt(parts[0]);
            if (m == month) {
                int d = Integer.parseInt(parts[1]);
                // 清明节特判：2026年起4月4或5日，简化为4月4/5都显示
                String name = entry.getValue();
                String key = LocalDate.of(year, m, d).toString();
                holidayNames.put(key, name);
                holidaySlogans.put(key, SLOGANS.getOrDefault(name, ""));
            }
        }

        // 农历节日（硬编码）
        for (LunarHoliday lh : LUNAR_HOLIDAYS) {
            if (lh.year == year && lh.month == month) {
                String key = LocalDate.of(year, month, lh.day).toString();
                holidayNames.put(key, lh.name);
                holidaySlogans.put(key, SLOGANS.getOrDefault(lh.name, ""));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("holidays", holidayNames);
        result.put("slogans", holidaySlogans);
        return result;
    }

    private static void L(int y, int m, int d, String name) {
        LUNAR_HOLIDAYS.add(new LunarHoliday(y, m, d, name));
    }

    private record LunarHoliday(int year, int month, int day, String name) {}
}
