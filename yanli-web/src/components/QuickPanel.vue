<template>
  <div class="quick-panel">
    <button class="qp-btn" @click="$emit('addToday')" title="添加今天事件">
      <span class="qp-icon">＋</span>
    </button>
    <button class="qp-btn" @click="$emit('viewToday')" title="查看今天安排">
      <span class="qp-icon">☰</span>
    </button>
  </div>
</template>

<script setup>
defineEmits(['addToday', 'viewToday'])
</script>

<style scoped>
.quick-panel {
  position: fixed; bottom: 112px; right: 16px; z-index: 98;
  display: flex; flex-direction: column; gap: 8px;
}

.qp-btn {
  width: 40px; height: 40px; border-radius: 50%; border: none;
  background: var(--primary, #c0392b); color: #fff; cursor: pointer;
  box-shadow: 0 2px 10px rgba(192,57,43,0.3); font-size: 16px;
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.15s, box-shadow 0.15s;
}
.qp-btn:hover { transform: scale(1.1); box-shadow: 0 4px 16px rgba(192,57,43,0.45); }
.qp-icon { line-height: 1; font-weight: bold; }
</style>
