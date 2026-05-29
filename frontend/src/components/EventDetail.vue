<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="detail-modal">
      <div class="detail-header" :style="{ borderTop: '4px solid ' + colorBar }">
        <h2>{{ event.title }}</h2>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <div class="detail-body">
        <div class="detail-row">
          <span class="detail-icon">🕐</span>
          <span>{{ formatRange(event.startTime, event.endTime) }}</span>
          <span v-if="event.allDay" class="badge">全天</span>
        </div>

        <div v-if="event.location" class="detail-row">
          <span class="detail-icon">📍</span>
          <span>{{ event.location }}</span>
        </div>

        <div v-if="event.category" class="detail-row">
          <span class="detail-icon">📂</span>
          <span class="category-tag">{{ categoryLabel }}</span>
        </div>

        <div v-if="event.recurringRule" class="detail-row">
          <span class="detail-icon">🔄</span>
          <span>{{ recurringLabel }}</span>
        </div>

        <div v-if="event.description" class="detail-desc">
          <div class="desc-label">备注</div>
          <p>{{ event.description }}</p>
        </div>
      </div>

      <div class="detail-footer">
        <button class="btn-edit" @click="$emit('edit', event)">✏️ 编辑</button>
        <button class="btn-delete" @click="$emit('delete', event)">🗑️ 删除</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({
  event: { type: Object, required: true }
})

defineEmits(['close', 'edit', 'delete'])

const colorBar = computed(() => {
  const colors = {
    meeting: '#3b82f6',
    personal: '#ec4899',
    work: '#10b981',
    reminder: '#f59e0b',
    other: '#6366f1'
  }
  return props.event.color || colors[props.event.category] || colors.other
})

const categoryLabel = computed(() => {
  const labels = {
    meeting: '会议', personal: '个人', work: '工作',
    reminder: '提醒', other: '其他'
  }
  return labels[props.event.category] || props.event.category
})

const recurringLabel = computed(() => {
  const r = props.event.recurringRule
  if (!r) return ''
  const types = { DAILY: '每天', WEEKLY: '每周', MONTHLY: '每月', YEARLY: '每年' }
  let label = types[r.type] || r.type
  if (r.intervalCount > 1) label = `每${r.intervalCount}${label.charAt(1)}`
  if (r.daysOfWeek) {
    const dayNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
    const days = r.daysOfWeek.split(',').map(d => dayNames[parseInt(d)] || d)
    label += ` (${days.join(', ')})`
  }
  return label
})

function formatRange(start, end) {
  const s = dayjs(start)
  const e = dayjs(end)
  if (s.format('YYYY-MM-DD') === e.format('YYYY-MM-DD')) {
    return s.format('YYYY年M月D日 HH:mm') + ' - ' + e.format('HH:mm')
  }
  return s.format('YYYY年M月D日 HH:mm') + ' - ' + e.format('M月D日 HH:mm')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.detail-modal {
  width: 440px;
  max-height: 80vh;
  background: var(--surface);
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 18px 20px 14px;
}

.detail-header h2 {
  font-size: 18px;
  padding-right: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.detail-body { padding: 0 20px 20px; }

.detail-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 14px;
}

.detail-icon { font-size: 16px; width: 24px; text-align: center; }

.badge {
  background: #dbeafe;
  color: #1e40af;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.category-tag {
  background: #f1f5f9;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.detail-desc {
  margin-top: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.desc-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.detail-desc p {
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.detail-footer {
  display: flex;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid var(--border);
}

.btn-edit, .btn-delete {
  flex: 1;
  padding: 10px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.btn-edit { background: #c0392b; color: white; }
.btn-delete { background: #fef2f2; color: var(--danger); }
</style>
