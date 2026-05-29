<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="form-modal">
      <div class="form-header">
        <h2>{{ isEdit ? '编辑事件' : '新建事件' }}</h2>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <form class="form-body" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label>标题 *</label>
          <input v-model="form.title" class="form-input" placeholder="事件标题" required />
        </div>

        <div class="form-row">
          <div class="form-group flex-1">
            <label>开始时间 *</label>
            <input v-model="form.startTime" type="datetime-local" class="form-input" required />
          </div>
          <div class="form-group flex-1">
            <label>结束时间 *</label>
            <input v-model="form.endTime" type="datetime-local" class="form-input" required />
          </div>
        </div>

        <div class="form-group">
          <label class="checkbox-label">
            <input v-model="form.allDay" type="checkbox" />
            全天事件
          </label>
        </div>

        <div class="form-group">
          <label>地点</label>
          <input v-model="form.location" class="form-input" placeholder="可选" />
        </div>

        <div class="form-row">
          <div class="form-group flex-1">
            <label>分类</label>
            <select v-model="form.category" class="form-input">
              <option value="meeting">会议</option>
              <option value="personal">个人</option>
              <option value="work">工作</option>
              <option value="reminder">提醒</option>
              <option value="other">其他</option>
            </select>
          </div>
          <div class="form-group flex-1">
            <label>颜色</label>
            <div class="color-picker">
              <button
                v-for="c in colors"
                :key="c"
                type="button"
                :class="['color-dot', { active: form.color === c }]"
                :style="{ background: c }"
                @click="form.color = c"
              ></button>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label>重复</label>
          <select v-model="form.recurringType" class="form-input">
            <option value="">不重复</option>
            <option value="DAILY">每天</option>
            <option value="WEEKLY">每周</option>
            <option value="MONTHLY">每月</option>
            <option value="YEARLY">每年</option>
          </select>
        </div>

        <div v-if="form.recurringType === 'WEEKLY'" class="form-group">
          <label>重复日</label>
          <div class="weekday-picker">
            <button
              v-for="(day, idx) in weekDays"
              :key="idx"
              type="button"
              :class="['day-btn', { active: selectedDays.includes(String(idx + 1)) }]"
              @click="toggleDay(idx + 1)"
            >{{ day }}</button>
          </div>
        </div>

        <div class="form-group">
          <label>备注</label>
          <textarea v-model="form.description" class="form-textarea" placeholder="可选" rows="3"></textarea>
        </div>

        <div class="form-footer">
          <button type="button" class="btn-cancel" @click="$emit('close')">取消</button>
          <button type="submit" class="btn-save">{{ isEdit ? '更新' : '创建' }}</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({
  editData: { type: Object, default: null }
})

const emit = defineEmits(['close', 'save'])

const isEdit = ref(false)

const form = reactive({
  title: '',
  startTime: '',
  endTime: '',
  allDay: false,
  location: '',
  category: 'other',
  color: '',
  description: '',
  recurringType: '',
  recurringDaysOfWeek: ''
})

const selectedDays = ref([])
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const colors = ['#3b82f6', '#ec4899', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4', '#f97316']

watch(() => props.editData, (data) => {
  if (data) {
    isEdit.value = true
    form.title = data.title || ''
    form.startTime = formatForInput(data.startTime)
    form.endTime = formatForInput(data.endTime)
    form.allDay = data.allDay || false
    form.location = data.location || ''
    form.category = data.category || 'other'
    form.color = data.color || ''
    form.description = data.description || ''

    if (data.recurringRule) {
      form.recurringType = data.recurringRule.type || ''
      if (data.recurringRule.daysOfWeek) {
        selectedDays.value = data.recurringRule.daysOfWeek.split(',')
        form.recurringDaysOfWeek = data.recurringRule.daysOfWeek
      }
    }
  } else {
    isEdit.value = false
    const now = dayjs()
    form.startTime = formatForInput(now.add(1, 'hour').startOf('hour'))
    form.endTime = formatForInput(now.add(2, 'hour').startOf('hour'))
    form.title = ''
    form.allDay = false
    form.location = ''
    form.category = 'other'
    form.color = ''
    form.description = ''
    form.recurringType = ''
    selectedDays.value = []
  }
}, { immediate: true })

function formatForInput(timeStr) {
  return dayjs(timeStr).format('YYYY-MM-DDTHH:mm')
}

function toggleDay(dayNum) {
  const idx = selectedDays.value.indexOf(String(dayNum))
  if (idx >= 0) {
    selectedDays.value.splice(idx, 1)
  } else {
    selectedDays.value.push(String(dayNum))
  }
  form.recurringDaysOfWeek = selectedDays.value.join(',')
}

function handleSubmit() {
  const data = {
    title: form.title,
    startTime: dayjs(form.startTime).format('YYYY-MM-DDTHH:mm:ss'),
    endTime: dayjs(form.endTime).format('YYYY-MM-DDTHH:mm:ss'),
    allDay: form.allDay,
    location: form.location || null,
    category: form.category,
    color: form.color || null,
    description: form.description || null
  }

  if (props.editData?.id) {
    data.id = props.editData.id
  }

  // 重复规则
  if (form.recurringType) {
    data.recurringRule = {
      type: form.recurringType,
      intervalCount: 1
    }
    if (form.recurringDaysOfWeek) {
      data.recurringRule.daysOfWeek = form.recurringDaysOfWeek
    }
  }

  emit('save', data)
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.form-modal {
  width: 500px;
  max-height: 90vh;
  background: var(--surface);
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.form-header h2 { font-size: 17px; }

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: var(--text-secondary);
}

.form-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.form-group { margin-bottom: 14px; }

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
}

.form-input:focus, .form-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-textarea { resize: vertical; }

.form-row {
  display: flex;
  gap: 12px;
}

.flex-1 { flex: 1; }

.checkbox-label {
  display: flex !important;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px !important;
}

.color-picker {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  padding: 4px 0;
}

.color-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: transform 0.15s;
}

.color-dot:hover { transform: scale(1.15); }
.color-dot.active { border-color: var(--text); transform: scale(1.15); }

.weekday-picker {
  display: flex;
  gap: 4px;
}

.day-btn {
  padding: 4px 8px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: var(--surface);
  cursor: pointer;
  font-size: 12px;
}

.day-btn.active {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--border);
  margin-top: 8px;
}

.btn-cancel, .btn-save {
  padding: 10px 24px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.btn-cancel { background: #f1f5f9; color: var(--text); }
.btn-save { background: #c0392b; color: white; }
</style>
