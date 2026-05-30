import { ref, onMounted, onUnmounted } from 'vue'
import { sendTextCommand } from '../api'

/**
 * 按住空格说话 (Push-to-Talk) Composable
 *
 * 使用浏览器 Web Speech API，按住空格键开始录音，松开结束。
 * 当焦点在 input/textarea 时不触发。
 */
export function usePushToTalk(onResult, getContextDate) {
  const isPressing = ref(false)     // 正在按住空格
  const isRecording = ref(false)    // 实际录音中
  const isProcessing = ref(false)
  const transcript = ref('')
  const error = ref(null)

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  let recognition = null
  let pressStartTime = 0
  let stream = null

  if (SpeechRecognition) {
    recognition = new SpeechRecognition()
    recognition.lang = 'zh-CN'
    recognition.interimResults = true       // 开启中间结果，实时显示
    recognition.continuous = true
    recognition.maxAlternatives = 1
  }

  function shouldIgnore() {
    const tag = document.activeElement?.tagName?.toLowerCase()
    return tag === 'input' || tag === 'textarea' || document.activeElement?.isContentEditable
  }

  function onKeyDown(e) {
    if (e.code === 'Space' && !e.repeat && !shouldIgnore()) {
      e.preventDefault()
      pressStartTime = Date.now()
      isPressing.value = true

      if (SpeechRecognition) {
        startWebSpeech()
      } else {
        startMediaRecord()
      }
    }
  }

  function onKeyUp(e) {
    if (e.code === 'Space' && isPressing.value) {
      isPressing.value = false
      const duration = Date.now() - pressStartTime

      if (duration < 300) {
        // 按得太短，可能是误触，取消
        cancelRecording()
        return
      }

      stopRecording()
    }
  }

  /** 浏览器原生语音识别 */
  function startWebSpeech() {
    isRecording.value = true
    error.value = null
    transcript.value = ''

    recognition.onresult = (event) => {
      // 实时显示中间结果
      let interim = ''
      let final = ''

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i]
        if (result.isFinal) {
          final += result[0].transcript
        } else {
          interim += result[0].transcript
        }
      }
      transcript.value = final || interim
    }

    recognition.onerror = (event) => {
      isRecording.value = false
      if (event.error === 'not-allowed') {
        error.value = '请允许麦克风权限后重试'
      } else if (event.error !== 'no-speech') {
        error.value = '识别出错: ' + event.error
      }
    }

    recognition.onend = () => {
      isRecording.value = false
    }

    try {
      recognition.start()
    } catch (e) {
      // 可能已在运行中
      isRecording.value = false
    }
  }

  function cancelRecording() {
    isPressing.value = false
    isRecording.value = false
    transcript.value = ''
    if (recognition) {
      try { recognition.abort() } catch (e) { /* ignore */ }
    }
    if (stream) {
      stream.getTracks().forEach(t => t.stop())
      stream = null
    }
  }

  /** 停止录音并处理 */
  async function stopRecording() {
    if (recognition) {
      try { recognition.stop() } catch (e) { /* ignore */ }
    }

    // 等一小段时间让最后的识别结果进来
    await new Promise(r => setTimeout(r, 200))

    const text = transcript.value.trim()
    if (!text) {
      error.value = '未识别到语音内容'
      return
    }

    isProcessing.value = true
    try {
      const res = await sendTextCommand(text, getContextDate ? getContextDate() : null)
      if (onResult) onResult(res.data)
    } catch (e) {
      error.value = '处理失败: ' + (e.response?.data?.errorMessage || e.message)
    } finally {
      isProcessing.value = false
    }
  }

  // ---- 录音模式（不支持 Web Speech 时的降级） ----
  let mediaRecorder = null
  let audioChunks = []
  let streamRef = null

  async function startMediaRecord() {
    try {
      streamRef = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaRecorder = new MediaRecorder(streamRef, { mimeType: 'audio/webm' })
      audioChunks = []

      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) audioChunks.push(e.data)
      }

      mediaRecorder.start()
      isRecording.value = true
      error.value = null
      transcript.value = '(录音中...)'
    } catch (e) {
      error.value = '无法访问麦克风'
      cancelRecording()
    }
  }

  function stopMediaRecord() {
    return new Promise((resolve) => {
      if (!mediaRecorder) return resolve()
      mediaRecorder.onstop = async () => {
        isRecording.value = false
        isProcessing.value = true
        const blob = new Blob(audioChunks, { type: 'audio/webm' })
        const file = new File([blob], 'recording.webm', { type: 'audio/webm' })
        try {
          const { uploadAudio } = await import('../api')
          const res = await uploadAudio(file)
          if (onResult) onResult(res.data)
        } catch (e) {
          error.value = '上传识别失败'
        } finally {
          isProcessing.value = false
          if (streamRef) {
            streamRef.getTracks().forEach(t => t.stop())
            streamRef = null
          }
        }
        resolve()
      }
      mediaRecorder.stop()
    })
  }

  // 覆盖 stopRecording 以处理录音模式
  const originalStopRecording = stopRecording
  stopRecording = async function() {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
      await stopMediaRecord()
    } else {
      await originalStopRecording()
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', onKeyDown)
    window.addEventListener('keyup', onKeyUp)
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', onKeyDown)
    window.removeEventListener('keyup', onKeyUp)
    cancelRecording()
  })

  return {
    isPressing,
    isRecording,
    isProcessing,
    transcript,
    error,
    hasWebSpeech: !!SpeechRecognition
  }
}
