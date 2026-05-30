package com.voicecalendar.controller;

import com.voicecalendar.model.dto.EventRequest;
import com.voicecalendar.model.dto.EventResponse;
import com.voicecalendar.service.EventService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    private Long getUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request, HttpSession session) {
        return ResponseEntity.ok(eventService.createEvent(getUserId(session), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(@PathVariable Long id, @Valid @RequestBody EventRequest request, HttpSession session) {
        return ResponseEntity.ok(eventService.updateEvent(getUserId(session), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id, HttpSession session) {
        eventService.deleteEvent(getUserId(session), id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    /** 批量删除 */
    @PostMapping("/batch-delete")
    public ResponseEntity<Map<String, Object>> batchDelete(@RequestBody Map<String, Object> body, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("ids");
        if (rawIds == null || rawIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请选择要删除的事件"));
        }
        int count = 0;
        for (Integer id : rawIds) {
            try { eventService.deleteEvent(getUserId(session), Long.valueOf(id)); count++; } catch (Exception ignored) { }
        }
        return ResponseEntity.ok(Map.of("message", "已删除 " + count + " 个事件", "count", count));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable Long id, HttpSession session) {
        return ResponseEntity.ok(eventService.getEvent(getUserId(session), id));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<EventResponse>> getByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, HttpSession session) {
        return ResponseEntity.ok(eventService.getEventsByDate(getUserId(session), date));
    }

    @GetMapping("/range")
    public ResponseEntity<List<EventResponse>> getByRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end, HttpSession session) {
        return ResponseEntity.ok(eventService.getEventsByRange(getUserId(session), start, end));
    }

    @GetMapping("/search")
    public ResponseEntity<List<EventResponse>> search(@RequestParam String keyword, HttpSession session) {
        return ResponseEntity.ok(eventService.searchEvents(getUserId(session), keyword));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAll(HttpSession session) {
        return ResponseEntity.ok(eventService.getAllEvents(getUserId(session)));
    }
}
