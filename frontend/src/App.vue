<template>
  <div class="app-wrapper">
    <div class="app">
      <header class="app-header">
        <div class="header-left">
          <button class="btn-nav" @click="prev">◀</button>
          <span class="current-date">{{ dateTitle }}</span>
          <button class="btn-nav" @click="next">▶</button>
          <button class="btn-today" @click="goToday">今天</button>
        </div>
        <div class="header-center">
          <div class="quick-input-wrap">
            <input
              v-model="quickInput"
              class="quick-input"
              placeholder="输入指令，如「添加明天下午三点开会」"
              @keyup.enter="sendQuickCommand"
            />
            <button class="quick-btn" @click="sendQuickCommand" :disabled="!quickInput.trim()">发送</button>
          </div>
        </div>
        <div class="header-right">
          <div class="view-switcher">
            <button
              v-for="v in views"
              :key="v.key"
              :class="['btn-view', { active: viewMode === v.key }]"
              @click="switchView(v.key)"
            >
              {{ v.label }}
            </button>
          </div>
        </div>
      </header>

      <main class="app-main">
        <TodayPanel :events="events" />
        <div class="calendar-area">
          <CalendarView
            :events="events"
            :currentDate="currentDate"
            :viewMode="viewMode"
            :loading="loading"
            @goToDate="goToDate"
            @eventClick="handleEventClick"
          />
        </div>
      </main>
    </div>

    <VoiceButton @command-result="handleVoiceResult" />

    <!-- 按住空格说话的浮层 -->
    <RecordingOverlay
      :visible="pttVisible"
      :isRecording="pttRecording"
      :isProcessing="pttProcessing"
      :transcript="pttTranscript"
      :error="pttError"
    />

    <EventDetail
      v-if="selectedEvent"
      :event="selectedEvent"
      @close="selectedEvent = null"
      @edit="handleEdit"
      @delete="handleDelete"
    />

    <EventForm
      v-if="showForm"
      :editData="editData"
      @close="showForm = false; editData = null"
      @save="handleSave"
    />

    <VoiceModal
      v-if="voiceResult"
      :result="voiceResult"
      @close="voiceResult = null"
      @confirm="handleVoiceConfirm"
      @refresh="loadEvents"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import dayjs from 'dayjs'
import { createEvent, updateEvent, deleteEvent, sendTextCommand } from './api'
import { useCalendar } from './composables/useCalendar'
import CalendarView from './components/CalendarView.vue'
import TodayPanel from './components/TodayPanel.vue'
import VoiceButton from './components/VoiceButton.vue'
import VoiceModal from './components/VoiceModal.vue'
import EventDetail from './components/EventDetail.vue'
import EventForm from './components/EventForm.vue'
import RecordingOverlay from './components/RecordingOverlay.vue'
import { usePushToTalk } from './composables/usePushToTalk'

const { currentDate, viewMode, events, loading, prev, next, goToday, switchView, goToDate, loadEvents } = useCalendar()

// 按住空格说话
const {
  isPressing: pttVisible,
  isRecording: pttRecording,
  isProcessing: pttProcessing,
  transcript: pttTranscript,
  error: pttError
} = usePushToTalk(handleVoiceResult)

const views = [
  { key: 'month', label: '月' },
  { key: 'week', label: '周' },
  { key: 'day', label: '日' }
]

const dateTitle = computed(() => {
  const d = currentDate.value
  if (!d) return ''
  switch (viewMode.value) {
    case 'month': return d.format('YYYY年 M月')
    case 'week': return d.format('YYYY年 M月D日')
    case 'day': return d.format('YYYY年 M月 D日')
    default: return ''
  }
})

const selectedEvent = ref(null)
const showForm = ref(false)
const editData = ref(null)
const voiceResult = ref(null)
const quickInput = ref('')

async function sendQuickCommand() {
  const text = quickInput.value.trim()
  if (!text) return
  try {
    const res = await sendTextCommand(text)
    quickInput.value = ''
    handleVoiceResult(res.data)
  } catch (e) {
    alert('处理失败')
  }
}

function handleEventClick(event) {
  selectedEvent.value = event
}

function handleVoiceResult(result) {
  voiceResult.value = result
}

function handleEdit(event) {
  editData.value = event
  showForm.value = true
  selectedEvent.value = null
}

async function handleDelete(event) {
  if (confirm(`确定删除事件「${event.title}」？`)) {
    try {
      await deleteEvent(event.id)
      loadEvents()
    } catch (e) {
      alert('删除失败: ' + e.message)
    }
  }
  selectedEvent.value = null
}

async function handleSave(formData) {
  try {
    if (formData.id) {
      await updateEvent(formData.id, formData)
    } else {
      await createEvent(formData)
    }
    showForm.value = false
    editData.value = null
    loadEvents()
  } catch (e) {
    alert('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

function handleVoiceConfirm(confirmed) {
  if (confirmed && voiceResult.value) {
    loadEvents()
  }
  voiceResult.value = null
}
</script>

<style>
:root {
  --primary: #c0392b;
  --primary-light: #e74c3c;
  --bg: #f5f5f5;
  --surface: #ffffff;
  --border: #e8e8e8;
  --text: #333333;
  --text-secondary: #999999;
  --danger: #e74c3c;
  --success: #27ae60;
  --warning: #f39c12;
  --today-bg: #c0392b;
}

* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #f5f5f5; }

.app-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px;
  min-height: 100vh;
}

.app {
  width: 100%;
  max-width: 960px;
  background: var(--surface);
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  border-bottom: 1px solid var(--border);
  background: #fafafa;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 0 20px;
}

.quick-input-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 420px;
  width: 100%;
}

.quick-input {
  flex: 1;
  padding: 5px 12px;
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 13px;
  outline: none;
  background: white;
}

.quick-input:focus {
  border-color: #c0392b;
  box-shadow: 0 0 0 2px rgba(192, 57, 43, 0.08);
}

.quick-input::placeholder {
  color: #ccc;
}

.quick-btn {
  padding: 5px 14px;
  background: #c0392b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}

.quick-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.header-right {
  display: flex;
  align-items: center;
}

.btn-today {
  padding: 4px 14px;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #666;
}

.btn-today:hover { background: #f0f0f0; }

.view-switcher {
  display: flex;
  border: 1px solid var(--border);
  border-radius: 4px;
  overflow: hidden;
}

.btn-view {
  padding: 4px 12px;
  border: none;
  background: var(--surface);
  cursor: pointer;
  font-size: 12px;
  color: #666;
  border-right: 1px solid var(--border);
}

.btn-view:last-child { border-right: none; }
.btn-view.active {
  background: var(--primary);
  color: white;
}

.nav-buttons { display: flex; gap: 2px; }

.btn-nav {
  padding: 4px 10px;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
}

.btn-nav:hover { background: #f0f0f0; }

.current-date {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}

.app-main {
  display: flex;
  background: var(--surface);
}

.calendar-area {
  flex: 1;
  min-width: 0;
}
</style>
