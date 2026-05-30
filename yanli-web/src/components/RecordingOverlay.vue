<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="overlay">
        <div class="overlay-bg"></div>
        <div class="recording-box">
          <!-- 按住说话的圆形动画 -->
          <div :class="['mic-circle', { active: isRecording }]">
            <span class="mic-icon">◉</span>
          </div>

          <div v-if="isRecording" class="status">
            {{ transcript || '正在聆听...' }}
          </div>
          <div v-else-if="isProcessing" class="status processing">
            <div class="spinner"></div>
            处理中...
          </div>
          <div v-else-if="error" class="status error">
            {{ error }}
          </div>
          <div v-else class="status hint">
            按住 <kbd>空格键</kbd> 说话，松开发送
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
defineProps({
  visible: Boolean,
  isRecording: Boolean,
  isProcessing: Boolean,
  transcript: String,
  error: String
})
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.overlay-bg {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
}

.recording-box {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  min-width: 260px;
}

.mic-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.mic-circle.active {
  background: #c0392b;
  animation: micPulse 1.2s infinite;
}

.mic-icon {
  font-size: 32px;
}

@keyframes micPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(192, 57, 43, 0.4); }
  50% { box-shadow: 0 0 0 20px rgba(192, 57, 43, 0); }
}

.status {
  font-size: 15px;
  color: #333;
  text-align: center;
  min-height: 24px;
}

.status.hint {
  font-size: 13px;
  color: #999;
}

.status.error {
  color: #e74c3c;
  font-size: 13px;
}

.status.processing {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid #e8e8e8;
  border-top-color: #c0392b;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

kbd {
  padding: 2px 8px;
  background: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: inherit;
  font-size: 12px;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
