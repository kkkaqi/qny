<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="voice-modal">
      <div :class="['modal-header', result.success ? 'header-success' : 'header-error']">
        <span>{{ result.success ? '✅ 指令识别成功' : '❌ 处理失败' }}</span>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <div class="modal-body">
        <!-- 原始文字 -->
        <div v-if="result.originalText" class="section">
          <div class="label">识别文字</div>
          <div class="text-bubble">{{ result.originalText }}</div>
        </div>

        <!-- 错误信息 -->
        <div v-if="result.errorMessage" class="error-msg">
          {{ result.errorMessage }}
        </div>

        <!-- 确认信息 -->
        <div v-if="result.confirmationMessage" class="section">
          <div class="label">处理结果</div>
          <div class="confirm-msg">{{ result.confirmationMessage }}</div>
        </div>

        <!-- 查询结果 - 事件列表 -->
        <div v-if="result.events && result.events.length > 0" class="event-list">
          <div
            v-for="event in result.events"
            :key="event.id"
            :class="['mini-event', event.category || 'other']"
          >
            <div class="me-title">{{ event.title }}</div>
            <div class="me-time">{{ formatRange(event.startTime, event.endTime) }}</div>
            <div v-if="event.location" class="me-loc">📍 {{ event.location }}</div>
          </div>
        </div>

        <!-- 需要确认时显示按钮 -->
        <div v-if="result.needsConfirmation" class="confirm-actions">
          <button class="btn-confirm" @click="$emit('confirm', true); $emit('close')">
            确认操作
          </button>
          <button class="btn-cancel" @click="$emit('confirm', false); $emit('close')">
            取消
          </button>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-done" @click="$emit('close')">
          {{ result.needsConfirmation ? '稍后处理' : '完成' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'

defineProps({
  result: { type: Object, required: true }
})

defineEmits(['close', 'confirm'])

function formatRange(start, end) {
  return dayjs(start).format('MM-DD HH:mm') + ' - ' + dayjs(end).format('HH:mm')
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

.voice-modal {
  width: 420px;
  max-height: 80vh;
  background: var(--surface);
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  color: white;
  font-weight: 600;
}

.header-success { background: var(--success); }
.header-error { background: var(--danger); }

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
  opacity: 0.8;
}

.modal-body {
  padding: 18px;
  overflow-y: auto;
  flex: 1;
}

.section {
  margin-bottom: 14px;
}

.label {
  font-size: 12px;
  color: var(--text-secondary);
  text-transform: uppercase;
  margin-bottom: 6px;
  font-weight: 600;
}

.text-bubble {
  background: #f1f5f9;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 15px;
}

.error-msg {
  background: #fef2f2;
  color: var(--danger);
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 14px;
}

.confirm-msg {
  font-size: 15px;
  line-height: 1.5;
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 12px 0;
}

.mini-event {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.mini-event.meeting { background: #eff6ff; }
.mini-event.personal { background: #fdf2f8; }
.mini-event.work { background: #ecfdf5; }
.mini-event.reminder { background: #fffbeb; }
.mini-event.other { background: #f8fafc; }

.me-title { font-weight: 600; }
.me-time, .me-loc { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

.confirm-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.btn-confirm, .btn-cancel {
  flex: 1;
  padding: 10px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.btn-confirm { background: #c0392b; color: white; }
.btn-cancel { background: #f1f5f9; color: var(--text); }

.modal-footer {
  padding: 12px 18px;
  border-top: 1px solid var(--border);
  text-align: right;
}

.btn-done {
  padding: 8px 20px;
  background: #f1f5f9;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}
</style>
