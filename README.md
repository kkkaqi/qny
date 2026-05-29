# 语音日历 (Voice Calendar)

以语音交互为核心的日历管理工具，支持按住空格键说话、文本指令输入和界面操作。

## 功能特性

- 按住空格键语音输入事件（Push-to-Talk），松开自动发送
- 页面顶部文本指令输入，支持中文自然语言
- 浏览器原生语音识别（Web Speech API，Chrome/Edge）
- 月视图 / 周视图 / 日视图三种日历布局
- 左侧今日面板：公历 + 农历 + 当日事件列表
- 事件分类（会议/个人/工作/提醒）+ 颜色标记
- 重复事件支持（每天/每周/每月）
- 用户注册登录，Session + Redis 认证，事件按用户隔离
- NLP 中文语音指令解析引擎

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + Axios + Day.js + solarlunar |
| 后端 | Spring Boot 3.2 + Spring Data JPA + Spring Session |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis（存储 Session） |
| 安全 | BCrypt 密码加密 + Session 拦截器 |
| 语音 | Web Speech API / 百度ASR / 阿里云NLS |

## 快速启动

### 环境要求

- JDK 17+
- Node.js 18+
- Maven 3.8+
- MySQL 8.0+
- Redis（Windows 可用 Docker: `docker run -d -p 6379:6379 redis`）

### 1. 启动 MySQL 和 Redis

确保两者都在运行。MySQL 默认账号 root/123456（可在 `application.yml` 修改）。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

首次启动 JPA 会自动建表。API 地址: http://localhost:8080/api

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:3000 → 自动跳转登录页 → 注册账号 → 进入日历。

## 登录机制

Session + Cookie 登录，Redis 存储 session（7 天有效期）。

流程：注册/登录 → 后端校验密码（BCrypt） → 写 session 到 Redis → `Set-Cookie` 下发浏览器 → 后续请求自动带 cookie → 后端 `SessionInterceptor` 校验 → 从 session 取 userId → 事件按用户隔离。

未登录访问任何 `/api/**`（除 `/api/auth/**`）返回 401，前端自动跳登录页。

## 语音功能

### 按住空格键说话（最快）

在日历页面任意位置按住空格键，弹窗出现并开始录音，松开发送。在输入框内按空格不会触发（避免冲突）。

### 文本指令输入

页面顶部中间有指令输入框，直接打字回车即可，和语音走同一套后端 NLP。示例：
- `添加明天下午三点在会议室开项目周会`
- `查看今天的日程`
- `删除会议`
- `明天有什么安排`

## 项目结构

```
voice-calendar/
├── backend/
│   ├── src/main/java/com/voicecalendar/
│   │   ├── VoiceCalendarApplication.java
│   │   ├── config/
│   │   │   ├── WebConfig.java           # CORS + 拦截器注册
│   │   │   ├── SessionConfig.java       # Redis Session 配置
│   │   │   └── SessionInterceptor.java  # 登录拦截器
│   │   ├── controller/
│   │   │   ├── AuthController.java      # 注册/登录/登出
│   │   │   ├── EventController.java     # 事件 CRUD
│   │   │   └── VoiceController.java     # 语音处理
│   │   ├── service/
│   │   │   ├── EventService.java        # 事件业务（按 userId 隔离）
│   │   │   ├── VoiceService.java        # ASR → NLP → 事件操作
│   │   │   └── NLPService.java          # 中文指令 NLP 解析
│   │   ├── model/                       # 实体 & DTO
│   │   ├── repository/                  # JPA 仓库
│   │   └── asr/                         # ASR 抽象层
│   └── src/main/resources/
│       ├── application.yml
│       └── schema.sql
├── frontend/
│   ├── src/
│   │   ├── App.vue                      # 路由容器
│   │   ├── components/
│   │   │   ├── HomePage.vue             # 日历主页
│   │   │   ├── LoginPage.vue            # 登录/注册
│   │   │   ├── CalendarView.vue         # 日历容器
│   │   │   ├── MonthView.vue            # 月视图
│   │   │   ├── WeekView.vue             # 周视图
│   │   │   ├── DayView.vue              # 日视图
│   │   │   ├── TodayPanel.vue           # 今日面板（农历+事件）
│   │   │   ├── VoiceButton.vue          # 语音按钮
│   │   │   ├── VoiceModal.vue           # 语音结果弹窗
│   │   │   ├── RecordingOverlay.vue     # 空格键录音浮层
│   │   │   ├── EventDetail.vue          # 事件详情
│   │   │   └── EventForm.vue            # 事件表单
│   │   ├── composables/
│   │   │   ├── useVoice.js              # 语音交互
│   │   │   ├── usePushToTalk.js         # 按住空格说话
│   │   │   └── useCalendar.js           # 日历状态
│   │   ├── api/index.js                 # API 封装 + 401 拦截
│   │   └── router/index.js              # 路由 + 登录守卫
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
| GET | /api/auth/me | 当前用户信息 |

### 事件管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/events | 我的所有事件 |
| POST | /api/events | 创建事件 |
| PUT | /api/events/{id} | 更新事件 |
| DELETE | /api/events/{id} | 删除事件 |
| GET | /api/events/date/{date} | 按日期查询 |
| GET | /api/events/range | 按时间范围查询 |
| GET | /api/events/search?keyword= | 搜索事件 |

### 语音处理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/voice/text | 文本指令处理 |
| POST | /api/voice/audio | 音频上传处理 |
| GET | /api/voice/status | 语音服务状态 |
