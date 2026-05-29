import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.response.use(
  res => res,
  error => {
    if (error.response?.status === 401 && !window.location.pathname.includes('/login')) {
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export function fetchUser() { return api.get('/auth/me').then(r => r.data) }
export function getEventsByRange(start, end) { return api.get('/events/range', { params: { start, end } }) }
export function getEventsByDate(date) { return api.get('/events/date/' + date) }
export function searchEvents(keyword) { return api.get('/events/search', { params: { keyword } }) }
export function getAllEvents() { return api.get('/events') }
export function getEvent(id) { return api.get('/events/' + id) }
export function createEvent(data) { return api.post('/events', data) }
export function updateEvent(id, data) { return api.put('/events/' + id, data) }
export function deleteEvent(id) { return api.delete('/events/' + id) }
export function uploadAudio(file) { const fd = new FormData(); fd.append('file', file); return api.post('/voice/audio', fd, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 60000 }) }
export function sendTextCommand(text) { return api.post('/voice/text', { text }) }
export function getVoiceStatus() { return api.get('/voice/status') }
export default api
