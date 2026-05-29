package com.voicecalendar.repository;

import com.voicecalendar.model.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query("SELECT e FROM CalendarEvent e WHERE e.userId = :userId AND e.startTime < :end AND e.endTime > :start ORDER BY e.startTime ASC")
    List<CalendarEvent> findByTimeRange(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<CalendarEvent> findByUserIdAndTitleContaining(Long userId, String keyword);

    @Query("SELECT e FROM CalendarEvent e WHERE e.userId = :userId AND e.startTime >= :dayStart AND e.startTime < :dayEnd ORDER BY e.startTime ASC")
    List<CalendarEvent> findByDate(@Param("userId") Long userId, @Param("dayStart") LocalDateTime dayStart, @Param("dayEnd") LocalDateTime dayEnd);

    List<CalendarEvent> findByUserIdOrderByStartTimeAsc(Long userId);
}
