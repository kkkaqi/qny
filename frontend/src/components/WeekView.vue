<template>
  <div class="week-view">
    <div class="week-header-row">
      <div class="time-gutter"></div>
      <div
        v-for="day in weekDays"
        :key="day.date"
        :class="['day-header', { 'is-today': day.isToday }]"
      >
        <div class="day-name">{{ day.name }}</div>
        <div :class="['day-num', { 'today-badge': day.isToday }]">{{ day.dateNum }}</div>
      </div>
    </div>
    <div class="time-grid">
      <div v-for="hour in hours" :key="hour" class="hour-row">
        <div class="time-label">{{ pad(hour) }}:00</div>
        <div v-for="day in weekDays" :key="day.date" class="hour-cell">
          <div
            v-for="event in getHourEvents(day.date, hour)"
            :key="event.id"
            :class="['week-event', event.category || 'other']"
            :style="{ top: event._top + '%', height: event._height + '%' }"
            @click="$emit('eventClick', event)"
            :title="event.title"
          >
            {{ event.title }}
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
  currentDate: { type: Object, required: true }
})

defineEmits(['eventClick'])

const hours = Array.from({ length: 24 }, (_, i) => i)

const weekDays = computed(() => {
  const start = props.currentDate.startOf('week')
  const today = dayjs().format('YYYY-MM-DD')
  const names = ['日', '一', '二', '三', '四', '五', '六']
  return Array.from({ length: 7 }, (_, i) => {
    const d = start.add(i, 'day')
    const dateStr = d.format('YYYY-MM-DD')
    return { date: dateStr, name: names[i], dateNum: d.date(), isToday: dateStr === today }
  })
})

function getHourEvents(dateStr, hour) {
  return props.events
    .filter(e => {
      const eDate = dayjs(e.startTime).format('YYYY-MM-DD')
      return eDate === dateStr && dayjs(e.startTime).hour() === hour
    })
    .map(e => {
      const startMin = dayjs(e.startTime).minute()
      const durationMin = dayjs(e.endTime).diff(dayjs(e.startTime), 'minute')
      return { ...e, _top: (startMin / 60) * 100, _height: Math.max((durationMin / 60) * 100, 6) }
    })
}

function pad(n) { return String(n).padStart(2, '0') }
</script>

<style scoped>
.week-view {
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.week-header-row {
  display: grid;
  grid-template-columns: 50px repeat(7, 1fr);
  position: sticky;
  top: 0;
  background: #fafafa;
  z-index: 5;
  border-bottom: 2px solid var(--primary, #c0392b);
}

.time-gutter { border-right: 1px solid var(--border); }

.day-header {
  padding: 6px 0;
  text-align: center;
  border-right: 1px solid #f0f0f0;
}

.day-name { font-size: 12px; color: #999; }
.day-num { font-size: 14px; color: #333; }

.today-badge {
  background: var(--today-bg, #c0392b);
  color: white;
  width: 24px;
  height: 24px;
  line-height: 24px;
  border-radius: 50%;
  display: inline-block;
  font-size: 12px;
}

.time-grid { flex: 1; }

.hour-row {
  display: grid;
  grid-template-columns: 50px repeat(7, 1fr);
  min-height: 36px;
  border-bottom: 1px solid #f5f5f5;
}

.time-label {
  padding: 2px 6px;
  font-size: 11px;
  color: #bbb;
  text-align: right;
  border-right: 1px solid #f0f0f0;
}

.hour-cell {
  border-right: 1px solid #f5f5f5;
  position: relative;
}

.week-event {
  position: absolute;
  left: 2px;
  right: 2px;
  padding: 2px 4px;
  border-radius: 3px;
  font-size: 10px;
  cursor: pointer;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.week-event.meeting { background: #d4e6ff; border-left: 2px solid #3b82f6; }
.week-event.personal { background: #fce7f3; border-left: 2px solid #ec4899; }
.week-event.work { background: #d1fae5; border-left: 2px solid #10b981; }
.week-event.reminder { background: #fef3c7; border-left: 2px solid #f59e0b; }
.week-event.other { background: #f0f0f0; border-left: 2px solid #999; }
</style>
