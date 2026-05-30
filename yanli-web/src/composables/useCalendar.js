import { ref, computed } from 'vue'
import dayjs from 'dayjs'
import { getEventsByRange, getAllEvents } from '../api'

/**
 * 日历状态管理 Composable
 */
export function useCalendar() {
  const currentDate = ref(dayjs())
  const viewMode = ref('month')  // 'month' | 'week' | 'day'
  const events = ref([])
  const loading = ref(false)

  /** 当前视图的日期范围 */
  const dateRange = computed(() => {
    const d = currentDate.value
    switch (viewMode.value) {
      case 'month':
        return {
          start: d.startOf('month').startOf('week').format('YYYY-MM-DDTHH:mm:ss'),
          end: d.endOf('month').endOf('week').format('YYYY-MM-DDTHH:mm:ss')
        }
      case 'week':
        return {
          start: d.startOf('week').format('YYYY-MM-DDTHH:mm:ss'),
          end: d.endOf('week').format('YYYY-MM-DDTHH:mm:ss')
        }
      case 'day':
        return {
          start: d.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
          end: d.endOf('day').format('YYYY-MM-DDTHH:mm:ss')
        }
      default:
        return {}
    }
  })

  /** 切换到上一个月/周/日 */
  function prev() {
    const unit = viewMode.value === 'day' ? 'day' : viewMode.value
    currentDate.value = currentDate.value.subtract(1, unit)
    loadEvents()
  }

  /** 切换到下一个月/周/日 */
  function next() {
    const unit = viewMode.value === 'day' ? 'day' : viewMode.value
    currentDate.value = currentDate.value.add(1, unit)
    loadEvents()
  }

  /** 回到今天 */
  function goToday() {
    currentDate.value = dayjs()
    loadEvents()
  }

  /** 切换视图 */
  function switchView(mode) {
    viewMode.value = mode
    loadEvents()
  }

  /** 跳转到指定日期 */
  function goToDate(date) {
    currentDate.value = dayjs(date)
    if (viewMode.value === 'month') {
      viewMode.value = 'day'
    }
    loadEvents()
  }

  /** 加载事件 */
  async function loadEvents() {
    loading.value = true
    try {
      const range = dateRange.value
      if (range.start && range.end) {
        const res = await getEventsByRange(range.start, range.end)
        events.value = res.data
      } else {
        const res = await getAllEvents()
        events.value = res.data
      }
    } catch (e) {
      console.error('加载事件失败:', e)
      events.value = []
    } finally {
      loading.value = false
    }
  }

  // 初始加载
  loadEvents()

  return {
    currentDate,
    viewMode,
    events,
    loading,
    dateRange,
    prev,
    next,
    goToday,
    switchView,
    goToDate,
    loadEvents
  }
}
