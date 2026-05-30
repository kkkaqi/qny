<template>
  <div class="month-view">
    <div class="week-header">
      <div v-for="day in weekDays" :key="day" :class="['week-day-header', { weekend: day === '日' || day === '六' }]">
        {{ day }}
      </div>
    </div>
    <div class="month-grid">
      <div
        v-for="(cell, idx) in calendarCells"
        :key="idx"
        :class="['day-cell', {
          'is-today': cell.isToday,
          'other-month': !cell.isCurrentMonth,
          'weekend': cell.isWeekend
        }]"
        @click="$emit('goToDate', cell.date)"
      >
        <div class="day-number">
          <span :class="{ 'today-badge': cell.isToday }">{{ cell.dayNum }}</span>
        </div>
        <div v-if="holidays[cell.date]" class="day-holiday">{{ holidays[cell.date] }}</div>
        <div v-if="slogans[cell.date]" class="day-slogan">{{ slogans[cell.date] }}</div>
        <div class="day-events">
          <div
            v-for="event in cell.events.slice(0, 4)"
            :key="event.id"
            :class="['event-chip', event.category || 'other']"
            @click.stop="$emit('eventClick', event)"
            :title="event.title"
          >
            {{ event.title }}
          </div>
          <div v-if="cell.events.length > 4" class="more-events" @click.stop="$emit('goToDate', cell.date)">
            +{{ cell.events.length - 4 }} 更多
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({
  events: { type: Array, default: () => [] },
  currentDate: { type: Object, required: true },
  holidays: { type: Object, default: () => ({}) },
  slogans: { type: Object, default: () => ({}) }
})

defineEmits(['goToDate', 'eventClick'])

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const calendarCells = computed(() => {
  const d = props.currentDate
  const startOfMonth = d.startOf('month')
  const endOfMonth = d.endOf('month')
  const startDay = startOfMonth.startOf('week')
  const endDay = endOfMonth.endOf('week')
  const today = dayjs().format('YYYY-MM-DD')

  const cells = []
  let current = startDay
  while (current.isBefore(endDay) || current.isSame(endDay, 'day')) {
    const dateStr = current.format('YYYY-MM-DD')
    const isCurrentMonth = current.month() === d.month()
    const dayOfWeek = current.day()

    const dayEvents = props.events.filter(e => {
      const eDate = dayjs(e.startTime).format('YYYY-MM-DD')
      return eDate === dateStr
    })

    cells.push({
      date: dateStr,
      dayNum: current.date(),
      isToday: dateStr === today,
      isCurrentMonth,
      isWeekend: dayOfWeek === 0 || dayOfWeek === 6,
      events: dayEvents
    })

    current = current.add(1, 'day')
  }

  return cells
})
</script>

<style scoped>
.month-view {
  display: flex;
  flex-direction: column;
}

.week-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border-bottom: 2px solid var(--primary, #c0392b);
}

.week-day-header {
  padding: 8px 0;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #666;
}

.week-day-header.weekend { color: var(--primary, #c0392b); }

.month-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.day-cell {
  aspect-ratio: 1;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  padding: 4px 6px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.day-cell:nth-child(7n) { border-right: none; }
.day-cell:hover { background: #fafafa; }

.day-cell.weekend .day-number {
  color: var(--primary, #c0392b);
}

.day-cell.other-month .day-number {
  color: #ddd;
}

.day-number {
  text-align: center;
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}

.today-badge {
  background: var(--today-bg, #c0392b);
  color: white;
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  border-radius: 50%;
  text-align: center;
  font-size: 13px;
}

.day-holiday {
  font-size: 10px; color: #e74c3c; text-align: center; line-height: 1.2;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.day-slogan {
  font-size: 9px; color: #e8a09a; text-align: center; line-height: 1.2;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-style: italic;
}

.day-events {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-height: 0;
}

.event-chip {
  font-size: 11px;
  padding: 1px 4px;
  border-radius: 3px;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}

.event-chip.meeting { background: #d4e6ff; color: #2563eb; }
.event-chip.personal { background: #fce7f3; color: #be185d; }
.event-chip.work { background: #d1fae5; color: #047857; }
.event-chip.reminder { background: #fef3c7; color: #b45309; }
.event-chip.other { background: #f0f0f0; color: #666; }

.more-events {
  font-size: 11px;
  color: var(--primary, #c0392b);
  padding: 0 4px;
  cursor: pointer;
}
</style>
