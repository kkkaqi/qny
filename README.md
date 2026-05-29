# 语音日历 (Voice Calendar)

以语音交互为核心的日历管理工具，支持通过语音和界面混合操作的方式管理日程。

## 功能特性

- 语音添加/删除/查看事件，支持中文自然语言
- 月视图 / 周视图 / 日视图三种日历布局
- 事件分类（会议/个人/工作/提醒）
- 重复事件支持（每天/每周/每月）
- 浏览器原生语音识别（Web Speech API）
- 后端 ASR 服务支持（百度/阿里云语音识别）
- NLP 中文语音指令解析引擎

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + Axios + Day.js |
| 后端 | Spring Boot 3.2 + Spring Data JPA |
| 数据库 | MySQL 8.0+ |
| 语音识别 | Web Speech API / 百度ASR / 阿里云NLS |

## 快速启动

### 环境要求

- JDK 17+
- Node.js 18+
- Maven 3.8+
- MySQL 8.0+

### 1. 准备数据库

确保 MySQL 已启动，然后执行初始化脚本（或让 JPA 自动建表）：

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

默认连接配置（可在 `application.yml` 中修改）：
- 地址: localhost:3306
- 数据库: voice_calendar
- 用户名: root
- 密码: root

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动后 API 地址: http://localhost:8080/api

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问: http://localhost:3000

## 语音功能使用

### 方式一：浏览器原生语音（推荐，无需配置）

直接点击右下角麦克风按钮，浏览器会调用系统语音识别（需要 Chrome/Edge 浏览器）。

支持的语音指令示例：
- 「添加明天下午三点在会议室开项目周会」
- 「查看今天的日程」
- 「删除会议」
- 「添加每天上午九点的站会」
- 「明天有什么安排」
- 「添加5月20日下午两点去看牙医」

### 方式二：后端云 ASR（需要配置密钥）

在 `backend/src/main/resources/application.yml` 中配置百度/阿里云的 ASR 密钥：

```yaml
voice:
  asr:
    provider: baidu   # 改为 baidu 启用
  baidu:
    app-id: your-app-id
    api-key: your-api-key
    secret-key: your-secret-key
```

## 项目结构

```
voice-calendar/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/voicecalendar/
│   │   ├── VoiceCalendarApplication.java
│   │   ├── config/WebConfig.java     # CORS 配置
│   │   ├── controller/
│   │   │   ├── EventController.java  # 事件 CRUD API
│   │   │   └── VoiceController.java  # 语音处理 API
│   │   ├── service/
│   │   │   ├── EventService.java     # 事件业务逻辑
│   │   │   ├── VoiceService.java     # 语音处理编排
│   │   │   └── NLPService.java       # 中文指令 NLP 解析
│   │   ├── model/                    # 实体 & DTO
│   │   ├── repository/               # JPA 仓库
│   │   └── asr/                      # ASR 抽象接口
│   │       ├── AsrProvider.java
│   │       ├── BaiduAsrProvider.java
│   │       └── MockAsrProvider.java
│   └── src/main/resources/
│       ├── application.yml
│       └── schema.sql             # MySQL 建表脚本
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── App.vue                   # 主布局
│   │   ├── components/
│   │   │   ├── CalendarView.vue      # 日历容器
│   │   │   ├── MonthView.vue         # 月视图
│   │   │   ├── WeekView.vue          # 周视图
│   │   │   ├── DayView.vue           # 日视图
│   │   │   ├── VoiceButton.vue       # 语音按钮
│   │   │   ├── VoiceModal.vue        # 语音结果弹窗
│   │   │   ├── EventDetail.vue       # 事件详情
│   │   │   └── EventForm.vue         # 事件表单
│   │   ├── composables/
│   │   │   ├── useVoice.js           # 语音交互逻辑
│   │   │   └── useCalendar.js        # 日历状态管理
│   │   ├── api/index.js              # API 封装
│   │   └── router/index.js           # 路由配置
│   ├── package.json
│   └── vite.config.js
└── README.md
```

## API 接口

### 事件管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/events | 获取所有事件 |
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

## NLP 指令解析能力

系统内置的中文 NLP 解析器支持：

- **时间解析**: 明天/后天/下周/X月X日、上午/下午/晚上、X点/X点半、"三点到四点"
- **意图识别**: 添加/新建/安排 → ADD，删除/取消 → DELETE，查看/有什么 → QUERY
- **地点提取**: "在/于 XXX" → location
- **分类推断**: 会议/开会 → meeting，生日 → personal，任务 → work
- **重复规则**: 每天/每周/每月/每周几 → recurringRule
