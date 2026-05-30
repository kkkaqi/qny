package com.voicecalendar.controller;

import com.voicecalendar.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    /** 获取某月节日 */
    @GetMapping("/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getMonth(
            @PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(holidayService.getMonthHolidays(year, month));
    }
}
