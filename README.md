# 言历 (YanLi)

以语音交互为核心的智能日历。按住空格说话、打字输入，自动创建日程。

## 功能特性

- 按住==空格==键语音输入（Push-to-Talk），松开发送
- 页面顶部文本指令框，支持中文自然语言
- 通义千问 LLM 智能解析意图与时间（NNLP 规则兜底）
- AI 闲聊浮窗，支持多角色切换与自定义人设
- 本周 / 本月 AI 事件总结（一句话概括）
- 暗色模式，偏好本地持久化
- 月视图节日 / 假期标记 + 宣传标语
- 月/周/日三种日历视图，事件分类 + 颜色
- 左侧今日面板：公历 + 农历 + 当日事件
- 右下角快捷面板：一键添加今日事件、查看今日安排
- 用户注册登录，Session + Redis，数据按用户隔离
- 重复事件支持（每天 / 每周 / 每月）

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + Axios + Day.js + solarlunar |
| 后端 | Spring Boot 3.2 + Spring Data JPA + Spring Session |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis（Session + 聊天历史） |
| LLM | 通义千问 (qwen-plus) |
| 安全 | BCrypt + Session 拦截器 |
| 语音 | Web Speech API（Chrome/Edge 原生） |

## 快速启动

### 环境要求

- JDK 17+
- Node.js 18+
- Maven 3.8+
- MySQL 8.0+
- Redis

### 1. 启动 MySQL 和 Redis

MySQL 默认账号 root/123456（可在 `application.yml` 修改）。Redis 端口 6379。

### 2. 启动后端

```bash
cd yanli
mvn spring-boot:run
```

JPA 自动建表。API 地址: http://localhost:8080/api

### 3. 启动前端

```bash
cd yanli-web
npm install
npm run dev
```

访问 http://localhost:3000 → 自动跳转登录页 → 注册账号 → 进入日历。

### 4. 配置 LLM（可选）

在 `yanli/src/main/resources/application.yml` 中填入千问 API Key：

```yaml
voice:
  llm:
    qwen:
      api-key: sk-xxx
```

不配置也能用，会自动回退到 NLP 规则解析。

## 登录机制

Session + Cookie，Redis 存储（7 天有效期）。BCrypt 加密密码，SessionInterceptor 拦截未登录请求。

## 语音 & 指令

| 方式 | 操作 |
|---|---|
| 按住空格 | 任意位置按住说话，松开发送 |
| 麦克风按钮 | 右下角 ◉ 按钮，点击开始录音 |
| 文本输入 | 页面顶部输入框，打字回车 |

示例：`明天下午三点开会`、`下周三交报告`、`每天八点跑步`

LLM 理解后自动创建事件。如果 LLM 不可用，自动回退 NLP 规则引擎。

## 项目结构

```
yanli/
├── yanli/                                 # Spring Boot
│   ├── src/main/java/com/voicecalendar/
│   │   ├── config/                        # CORS / Session / 拦截器
│   │   ├── controller/
│   │   │   ├── AuthController.java        # 注册/登录/登出
│   │   │   ├── ChatController.java        # AI 闲聊 + 总结
│   │   │   ├── EventController.java       # 事件 CRUD
│   │   │   ├── HolidayController.java     # 节日数据
│   │   │   └── VoiceController.java       # 语音/文字指令
│   │   ├── service/
│   │   │   ├── ChatService.java           # 闲聊 + AI 总结
│   │   │   ├── EventService.java          # 事件业务
│   │   │   ├── HolidayService.java        # 节日 + 标语
│   │   │   ├── NLPService.java            # NLP 规则兜底
│   │   │   └── VoiceService.java          # LLM → NLP → 操作
│   │   ├── llm/
│   │   │   ├── LlmProvider.java           # LLM 接口
│   │   │   ├── LLMService.java            # 提示词 + 解析
│   │   │   └── QwenLlmProvider.java       # 千问实现
│   │   ├── asr/                           # 语音识别抽象
│   │   ├── model/                         # 实体 & DTO
│   │   └── repository/                    # JPA 仓库
│   └── src/main/resources/
├── yanli-web/                             # Vue 3
│   ├── src/
│   │   ├── components/
│   │   │   ├── HomePage.vue               # 日历主页
│   │   │   ├── LoginPage.vue              # 登录/注册
│   │   │   ├── CalendarView.vue           # 日历容器
│   │   │   ├── MonthView.vue              # 月视图
│   │   │   ├── WeekView.vue               # 周视图
│   │   │   ├── DayView.vue                # 日视图
│   │   │   ├── TodayPanel.vue             # 今日面板
│   │   │   ├── ChatPanel.vue              # AI 闲聊浮窗
│   │   │   ├── QuickPanel.vue             # 快捷操作
│   │   │   ├── VoiceButton.vue            # 语音按钮
│   │   │   ├── RecordingOverlay.vue       # 录音浮层
│   │   │   ├── VoiceModal.vue             # 语音结果
│   │   │   ├── EventDetail.vue            # 事件详情
│   │   │   └── EventForm.vue              # 事件表单
│   │   ├── composables/
│   │   │   ├── useVoice.js                # 语音交互
│   │   │   ├── usePushToTalk.js           # 按住空格
│   │   │   └── useCalendar.js             # 日历状态
│   │   ├── api/index.js                   # API + 401 拦截
│   │   └── router/index.js                # 路由 + 守卫
│   └── package.json
└── README.md
```

## API 接口

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录 |
| POST | /api/auth/logout | 登出 |
| GET | /api/auth/me | 当前用户 |

### 事件
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/events | 我的事件 |
| POST | /api/events | 创建事件 |
| PUT | /api/events/{id} | 更新事件 |
| DELETE | /api/events/{id} | 删除事件 |
| GET | /api/events/date/{date} | 按日期查询 |
| GET | /api/events/range | 按时间范围查询 |

### 语音
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/voice/text | 文本指令 |
| POST | /api/voice/audio | 音频上传 |
| GET | /api/voice/status | 服务状态 |

### 聊天 & 总结
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/chat/send | 发送消息 |
| POST | /api/chat/reset | 清空对话 |
| POST | /api/chat/summary | AI 周/月总结 |
| GET | /api/chat/roles | 角色列表 |

### 节日
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/holidays/{year}/{month} | 获取节日 + 标语 |
