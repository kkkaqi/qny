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
            <input v-model="quickInput" class="quick-input" placeholder="输入指令，如「添加明天下午三点开会」" @keyup.enter="sendQuickCommand" />
            <button class="quick-btn" @click="sendQuickCommand" :disabled="!quickInput.trim()">发送</button>
          </div>
        </div>
        <div class="header-right">
          <div class="view-switcher">
            <button v-for="v in views" :key="v.key" :class="['btn-view', { active: viewMode === v.key }]" @click="switchView(v.key)">{{ v.label }}</button>
          </div>
          <span class="user-info">{{ user.nickname || user.username }}</span>
          <button class="btn-logout" @click="handleLogout">退出</button>
        </div>
      </header>
      <main class="app-main">
        <TodayPanel :events="events" />
        <div class="calendar-area">
          <CalendarView :events="events" :currentDate="currentDate" :viewMode="viewMode" :loading="loading" @goToDate="goToDate" @eventClick="(e) => selectedEvent = e" />
        </div>
      </main>
    </div>
    <VoiceButton @command-result="(r) => voiceResult = r" />
    <RecordingOverlay :visible="pttVisible" :isRecording="pttRecording" :isProcessing="pttProcessing" :transcript="pttTranscript" :error="pttError" />
    <EventDetail v-if="selectedEvent" :event="selectedEvent" @close="selectedEvent = null" @edit="handleEdit" @delete="handleDelete" />
    <EventForm v-if="showForm" :editData="editData" @close="showForm = false; editData = null" @save="handleSave" />
    <VoiceModal v-if="voiceResult" :result="voiceResult" @close="voiceResult = null; loadEvents()" @confirm="handleVoiceConfirm" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { createEvent, updateEvent, deleteEvent, sendTextCommand, fetchUser } from '../api'
import { useCalendar } from '../composables/useCalendar'
import CalendarView from './CalendarView.vue'
import TodayPanel from './TodayPanel.vue'
import VoiceButton from './VoiceButton.vue'
import VoiceModal from './VoiceModal.vue'
import EventDetail from './EventDetail.vue'
import EventForm from './EventForm.vue'
import RecordingOverlay from './RecordingOverlay.vue'
import { usePushToTalk } from '../composables/usePushToTalk'

const router = useRouter()
const user = ref({ username: '', nickname: '' })
fetchUser().then(u => user.value = u).catch(() => router.replace('/login'))

const { currentDate, viewMode, events, loading, prev, next, goToday, switchView, goToDate, loadEvents } = useCalendar()
const { isPressing: pttVisible, isRecording: pttRecording, isProcessing: pttProcessing, transcript: pttTranscript, error: pttError } = usePushToTalk(handleVoiceResult)

const views = [{ key: 'month', label: '月' }, { key: 'week', label: '周' }, { key: 'day', label: '日' }]
const dateTitle = computed(() => {
  const d = currentDate.value; if (!d) return ''
  switch (viewMode.value) { case 'month': return d.format('YYYY年 M月'); case 'week': return d.format('YYYY年 M月D日'); case 'day': return d.format('YYYY年 M月 D日'); default: return '' }
})

const selectedEvent = ref(null), showForm = ref(false), editData = ref(null), voiceResult = ref(null), quickInput = ref('')

async function sendQuickCommand() { const t = quickInput.value.trim(); if (!t) return; try { const r = await sendTextCommand(t); quickInput.value = ''; voiceResult.value = r.data } catch { alert('处理失败') } }
function handleVoiceResult(r) { voiceResult.value = r }
function handleEdit(e) { editData.value = e; showForm.value = true; selectedEvent.value = null }
async function handleDelete(e) { if (!confirm('确定删除「' + e.title + '」？')) return; try { await deleteEvent(e.id); loadEvents() } catch { alert('删除失败') } selectedEvent.value = null }
async function handleSave(d) { try { d.id ? await updateEvent(d.id, d) : await createEvent(d); showForm.value = false; editData.value = null; loadEvents() } catch (e) { alert('保存失败: ' + (e.response?.data?.message || e.message)) } }
function handleVoiceConfirm(c) { if (c) loadEvents(); voiceResult.value = null }
async function handleLogout() { await axios.post('/api/auth/logout'); router.replace('/login') }
</script>

<style scoped>
.app-wrapper { display: flex; justify-content: center; padding: 20px; min-height: 100vh; }
.app { width: 100%; max-width: 1040px; background: var(--surface); border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); overflow: hidden; display: flex; flex-direction: column; }
.app-header { display: flex; align-items: center; justify-content: space-between; padding: 8px 16px; border-bottom: 1px solid var(--border); background: #fafafa; gap: 8px; }
.header-left { display: flex; align-items: center; gap: 6px; }
.header-center { flex: 1; display: flex; justify-content: center; }
.quick-input-wrap { display: flex; align-items: center; gap: 6px; max-width: 380px; width: 100%; }
.quick-input { flex: 1; padding: 5px 10px; border: 1px solid var(--border); border-radius: 4px; font-size: 13px; outline: none; background: white; }
.quick-input:focus { border-color: #c0392b; box-shadow: 0 0 0 2px rgba(192,57,43,0.08); }
.quick-input::placeholder { color: #ccc; }
.quick-btn { padding: 5px 12px; background: #c0392b; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; white-space: nowrap; }
.quick-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.header-right { display: flex; align-items: center; gap: 10px; }
.btn-today { padding: 4px 12px; border: 1px solid var(--border); background: var(--surface); border-radius: 4px; cursor: pointer; font-size: 12px; color: #666; }
.btn-today:hover { background: #f0f0f0; }
.view-switcher { display: flex; border: 1px solid var(--border); border-radius: 4px; overflow: hidden; }
.btn-view { padding: 4px 10px; border: none; background: var(--surface); cursor: pointer; font-size: 12px; color: #666; border-right: 1px solid var(--border); }
.btn-view:last-child { border-right: none; }
.btn-view.active { background: var(--primary); color: white; }
.btn-nav { padding: 4px 10px; border: 1px solid var(--border); background: var(--surface); border-radius: 4px; cursor: pointer; font-size: 13px; color: #666; }
.btn-nav:hover { background: #f0f0f0; }
.current-date { font-size: 16px; font-weight: 600; color: var(--text); }
.user-info { font-size: 12px; color: #999; }
.btn-logout { padding: 4px 10px; border: 1px solid var(--border); background: var(--surface); border-radius: 4px; cursor: pointer; font-size: 12px; color: #999; }
.btn-logout:hover { color: #c0392b; border-color: #c0392b; }
.app-main { display: flex; background: var(--surface); }
.calendar-area { flex: 1; min-width: 0; }
</style>
