<template>
  <div class="calendar-container">
    <div v-if="loading" class="loading-overlay">
      <div class="spinner"></div>
    </div>

    <MonthView
      v-if="viewMode === 'month'"
      :events="events"
      :currentDate="currentDate"
      :holidays="holidays"
      :slogans="slogans"
      @goToDate="$emit('goToDate', $event)"
      @eventClick="$emit('eventClick', $event)"
    />

    <WeekView
      v-else-if="viewMode === 'week'"
      :events="events"
      :currentDate="currentDate"
      @eventClick="$emit('eventClick', $event)"
    />

    <DayView
      v-else-if="viewMode === 'day'"
      :events="events"
      :currentDate="currentDate"
      @eventClick="$emit('eventClick', $event)"
    />
  </div>
</template>

<script setup>
import MonthView from './MonthView.vue'
import WeekView from './WeekView.vue'
import DayView from './DayView.vue'

defineProps({
  events: Array,
  currentDate: Object,
  viewMode: String,
  loading: Boolean,
  holidays: Object,
  slogans: Object
})

defineEmits(['goToDate', 'eventClick'])
</script>

<style scoped>
.calendar-container {
  position: relative;
  height: 100%;
}

.loading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255,255,255,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: #c0392b;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
