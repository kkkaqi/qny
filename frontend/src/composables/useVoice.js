import { ref, onUnmounted } from 'vue'
import { uploadAudio, sendTextCommand } from '../api'

/**
 * 语音交互 Composable
 *
 * 双模式策略：
 * 1. Web Speech API（优先）：浏览器原生语音识别，免费且无需后端ASR密钥
 * 2. 音频上传（备用）：录制音频上传后端，通过云ASR服务识别
 */
export function useVoice() {
  const isListening = ref(false)
  const isProcessing = ref(false)
  const transcript = ref('')        // 当前识别的文字
  const result = ref(null)          // 后端处理结果
  const error = ref(null)
  const mode = ref('webspeech')     // 'webspeech' | 'upload'

  // Web Speech API 支持检测
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  let recognition = null

  if (SpeechRecognition) {
    recognition = new SpeechRecognition()
    recognition.lang = 'zh-CN'
    recognition.interimResults = false
    recognition.continuous = false
    recognition.maxAlternatives = 1
  }

  /** 开始语音监听 */
  function startListening() {
    error.value = null
    result.value = null
    transcript.value = ''

    if (SpeechRecognition) {
      // 优先使用 Web Speech API
      startWebSpeech()
    } else {
      // 降级使用录音上传
      startRecording()
    }
  }

  /** 浏览器原生语音识别 */
  function startWebSpeech() {
    isListening.value = true
    mode.value = 'webspeech'

    recognition.onresult = async (event) => {
      const text = event.results[0][0].transcript
      transcript.value = text
      isListening.value = false
      isProcessing.value = true

      try {
        const res = await sendTextCommand(text)
        result.value = res.data
      } catch (e) {
        error.value = '处理失败: ' + (e.response?.data?.errorMessage || e.message)
      } finally {
        isProcessing.value = false
      }
    }

    recognition.onerror = (event) => {
      isListening.value = false
      if (event.error === 'not-allowed') {
        error.value = '请允许麦克风权限后重试'
      } else if (event.error === 'no-speech') {
        error.value = '未检测到语音，请重试'
      } else {
        error.value = '语音识别出错: ' + event.error
      }
    }

    recognition.onend = () => {
      isListening.value = false
    }

    recognition.start()
  }

  /** 停止监听 */
  function stopListening() {
    if (recognition && isListening.value) {
      recognition.stop()
      isListening.value = false
    }
    if (mediaRecorder && isListening.value) {
      mediaRecorder.stop()
      isListening.value = false
    }
  }

  // ---- 录音上传模式（备用） ----
  let mediaRecorder = null
  let audioChunks = []

  async function startRecording() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
      audioChunks = []

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunks.push(event.data)
        }
      }

      mediaRecorder.onstop = async () => {
        isListening.value = false
        isProcessing.value = true

        const audioBlob = new Blob(audioChunks, { type: 'audio/webm' })
        try {
          const file = new File([audioBlob], 'recording.webm', { type: 'audio/webm' })
          const res = await uploadAudio(file)
          result.value = res.data
        } catch (e) {
          error.value = '上传识别失败: ' + e.message
        } finally {
          isProcessing.value = false
          // 释放麦克风
          stream.getTracks().forEach(t => t.stop())
        }
      }

      isListening.value = true
      mode.value = 'upload'
      mediaRecorder.start()

      // 最长录制 10 秒
      setTimeout(() => {
        if (mediaRecorder && mediaRecorder.state === 'recording') {
          mediaRecorder.stop()
        }
      }, 10000)

    } catch (e) {
      isListening.value = false
      error.value = '无法访问麦克风: ' + e.message
    }
  }

  /** 直接发送文字指令 */
  async function sendText(text) {
    if (!text.trim()) return
    isProcessing.value = true
    error.value = null
    result.value = null
    transcript.value = text

    try {
      const res = await sendTextCommand(text)
      result.value = res.data
    } catch (e) {
      error.value = '处理失败: ' + (e.response?.data?.errorMessage || e.message)
    } finally {
      isProcessing.value = false
    }
  }

  /** 重置状态 */
  function reset() {
    isListening.value = false
    isProcessing.value = false
    transcript.value = ''
    result.value = null
    error.value = null
  }

  onUnmounted(() => {
    stopListening()
  })

  return {
    isListening,
    isProcessing,
    transcript,
    result,
    error,
    mode,
    hasWebSpeech: !!SpeechRecognition,
    startListening,
    stopListening,
    sendText,
    reset
  }
}
