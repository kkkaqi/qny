import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// ==================== 事件 API ====================

/** 获取时间范围内的事件 */
export function getEventsByRange(start, end) {
  return api.get('/events/range', { params: { start, end } })
}

/** 按日期获取事件 */
export function getEventsByDate(date) {
  return api.get(`/events/date/${date}`)
}

/** 搜索事件 */
export function searchEvents(keyword) {
  return api.get('/events/search', { params: { keyword } })
}

/** 获取所有事件 */
export function getAllEvents() {
  return api.get('/events')
}

/** 获取事件详情 */
export function getEvent(id) {
  return api.get(`/events/${id}`)
}

/** 创建事件 */
export function createEvent(data) {
  return api.post('/events', data)
}

/** 更新事件 */
export function updateEvent(id, data) {
  return api.put(`/events/${id}`, data)
}

/** 删除事件 */
export function deleteEvent(id) {
  return api.delete(`/events/${id}`)
}

// ==================== 语音 API ====================

/** 上传音频文件进行识别 */
export function uploadAudio(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/voice/audio', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

/** 发送文字指令 */
export function sendTextCommand(text) {
  return api.post('/voice/text', { text })
}

/** 获取语音服务状态 */
export function getVoiceStatus() {
  return api.get('/voice/status')
}

export default api
