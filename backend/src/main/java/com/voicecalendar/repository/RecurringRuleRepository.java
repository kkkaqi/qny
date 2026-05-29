package com.voicecalendar.repository;

import com.voicecalendar.model.RecurringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecurringRuleRepository extends JpaRepository<RecurringRule, Long> {
}
