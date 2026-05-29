<template>
  <div class="day-view">
    <div v-if="dayEvents.length === 0" class="empty-day">
      <p>暂无事件</p>
      <p class="hint">点击右下角麦克风按钮，用语音添加</p>
    </div>
    <div
      v-for="event in dayEvents"
      :key="event.id"
      :class="['day-event', event.category || 'other']"
      @click="$emit('eventClick', event)"
    >
      <div class="event-bar" :style="{ background: eventColor(event) }"></div>
      <div class="event-content">
        <div class="event-title-row">
          <span class="de-title">{{ event.title }}</span>
          <span v-if="event.recurringRule" class="recur-icon" title="重复">🔄</span>
        </div>
        <div class="event-meta">
          <span>{{ formatTimeRange(event.startTime, event.endTime) }}</span>
          <span v-if="event.location">📍 {{ event.location }}</span>
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

const dayEvents = computed(() => {
  const dateStr = props.currentDate.format('YYYY-MM-DD')
  return props.events
    .filter(e => {
      const eDate = dayjs(e.startTime).format('YYYY-MM-DD')
      const eEndDate = dayjs(e.endTime).format('YYYY-MM-DD')
      return eDate <= dateStr && eEndDate >= dateStr
    })
    .sort((a, b) => dayjs(a.startTime).diff(dayjs(b.startTime)))
})

function formatTimeRange(start, end) {
  const s = dayjs(start), e = dayjs(end)
  if (s.format('YYYY-MM-DD') === e.format('YYYY-MM-DD')) {
    return s.format('HH:mm') + ' - ' + e.format('HH:mm')
  }
  return s.format('MM-DD HH:mm') + ' - ' + e.format('MM-DD HH:mm')
}

function eventColor(event) {
  const colors = { meeting: '#3b82f6', personal: '#ec4899', work: '#10b981', reminder: '#f59e0b' }
  return event.color || colors[event.category] || '#c0392b'
}
</script>

<style scoped>
.day-view {
  padding: 12px 20px;
}

.empty-day {
  text-align: center;
  padding: 50px 20px;
  color: #999;
  font-size: 14px;
}
.empty-day .hint { font-size: 12px; margin-top: 4px; }

.day-event {
  display: flex;
  margin-bottom: 6px;
  border-radius: 4px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  overflow: hidden;
}

.day-event:hover { background: #f5f5f5; }

.event-bar { width: 3px; flex-shrink: 0; }
.event-content { flex: 1; padding: 8px 14px; }
.event-title-row { display: flex; align-items: center; gap: 4px; }
.de-title { font-size: 14px; font-weight: 500; color: #333; }
.recur-icon { font-size: 12px; }
.event-meta { margin-top: 2px; font-size: 12px; color: #999; display: flex; gap: 12px; }
</style>
