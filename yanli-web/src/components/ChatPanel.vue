<template>
  <div class="chat-wrapper">
    <!-- FAB -->
    <button v-if="!show" class="chat-fab" @click="openChat" title="闲聊">✦</button>

    <!-- 聊天浮窗 -->
    <Transition name="slide">
      <div v-if="show" class="chat-window">
        <div class="chat-header">
          <span>◈ 闲聊</span>
          <div class="chat-header-actions">
            <select v-model="currentRole" class="role-select" @change="switchRole">
              <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.name }}</option>
            </select>
            <button class="btn-icon" @click="resetChat" title="清空对话">🗑️</button>
            <button class="btn-icon" @click="show = false">✕</button>
          </div>
        </div>

        <!-- 自定义角色输入 -->
        <div v-if="currentRole === 'custom'" class="custom-prompt">
          <input v-model="customPrompt" class="prompt-input" placeholder="输入角色设定，如「你是一只傲娇的猫娘」" @keyup.enter="applyCustomRole" />
          <button class="btn-apply" @click="applyCustomRole">应用</button>
        </div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="msgList">
          <div v-if="messages.length === 0" class="chat-empty">
            <div class="empty-avatar">◈</div>
            <p>{{ currentRole === 'custom' ? '设定好角色后开始聊天吧' : '开始聊天吧！' }}</p>
          </div>
          <div
            v-for="(msg, i) in messages"
            :key="i"
            :class="['chat-msg', msg.role === 'user' ? 'msg-user' : 'msg-ai']"
          >
            <div class="msg-avatar">{{ msg.role === 'user' ? '●' : '◈' }}</div>
            <div class="msg-bubble">{{ msg.content }}</div>
          </div>
          <div v-if="loading" class="chat-msg msg-ai">
            <div class="msg-avatar">◈</div>
            <div class="msg-bubble typing">...</div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="chat-input-area">
          <input v-model="input" class="chat-input" placeholder="输入消息..." @keyup.enter="sendMsg" :disabled="loading" />
          <button class="btn-send" @click="sendMsg" :disabled="!input.trim() || loading">发送</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import axios from 'axios'

const show = ref(false)
const input = ref('')
const loading = ref(false)
const roles = ref([])
const currentRole = ref('default')
const customPrompt = ref('')
const msgList = ref(null)

// 每个角色独立的消息列表
const messagesByRole = ref({})
const messages = computed({
  get: () => messagesByRole.value[currentRole.value] || [],
  set: (v) => { messagesByRole.value[currentRole.value] = v }
})

let currentRolePrompt = ''

onMounted(async () => {
  try { const r = await axios.get('/api/chat/roles'); roles.value = r.data } catch (e) { /* ignore */ }
  const saved = localStorage.getItem('chat_role')
  if (saved) {
    currentRole.value = saved
    const role = roles.value.find(r => r.id === saved)
    if (role && role.id !== 'custom') currentRolePrompt = role.prompt
  }
})

function openChat() {
  show.value = true
  scrollBottom()
}

async function sendMsg() {
  const msg = input.value.trim()
  if (!msg || loading.value) return
  input.value = ''
  const list = messagesByRole.value[currentRole.value] || []
  list.push({ role: 'user', content: msg })
  messagesByRole.value[currentRole.value] = list
  loading.value = true
  scrollBottom()

  try {
    const body = { message: msg, roleId: currentRole.value }
    if (currentRolePrompt) body.rolePrompt = currentRolePrompt
    const res = await axios.post('/api/chat/send', body)
    list.push({ role: 'assistant', content: res.data.reply || '...' })
  } catch (e) {
    list.push({ role: 'assistant', content: '抱歉，出了点问题' })
  } finally {
    loading.value = false
    scrollBottom()
  }
}

async function resetChat() {
  messagesByRole.value[currentRole.value] = []
  try { await axios.post('/api/chat/reset', { roleId: currentRole.value }) } catch (e) { /* ignore */ }
}

function switchRole() {
  localStorage.setItem('chat_role', currentRole.value)
  const role = roles.value.find(r => r.id === currentRole.value)
  if (role && role.id !== 'custom') {
    currentRolePrompt = role.prompt
    customPrompt.value = ''
  } else if (role && role.id === 'custom') {
    currentRolePrompt = ''
  }
}

function applyCustomRole() {
  currentRolePrompt = customPrompt.value.trim() || '你是用户的AI智能助手。'
  messagesByRole.value[currentRole.value] = []
}

function scrollBottom() {
  nextTick(() => {
    const el = msgList.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style scoped>
.chat-wrapper { position: fixed; bottom: 64px; right: 16px; z-index: 99; }
.chat-fab {
  width: 42px; height: 42px; border-radius: 50%; border: none;
  background: #c0392b; color: white; font-size: 18px; cursor: pointer;
  box-shadow: 0 2px 10px rgba(192,57,43,0.3);
}

.chat-window {
  width: 360px; height: 500px; background: white; border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12); display: flex; flex-direction: column; overflow: hidden;
}

.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; background: #c0392b; color: white; font-weight: 600; font-size: 14px;
}
.chat-header-actions { display: flex; align-items: center; gap: 6px; }
.role-select { padding: 3px 6px; border-radius: 4px; border: none; font-size: 12px; background: rgba(255,255,255,0.2); color: white; outline: none; cursor: pointer; }
.role-select option { color: #333; }
.btn-icon { background: none; border: none; color: white; font-size: 16px; cursor: pointer; opacity: 0.8; }
.btn-icon:hover { opacity: 1; }

.custom-prompt { display: flex; gap: 6px; padding: 8px 12px; background: #fef5f5; border-bottom: 1px solid #f0d0d0; }
.prompt-input { flex: 1; padding: 5px 10px; border: 1px solid #e8e8e8; border-radius: 4px; font-size: 12px; outline: none; }
.btn-apply { padding: 5px 12px; background: #c0392b; color: white; border: none; border-radius: 4px; font-size: 12px; cursor: pointer; }

.chat-messages { flex: 1; overflow-y: auto; padding: 12px; display: flex; flex-direction: column; gap: 10px; }
.chat-empty { text-align: center; padding: 40px 20px; color: #ccc; }
.empty-avatar { font-size: 48px; margin-bottom: 8px; }

.chat-msg { display: flex; gap: 8px; align-items: flex-start; }
.msg-user { flex-direction: row-reverse; }
.msg-avatar { width: 32px; height: 32px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }
.msg-bubble { max-width: 75%; padding: 8px 12px; border-radius: 12px; font-size: 13px; line-height: 1.4; word-break: break-word; }
.msg-user .msg-bubble { background: #c0392b; color: white; border-bottom-right-radius: 4px; }
.msg-ai .msg-bubble { background: #f0f0f0; color: #333; border-bottom-left-radius: 4px; }
.typing::after { content: ''; animation: dots 1.4s infinite; }
@keyframes dots { 0%,20% { content: '.' } 40% { content: '..' } 60%,100% { content: '...' } }

.chat-input-area { display: flex; gap: 8px; padding: 10px 12px; border-top: 1px solid #f0f0f0; }
.chat-input { flex: 1; padding: 8px 12px; border: 1px solid #e8e8e8; border-radius: 6px; font-size: 13px; outline: none; }
.chat-input:focus { border-color: #c0392b; }
.btn-send { padding: 8px 16px; background: #c0392b; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
.btn-send:disabled { opacity: 0.5; cursor: not-allowed; }

.slide-enter-active, .slide-leave-active { transition: all 0.25s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); }
</style>
