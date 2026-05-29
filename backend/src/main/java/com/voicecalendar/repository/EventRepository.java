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

    /** 查询指定时间范围内的事件 */
    @Query("SELECT e FROM CalendarEvent e WHERE e.startTime < :end AND e.endTime > :start ORDER BY e.startTime ASC")
    List<CalendarEvent> findByTimeRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** 按标题模糊搜索 */
    List<CalendarEvent> findByTitleContaining(String keyword);

    /** 查询某天的事件 */
    @Query("SELECT e FROM CalendarEvent e WHERE " +
           "e.startTime >= :dayStart AND e.startTime < :dayEnd " +
           "ORDER BY e.startTime ASC")
    List<CalendarEvent> findByDate(
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    /** 查询指定分类的事件 */
    List<CalendarEvent> findByCategory(String category);
}
