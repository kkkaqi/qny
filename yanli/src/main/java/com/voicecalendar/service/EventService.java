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

    @Transactional
    public EventResponse createEvent(Long userId, EventRequest request) {
        CalendarEvent event = CalendarEvent.builder()
                .userId(userId).title(request.getTitle()).description(request.getDescription())
                .location(request.getLocation()).startTime(request.getStartTime()).endTime(request.getEndTime())
                .allDay(request.getAllDay()).color(request.getColor()).category(request.getCategory()).build();
        if (request.getRecurringRule() != null) {
            RecurringRule rule = buildRecurringRule(request.getRecurringRule());
            recurringRuleRepository.save(rule);
            event.setRecurringRule(rule);
        }
        CalendarEvent saved = eventRepository.save(event);
        log.info("用户 {} 创建事件: {} (ID: {})", userId, saved.getTitle(), saved.getId());
        return EventResponse.fromEntity(saved);
    }

    @Transactional
    public EventResponse updateEvent(Long userId, Long id, EventRequest request) {
        CalendarEvent event = eventRepository.findById(id).orElseThrow(() -> new RuntimeException("事件不存在: " + id));
        if (!event.getUserId().equals(userId)) throw new RuntimeException("无权操作此事件");
        event.setTitle(request.getTitle()); event.setDescription(request.getDescription());
        event.setLocation(request.getLocation()); event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime()); event.setAllDay(request.getAllDay());
        event.setColor(request.getColor()); event.setCategory(request.getCategory());
        if (request.getRecurringRule() != null) {
            RecurringRule oldRule = event.getRecurringRule();
            RecurringRule newRule = buildRecurringRule(request.getRecurringRule());
            recurringRuleRepository.save(newRule);
            event.setRecurringRule(newRule);
            if (oldRule != null) recurringRuleRepository.delete(oldRule);
        }
        CalendarEvent saved = eventRepository.save(event);
        return EventResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteEvent(Long userId, Long id) {
        CalendarEvent event = eventRepository.findById(id).orElseThrow(() -> new RuntimeException("事件不存在: " + id));
        if (!event.getUserId().equals(userId)) throw new RuntimeException("无权操作此事件");
        eventRepository.deleteById(id);
    }

    public EventResponse getEvent(Long userId, Long id) {
        CalendarEvent event = eventRepository.findById(id).orElseThrow(() -> new RuntimeException("事件不存在: " + id));
        if (!event.getUserId().equals(userId)) throw new RuntimeException("无权查看此事件");
        return EventResponse.fromEntity(event);
    }

    public List<EventResponse> getEventsByDate(Long userId, LocalDate date) {
        return eventRepository.findByDate(userId, date.atStartOfDay(), date.atTime(LocalTime.MAX))
                .stream().map(EventResponse::fromEntity).collect(Collectors.toList());
    }

    public List<EventResponse> getEventsByRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByTimeRange(userId, start, end).stream().map(EventResponse::fromEntity).collect(Collectors.toList());
    }

    public List<EventResponse> searchEvents(Long userId, String keyword) {
        return eventRepository.findByUserIdAndTitleContaining(userId, keyword).stream().map(EventResponse::fromEntity).collect(Collectors.toList());
    }

    public List<EventResponse> getAllEvents(Long userId) {
        return eventRepository.findByUserIdOrderByStartTimeAsc(userId).stream().map(EventResponse::fromEntity).collect(Collectors.toList());
    }

    private RecurringRule buildRecurringRule(RecurringRuleRequest req) {
        return RecurringRule.builder().type(req.getType()).intervalCount(req.getIntervalCount() != null ? req.getIntervalCount() : 1)
                .daysOfWeek(req.getDaysOfWeek()).daysOfMonth(req.getDaysOfMonth()).build();
    }
}
