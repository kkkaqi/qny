<template>
  <div class="voice-button-container">
    <!-- 语音指令输入面板 -->
    <div v-if="showPanel" class="voice-panel">
      <div class="panel-header">
        <span>◉ 语音输入</span>
        <button class="close-btn" @click="closePanel">✕</button>
      </div>

      <!-- 语音识别状态 -->
      <div v-if="isListening" class="listening-indicator">
        <div class="pulse-ring"></div>
        <p>{{ mode === 'webspeech' ? '正在聆听...' : '正在录音...' }}</p>
        <button class="btn-stop" @click="stopListening">停止</button>
      </div>

      <!-- 处理中 -->
      <div v-else-if="isProcessing" class="processing">
        <div class="spinner-sm"></div>
        <p>正在处理...</p>
      </div>

      <!-- 文字输入区域 -->
      <div v-else class="panel-body">
        <div class="input-row">
          <input
            ref="textInput"
            v-model="textInput"
            class="text-input"
            placeholder="输入指令，例如「添加明天下午三点开会」"
            @keyup.enter="sendTextCmd"
          />
          <button class="btn-send" @click="sendTextCmd" :disabled="!textInput.trim()">
            发送
          </button>
        </div>

        <div class="divider">
          <span>或者</span>
        </div>

        <button class="btn-voice" @click="startListening" :disabled="isProcessing">
          <span class="mic-icon">◉</span>
          <span>{{ hasWebSpeech ? '点击开始语音输入' : '点击开始录音上传' }}</span>
        </button>

        <div class="shortcut-hint">
          提示：按住 <kbd>空格键</kbd> 可以直接说话
        </div>
        <div v-if="!hasWebSpeech" class="hint">
          当前浏览器不支持语音识别，将使用录音上传模式
        </div>
      </div>
    </div>

    <!-- FAB 浮动按钮 -->
    <button
      v-if="!showPanel"
      :class="['fab', { 'fab-pulse': !showPanel }]"
      @click="togglePanel"
      title="语音助手"
    >
      <span class="fab-icon">◉</span>
    </button>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useVoice } from '../composables/useVoice'

const emit = defineEmits(['commandResult'])

const {
  isListening, isProcessing, result, error,
  mode, hasWebSpeech,
  startListening, stopListening, sendText, reset
} = useVoice()

const showPanel = ref(false)
const textInput = ref('')
const textInputRef = ref(null)

function togglePanel() {
  showPanel.value = true
  reset()
  nextTick(() => {
    textInputRef.value?.focus()
  })
}

function closePanel() {
  showPanel.value = false
  reset()
  textInput.value = ''
}

function sendTextCmd() {
  if (textInput.value.trim()) {
    sendText(textInput.value.trim())
    textInput.value = ''
  }
}

// 监听结果，通知父组件
watch(result, (val) => {
  if (val) {
    emit('commandResult', val)
    // 延迟关闭面板让用户看到结果
    setTimeout(() => {
      textInput.value = ''
    }, 500)
  }
})

watch(error, (val) => {
  if (val) {
    console.error('语音错误:', val)
  }
})

watch(isListening, (val) => {
  if (!val && textInputRef.value) {
    nextTick(() => textInputRef.value?.focus())
  }
})
</script>

<style scoped>
.voice-button-container {
  position: fixed;
  bottom: 16px;
  right: 16px;
  z-index: 100;
}

/* FAB 按钮 */
.fab {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: var(--primary);
  color: white;
  font-size: 18px;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(192, 57, 43, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s, box-shadow 0.15s;
}

.fab:hover {
  transform: scale(1.08);
  box-shadow: 0 4px 14px rgba(192, 57, 43, 0.4);
}

.fab-pulse {
  animation: fabPulse 2s infinite;
}

@keyframes fabPulse {
  0%, 100% { box-shadow: 0 2px 10px rgba(192, 57, 43, 0.3); }
  50% { box-shadow: 0 4px 16px rgba(192, 57, 43, 0.5); }
}

.fab-icon { line-height: 1; }

/* 面板 */
.voice-panel {
  width: 360px;
  background: var(--surface);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #c0392b;
  color: white;
  font-size: 14px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
  opacity: 0.8;
}

.close-btn:hover { opacity: 1; }

.panel-body {
  padding: 18px;
}

.input-row {
  display: flex;
  gap: 8px;
}

.text-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.text-input:focus {
  border-color: #c0392b;
  box-shadow: 0 0 0 3px rgba(192, 57, 43, 0.1);
}

.btn-send {
  padding: 10px 18px;
  background: #c0392b;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
}

.btn-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.divider {
  display: flex;
  align-items: center;
  margin: 16px 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border);
}

.divider span {
  padding: 0 12px;
}

.btn-voice {
  width: 100%;
  padding: 14px;
  background: #fef5f5;
  border: 2px dashed #e74c3c;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  color: #c0392b;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.15s;
}

.btn-voice:hover {
  background: #fde8e8;
}

.mic-icon { font-size: 20px; }

.shortcut-hint {
  margin-top: 14px;
  font-size: 12px;
  color: #999;
  text-align: center;
}

.shortcut-hint kbd {
  padding: 1px 6px;
  background: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 3px;
  font-family: inherit;
  font-size: 11px;
}

.hint {
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
}

/* 聆听状态 */
.listening-indicator {
  padding: 30px;
  text-align: center;
}

.pulse-ring {
  width: 60px;
  height: 60px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: #c0392b;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(192, 57, 43, 0.4); }
  100% { box-shadow: 0 0 0 24px rgba(192, 57, 43, 0); }
}

.btn-stop {
  margin-top: 16px;
  padding: 8px 24px;
  background: var(--danger);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.processing {
  padding: 30px;
  text-align: center;
}

.spinner-sm {
  width: 28px;
  height: 28px;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
