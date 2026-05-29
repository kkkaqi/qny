package com.voicecalendar.service;

import com.voicecalendar.model.CalendarEvent;
import com.voicecalendar.model.RecurringRule;
import com.voicecalendar.model.dto.EventRequest;
import com.voicecalendar.model.dto.EventResponse;
import com.voicecalendar.model.dto.RecurringRuleRequest;
import com.voicecalendar.repository.EventRepository;
import com.voicecalendar.repository.RecurringRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final RecurringRuleRepository recurringRuleRepository;

    /** 创建事件 */
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        CalendarEvent event = CalendarEvent.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .allDay(request.getAllDay())
                .color(request.getColor())
                .category(request.getCategory())
                .build();

        // 处理重复规则
        if (request.getRecurringRule() != null) {
            RecurringRule rule = buildRecurringRule(request.getRecurringRule());
            recurringRuleRepository.save(rule);
            event.setRecurringRule(rule);
        }

        CalendarEvent saved = eventRepository.save(event);
        log.info("创建事件: {} (ID: {})", saved.getTitle(), saved.getId());
        return EventResponse.fromEntity(saved);
    }

    /** 更新事件 */
    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("事件不存在: " + id));

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setAllDay(request.getAllDay());
        event.setColor(request.getColor());
        event.setCategory(request.getCategory());

        // 处理重复规则更新
        if (request.getRecurringRule() != null) {
            RecurringRule oldRule = event.getRecurringRule();
            RecurringRule newRule = buildRecurringRule(request.getRecurringRule());
            recurringRuleRepository.save(newRule);
            event.setRecurringRule(newRule);
            if (oldRule != null) {
                recurringRuleRepository.delete(oldRule);
            }
        }

        CalendarEvent saved = eventRepository.save(event);
        log.info("更新事件: {} (ID: {})", saved.getTitle(), saved.getId());
        return EventResponse.fromEntity(saved);
    }

    /** 删除事件 */
    @Transactional
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
        log.info("删除事件 ID: {}", id);
    }

    /** 获取事件详情 */
    public EventResponse getEvent(Long id) {
        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("事件不存在: " + id));
        return EventResponse.fromEntity(event);
    }

    /** 按日期查询事件 */
    public List<EventResponse> getEventsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return eventRepository.findByDate(start, end).stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /** 按时间范围查询事件 */
    public List<EventResponse> getEventsByRange(LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByTimeRange(start, end).stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /** 按关键词搜索事件 */
    public List<EventResponse> searchEvents(String keyword) {
        return eventRepository.findByTitleContaining(keyword).stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /** 获取所有事件 */
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private RecurringRule buildRecurringRule(RecurringRuleRequest req) {
        return RecurringRule.builder()
                .type(req.getType())
                .intervalCount(req.getIntervalCount() != null ? req.getIntervalCount() : 1)
                .daysOfWeek(req.getDaysOfWeek())
                .daysOfMonth(req.getDaysOfMonth())
                .build();
    }
}
