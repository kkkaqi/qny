<template>
  <div class="today-panel">
    <div class="today-main">
      <div class="today-date">{{ dateNum }}</div>
      <div class="today-info">
        <div class="today-month">{{ month }}月</div>
        <div class="today-weekday">{{ weekdayLabel }}</div>
      </div>
    </div>
    <div class="today-year">{{ year }}年</div>
    <div class="today-lunar">
      <span class="lunar-label">农历</span>
      <span class="lunar-text">{{ lunarText }}</span>
    </div>
    <div class="today-events">
      <div class="events-label">今日事件</div>
      <div v-if="todayEvents.length === 0" class="no-events">暂无安排</div>
      <div
        v-for="event in todayEvents.slice(0, 5)"
        :key="event.id"
        :class="['today-event-item', event.category || 'other']"
      >
        <div class="te-time">{{ formatTime(event.startTime) }}</div>
        <div class="te-title">{{ event.title }}</div>
      </div>
      <div v-if="todayEvents.length > 5" class="more">
        还有 {{ todayEvents.length - 5 }} 个事件
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'
import solarLunar from 'solarlunar'

const props = defineProps({
  events: { type: Array, default: () => [] }
})

const now = dayjs()

// 公历信息
const year = computed(() => now.year())
const month = computed(() => now.month() + 1)
const dateNum = computed(() => now.date())

const weekdayLabel = computed(() => {
  const labels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return labels[now.day()]
})

// 农历信息 - 使用 solarlunar 库
const lunarText = computed(() => {
  try {
    const lunar = solarLunar.solar2lunar(now.year(), now.month() + 1, now.date())
    return `${lunar.animal}年 ${lunar.monthCn}${lunar.dayCn}`
  } catch (e) {
    return '农历加载中'
  }
})

// 今日事件
const todayEvents = computed(() => {
  const todayStr = now.format('YYYY-MM-DD')
  return props.events.filter(e => {
    const eDate = dayjs(e.startTime).format('YYYY-MM-DD')
    const eEndDate = dayjs(e.endTime).format('YYYY-MM-DD')
    return eDate <= todayStr && eEndDate >= todayStr
  }).sort((a, b) => dayjs(a.startTime).diff(dayjs(b.startTime)))
})

function formatTime(timeStr) {
  return dayjs(timeStr).format('HH:mm')
}
</script>

<style scoped>
.today-panel {
  width: 200px;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.today-main {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 4px;
}

.today-date {
  font-size: 52px;
  font-weight: 700;
  color: #333;
  line-height: 1;
}

.today-info {
  padding-top: 4px;
}

.today-month {
  font-size: 16px;
  color: #666;
  line-height: 1.3;
}

.today-weekday {
  font-size: 13px;
  color: #c0392b;
}

.today-year {
  font-size: 14px;
  color: #333;
  margin-bottom: 2px;
}

.today-lunar {
  margin: 8px 0 16px;
  padding: 6px 12px;
  background: #fef5f5;
  border-radius: 4px;
}

.lunar-label {
  font-size: 11px;
  color: #999;
  margin-right: 4px;
}

.lunar-text {
  font-size: 13px;
  color: #c0392b;
}

.today-events {
  width: 100%;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.events-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}

.no-events {
  font-size: 12px;
  color: #ccc;
  text-align: center;
  padding: 8px 0;
}

.today-event-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 4px 0;
  font-size: 12px;
  border-bottom: 1px solid #fafafa;
}

.te-time {
  color: #999;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.te-title {
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more {
  font-size: 11px;
  color: #c0392b;
  text-align: center;
  padding: 4px 0;
}
</style>
