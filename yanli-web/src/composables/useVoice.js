import { ref } from 'vue'
import { uploadAudio, sendTextCommand } from '../api'

export function useVoice() {
  const isListening = ref(false)
  const isProcessing = ref(false)
  const transcript = ref('')
  const result = ref(null)
  const error = ref(null)
  const mode = ref('webspeech')

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  let recognition = null
  let mediaRecorder = null

  /** 开始语音监听 */
  function startListening() {
    error.value = null
    result.value = null
    transcript.value = ''
    isListening.value = false

    if (SpeechRecognition) {
      startWebSpeech()
    } else {
      startRecording()
    }
  }

  /** 浏览器原生语音识别 */
  function startWebSpeech() {
    // 每次新建实例，避免旧实例状态异常
    if (recognition) {
      try { recognition.abort() } catch (e) { /* ignore */ }
      recognition = null
    }

    recognition = new SpeechRecognition()
    recognition.lang = 'zh-CN'
    recognition.interimResults = false
    recognition.continuous = false
    recognition.maxAlternatives = 1

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
      } else if (event.error === 'aborted') {
        // 用户主动停止，不算错误
      } else {
        error.value = '语音识别出错: ' + event.error
      }
    }

    recognition.onend = () => {
      isListening.value = false
    }

    try {
      recognition.start()
      isListening.value = true
      mode.value = 'webspeech'
    } catch (e) {
      error.value = '无法启动语音识别，请刷新页面重试'
      isListening.value = false
    }
  }

  /** 停止监听 */
  function stopListening() {
    if (recognition) {
      try { recognition.abort() } catch (e) { /* ignore */ }
    }
    if (mediaRecorder && mediaRecorder.state === 'recording') {
      mediaRecorder.stop()
    }
    isListening.value = false
  }

  // ---- 录音上传模式（备用） ----
  let audioChunks = []
  let streamRef = null

  async function startRecording() {
    try {
      streamRef = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaRecorder = new MediaRecorder(streamRef, { mimeType: 'audio/webm' })
      audioChunks = []

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) audioChunks.push(event.data)
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
        }
      }

      isListening.value = true
      mode.value = 'upload'
      mediaRecorder.start()

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

  function reset() {
    isListening.value = false
    isProcessing.value = false
    transcript.value = ''
    result.value = null
    error.value = null
  }

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
