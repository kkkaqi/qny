<template>
  <router-view />
</template>

<style>
:root { --primary: #c0392b; --primary-light: #e74c3c; --bg: #f5f5f5; --surface: #ffffff; --border: #e8e8e8; --text: #333333; --text-secondary: #999999; --danger: #e74c3c; --success: #27ae60; --warning: #f39c12; --today-bg: #c0392b; }
:root.dark {
  --bg: #1a1a2e; --surface: #16213e; --border: #2a2a4a;
  --text: #e0e0e0; --text-secondary: #999;
}
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif; background: var(--bg); color: var(--text); }

/* 暗色模式全局覆盖 */
.dark .app-header, .dark .app-main { background: var(--surface); border-color: var(--border); }
.dark .btn-today, .dark .btn-nav { background: var(--surface); border-color: var(--border); color: var(--text); }
.dark .btn-today:hover, .dark .btn-nav:hover { background: #222244; }
.dark .quick-input { background: #1a1a2e; border-color: var(--border); color: var(--text); }
.dark .btn-today, .dark .btn-nav, .dark .btn-view { color: var(--text-secondary); }
.dark .btn-view.active { color: #fff; }
.dark .view-switcher { border-color: var(--border); }
.dark .current-date { color: var(--text); }

/* 月视图 */
.dark .week-day-header { color: var(--text-secondary); }
.dark .day-cell { border-color: #222244; }
.dark .day-cell:hover { background: #222244; }
.dark .day-number { color: #ccc !important; }
.dark .day-cell.other-month .day-number { color: #444 !important; }
.dark .day-cell.weekend .day-number { color: #e74c3c !important; }

/* 日视图、周视图 */
.dark .day-event { background: var(--surface); border-color: var(--border); }
.dark .day-event:hover { background: #222244; }
.dark .de-title { color: var(--text); }
.dark .hour-cell, .dark .hour-row { border-color: #222244; }
.dark .time-label { color: #666; }

/* 左侧面板 */
.dark .today-panel { color: var(--text); }
.dark .today-date { color: var(--text); }
.dark .today-month, .dark .today-year { color: var(--text-secondary); }
.dark .today-events, .dark .today-summary { border-color: var(--border); }
.dark .today-event-item .te-title { color: var(--text); }
.dark .events-label { color: var(--text-secondary); }

/* 弹窗 */
.dark .modal-overlay .detail-modal, .dark .modal-overlay .form-modal,
.dark .modal-overlay .voice-modal, .dark .recording-box { background: var(--surface); color: var(--text); }
.dark .form-input, .dark .form-textarea { background: #1a1a2e; border-color: var(--border); color: var(--text); }
.dark .detail-header h2, .dark .form-header h2 { color: var(--text); }
.dark .detail-footer, .dark .form-footer, .dark .modal-footer { border-color: var(--border); }
.dark .btn-cancel { background: #2a2a4a; color: var(--text); }
.dark .chat-window { background: var(--surface); }
.dark .chat-messages, .dark .chat-input-area { background: #1a1a2e; border-color: var(--border); }
.dark .msg-ai .msg-bubble { background: #2a2a4a; color: #ddd; }
.dark .chat-input { background: #2a2a4a; border-color: var(--border); color: var(--text); }
.dark .voice-panel { background: var(--surface); color: var(--text); }
.dark .text-input { background: #1a1a2e; border-color: var(--border); color: var(--text); }
.dark .event-chip.other { background: #2a2a4a; color: #aaa; }
.dark .event-chip.meeting { background: #1a2840; color: #93c5fd; }
.dark .event-chip.personal { background: #2a1a30; color: #f9a8d4; }
.dark .event-chip.work { background: #1a3028; color: #86efac; }
.dark .event-chip.reminder { background: #2a2818; color: #fcd34d; }

/* 强制覆盖 scoped 样式的硬编码颜色 */
.dark [style*="background:#fafafa"], .dark [style*="background: #fafafa"] { background: var(--surface) !important; }
.dark .app-header, .dark [class*="app-header"] { background: var(--surface) !important; border-color: var(--border) !important; }
.dark .app { background: var(--surface) !important; }
.dark .app-wrapper { background: transparent !important; }
.dark .week-header { border-color: var(--border) !important; }
.dark .hour-row { border-color: #222244 !important; }
.dark .time-label { color: #666 !important; }
.dark .day-cell { border-color: #1a1a30 !important; }
.dark .day-cell:hover { background: #222244 !important; }
.dark .form-group label { color: var(--text-secondary) !important; }
.dark .btn-today, .dark .btn-nav, .dark .btn-view { background: var(--surface) !important; }
.dark .quick-btn:disabled { background: #555 !important; }
.dark .today-panel { background: transparent !important; }
.dark .today-date, .dark .today-month, .dark .today-year { color: var(--text) !important; }
.dark .today-lunar { background: #2a1a20 !important; }
.dark .today-event-item { border-color: var(--border) !important; }
.dark input::placeholder, .dark textarea::placeholder { color: #666 !important; }
.dark .chat-input::placeholder { color: #666 !important; }
.dark .quick-input::placeholder { color: #666 !important; }
.dark .summary-text { color: var(--text-secondary) !important; }
.dark .btn-summary { background: var(--surface) !important; color: var(--text-secondary) !important; border-color: var(--border) !important; }
.dark .btn-summary.active { background: var(--primary) !important; color: #fff !important; }
.dark .day-header { border-color: #222244 !important; color: var(--text-secondary) !important; }
.dark .day-name { color: var(--text-secondary) !important; }
.dark .day-num { color: var(--text) !important; }
.dark .empty-day { color: var(--text-secondary) !important; }
.dark .chat-empty { color: #666 !important; }
.dark .chat-header { background: var(--primary) !important; }
</style>
