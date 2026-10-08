# AI 工具箱 - 桌面端扩展迭代计划

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档版本 | v1.1 |
| 创建日期 | 2024-01 |
| 更新日期 | 2026-09（新增第十章：实现差距勘误与完善/扩展建议） |
| 项目名称 | AI-Toolbox Desktop |
| 当前版本 | 1.0.0 |
| 目标平台 | Windows / macOS / Linux |

---

## 一、现状分析

### 当前功能模块（6个）

| 模块 | 功能 | 状态 |
|------|------|------|
| 笔记 | Markdown 编辑、标签分类、文件夹管理 | ✅ 已完成 |
| AI | AI 对话、工具调用 | ✅ 已完成 |
| 计算器 | 科学计算 | ✅ 已完成 |
| 密码 | 密码管理、生成 | ✅ 已完成 |
| 日程 | 日程管理、日历 | ✅ 已完成 |
| 设置 | AI 配置、主题设置 | ✅ 已完成 |

### 扩展目标

将桌面端功能从 **6 个模块** 扩展至 **15+ 个模块**，覆盖开发者工具、文件处理、数据处理、网络工具等高频使用场景。

---

## 二、功能规划总览

### 功能分类与数量

| 分类 | 功能数量 | 优先级 |
|------|----------|--------|
| 开发者工具 | 7 | P0 |
| 文件处理工具 | 5 | P1 |
| 文本处理工具 | 5 | P1 |
| 数据工具 | 4 | P1 |
| 网络工具 | 4 | P2 |
| 系统工具 | 4 | P2 |
| 图像工具 | 5 | P2 |
| 生产力工具 | 4 | P3 |
| 安全工具 | 3 | P3 |
| **合计** | **41** | - |

---

## 三、迭代计划

### 迭代 1：开发者工具套件（Sprint 1-2）

**目标**：打造高频开发者工具，快速提升桌面端价值

**预估工期**：2-3 周

| 序号 | 功能 | 描述 | 工作量 | 技术实现 |
|------|------|------|--------|----------|
| 1.1 | JSON 格式化 | JSON 美化、压缩、验证、树形展示 | 3天 | kotlinx-serialization |
| 1.2 | Base64 编解码 | 文本/文件 Base64 编解码 | 1天 | java.util.Base64 |
| 1.3 | URL 编解码 | URL 编码/解码，支持批量 | 1天 | java.net.URLEncoder |
| 1.4 | 时间戳工具 | 时间戳↔日期互转，支持多种格式 | 1天 | java.time |
| 1.5 | 哈希计算 | MD5/SHA1/SHA256，支持文件校验 | 2天 | java.security.MessageDigest |
| 1.6 | 正则测试 | 正则表达式实时匹配、高亮、捕获组 | 2天 | kotlin.text.Regex |
| 1.7 | UUID 生成 | 批量生成 UUID v4/v5，支持多种格式 | 1天 | java.util.UUID |
| 1.8 | 颜色工具 | 颜色选择器、HEX/RGB/HSL 互转 | 2天 | Compose Canvas |

**交付物**：
- 新增 `developer` 模块
- 侧边栏新增「开发者工具」入口
- 每个工具独立页面，支持快捷键切换

---

### 迭代 2：文本与文件处理（Sprint 3-4）

**目标**：提供强大的文本和文件处理能力

**预估工期**：2-3 周

| 序号 | 功能 | 描述 | 工作量 | 技术实现 |
|------|------|------|--------|----------|
| 2.1 | 文本 Diff 对比 | 双栏对比，高亮差异，支持合并 | 3天 | java-diff-utils |
| 2.2 | 字数统计 | 字符、单词、行数、段落统计 | 1天 | 纯 Kotlin 实现 |
| 2.3 | 文本转换 | 大小写、繁简、驼峰、下划线转换 | 2天 | 纯 Kotlin 实现 |
| 2.4 | 文本清理 | 去空行、去空格、去重复行 | 1天 | 纯 Kotlin 实现 |
| 2.5 | Markdown 预览 | 实时预览、导出 HTML/PDF | 3天 | commonmark-java |
| 2.6 | 文件搜索 | 按文件名、内容、正则搜索 | 3天 | java.nio.file |
| 2.7 | 批量重命名 | 规则化重命名，支持正则、序号 | 2天 | java.nio.file |
| 2.8 | 文件对比 | 二进制/文本文件 Diff 对比 | 2天 | java-diff-utils |

**交付物**：
- 新增 `text` 模块（文本处理）
- 新增 `file` 模块（文件处理）
- 支持拖拽文件到窗口处理

---

### 迭代 3：数据处理工具（Sprint 5-6）

**目标**：提供数据查看、转换、可视化能力

**预估工期**：2-3 周

| 序号 | 功能 | 描述 | 工作量 | 技术实现 |
|------|------|------|--------|----------|
| 3.1 | CSV 查看器 | 表格展示、筛选、排序、搜索 | 3天 | Apache Commons CSV |
| 3.2 | JSON↔CSV 转换 | JSON 与 CSV 互转 | 2天 | kotlinx-serialization |
| 3.3 | XML 格式化 | XML 美化、压缩、XPath 查询 | 2天 | javax.xml |
| 3.4 | YAML 格式化 | YAML 格式化、验证、转换 | 2天 | SnakeYAML |
| 3.5 | 数据可视化 | 柱状图、折线图、饼图生成 | 4天 | Compose Canvas |
| 3.6 | 数据库查看器 | SQLite 数据库浏览、SQL 查询 | 3天 | SQLDelight |
| 3.7 | 文件去重 | 查找重复文件，MD5 校验 | 2天 | java.security |

**交付物**：
- 新增 `data` 模块
- 支持拖拽文件导入
- 数据导出功能

---

### 迭代 4：网络与系统工具（Sprint 7-8）

**目标**：提供网络诊断和系统监控能力

**预估工期**：2-3 周

| 序号 | 功能 | 描述 | 工作量 | 技术实现 |
|------|------|------|--------|----------|
| 4.1 | HTTP 客户端 | API 测试、请求构建、响应查看 | 4天 | Ktor Client |
| 4.2 | 网络信息 | IP 地址、DNS 查询、Whois | 2天 | java.net |
| 4.3 | 端口扫描 | TCP 端口扫描、服务识别 | 2天 | java.net.Socket |
| 4.4 | Ping 工具 | ICMP Ping、Traceroute | 2天 | ProcessBuilder |
| 4.5 | 系统信息 | CPU、内存、磁盘、网络监控 | 3天 | oshi-core |
| 4.6 | 进程管理 | 进程列表、资源占用、终止进程 | 2天 | oshi-core |
| 4.7 | 剪贴板历史 | 记录剪贴板历史、快速粘贴 | 2天 | java.awt.Clipboard |
| 4.8 | 快捷启动 | 应用/文件/URL 快速启动 | 2天 | java.awt.Desktop |

**交付物**：
- 新增 `network` 模块
- 新增 `system` 模块
- 系统托盘常驻支持

---

### 迭代 5：图像处理工具（Sprint 9-10）

**目标**：提供图像查看、编辑、转换能力

**预估工期**：2-3 周

| 序号 | 功能 | 描述 | 工作量 | 技术实现 |
|------|------|------|--------|----------|
| 5.1 | 图片查看器 | 图片浏览、缩放、旋转 | 3天 | Compose Canvas |
| 5.2 | 图片压缩 | 批量压缩，质量/尺寸调整 | 2天 | javax.imageio |
| 5.3 | 格式转换 | PNG↔JPG↔WebP↔BMP 互转 | 2天 | javax.imageio |
| 5.4 | 图片裁剪 | 自由裁剪、固定比例裁剪 | 2天 | Compose Canvas |
| 5.5 | 水印工具 | 文字/图片水印，批量添加 | 2天 | Java2D |
| 5.6 | 屏幕截图 | 区域截图、窗口截图 | 3天 | java.awt.Robot |
| 5.7 | 取色器 | 屏幕取色、颜色代码复制 | 1天 | java.awt.Robot |

**交付物**：
- 新增 `image` 模块
- 支持拖拽图片处理
- 批量处理队列

---

### 迭代 6：生产力与安全工具（Sprint 11-12）

**目标**：提升日常工作效率和数据安全

**预估工期**：2-3 周

| 序号 | 功能 | 描述 | 工作量 | 技术实现 |
|------|------|------|--------|----------|
| 6.1 | 桌面便签 | 置顶便签、提醒、颜色分类 | 3天 | Compose Desktop |
| 6.2 | 番茄钟 | 专注计时、统计报告 | 2天 | Compose Desktop |
| 6.3 | 任务看板 | 看板式任务管理、拖拽排序 | 3天 | Compose Desktop |
| 6.4 | 快捷键管理 | 全局快捷键、自定义映射 | 2天 | JNativeHook |
| 6.5 | 多剪贴板 | 扩展剪贴板、分类管理 | 2天 | java.awt.Clipboard |
| 6.6 | 密码生成器 | 可配置规则、批量生成 | 1天 | java.security |
| 6.7 | 文件加密 | AES 加密/解密文件 | 2天 | javax.crypto |
| 6.8 | 密码强度检测 | 分析密码强度、给出建议 | 1天 | 纯 Kotlin 实现 |

**交付物**：
- 新增 `productivity` 模块
- 新增 `security` 模块
- 系统托盘快捷操作

---

## 四、技术架构设计

### 4.1 模块结构

```
desktop/src/jvmMain/kotlin/com/toolbox/
├── ui/
│   ├── developer/          # 开发者工具
│   │   ├── json/           # JSON 格式化
│   │   ├── base64/         # Base64 编解码
│   │   ├── timestamp/      # 时间戳工具
│   │   ├── hash/           # 哈希计算
│   │   ├── regex/          # 正则测试
│   │   ├── uuid/           # UUID 生成
│   │   └── color/          # 颜色工具
│   ├── text/               # 文本处理
│   │   ├── diff/           # 文本对比
│   │   ├── counter/        # 字数统计
│   │   ├── converter/      # 文本转换
│   │   ├── cleaner/        # 文本清理
│   │   └── markdown/       # Markdown 预览
│   ├── file/               # 文件处理
│   │   ├── search/         # 文件搜索
│   │   ├── rename/         # 批量重命名
│   │   ├── compare/        # 文件对比
│   │   └── dedup/          # 文件去重
│   ├── data/               # 数据工具
│   │   ├── csv/            # CSV 查看器
│   │   ├── converter/      # 格式转换
│   │   ├── chart/          # 数据可视化
│   │   └── database/       # 数据库查看器
│   ├── network/            # 网络工具
│   │   ├── http/           # HTTP 客户端
│   │   ├── info/           # 网络信息
│   │   ├── scanner/        # 端口扫描
│   │   └── ping/           # Ping 工具
│   ├── system/             # 系统工具
│   │   ├── info/           # 系统信息
│   │   ├── process/        # 进程管理
│   │   ├── clipboard/      # 剪贴板历史
│   │   └── launcher/       # 快捷启动
│   ├── image/              # 图像工具
│   │   ├── viewer/         # 图片查看器
│   │   ├── compress/       # 图片压缩
│   │   ├── converter/      # 格式转换
│   │   ├── editor/         # 图片编辑
│   │   └── screenshot/     # 屏幕截图
│   ├── productivity/       # 生产力工具
│   │   ├── sticky/         # 桌面便签
│   │   ├── pomodoro/       # 番茄钟
│   │   ├── kanban/         # 任务看板
│   │   └── clipboard/      # 多剪贴板
│   └── security/           # 安全工具
│       ├── generator/      # 密码生成
│       ├── encrypt/        # 文件加密
│       └── strength/       # 强度检测
└── util/                   # 工具类
    ├── file/               # 文件工具
    ├── network/            # 网络工具
    ├── system/             # 系统工具
    └── image/              # 图像工具
```

### 4.2 新增依赖

```kotlin
// build.gradle.kts 新增依赖

// Diff 差异对比
implementation("io.github.java-diff-utils:java-diff-utils:4.12")

// Markdown 解析
implementation("org.commonmark:commonmark:0.22.0")

// CSV 处理
implementation("org.apache.commons:commons-csv:1.10.0")

// YAML 处理
implementation("org.yaml:snakeyaml:2.2")

// 系统信息
implementation("com.github.oshi:oshi-core:6.4.8")

// 全局快捷键
implementation("com.github.kwhat:jnativehook:2.2.2")

// 图像处理
implementation("com.twelvemonkeys.imageio:imageio-core:3.10.1")
implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.10.1")
implementation("com.twelvemonkeys.imageio:imageio-webp:3.10.1")
```

### 4.3 UI 设计规范

#### 侧边栏布局

```
┌─────────────────────────────────────────────────────────┐
│  [Logo]  AI 工具箱                                       │
├─────────────────────────────────────────────────────────┤
│  📝 笔记                                                │
│  🤖 AI                                                  │
│  🧮 计算器                                               │
│  🔒 密码                                                │
│  📅 日程                                                │
│  ─────────────────────────────────────────────────────  │
│  🔧 开发者工具                                           │
│  📄 文本处理                                             │
│  📁 文件处理                                             │
│  📊 数据工具                                             │
│  🌐 网络工具                                             │
│  💻 系统工具                                             │
│  🖼️ 图像工具                                            │
│  ⚡ 生产力                                               │
│  🛡️ 安全工具                                            │
│  ─────────────────────────────────────────────────────  │
│  ⚙️ 设置                                                │
└─────────────────────────────────────────────────────────┘
```

#### 工具页面布局

```
┌─────────────────────────────────────────────────────────┐
│  [返回]  工具名称                    [快捷键] [帮助]        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────────┐  ┌─────────────────────┐       │
│  │                     │  │                     │       │
│  │      输入区域        │  │      输出区域         │       │
│  │                     │  │                     │       │
│  │                     │  │                     │       │
│  └─────────────────────┘  └─────────────────────┘       │
│                                                         │
│  [转换] [复制结果] [清空] [导出]                            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 五、开发规范

### 5.1 代码规范

- 使用 Kotlin 协程处理异步操作
- 遵循 MVVM 架构模式
- 使用 Koin 进行依赖注入
- 所有工具函数编写单元测试

### 5.2 UI 规范

- 遵循 Material 3 设计规范
- 支持亮色/暗色主题
- 响应式布局，最小宽度 800px
- 支持键盘快捷键操作

### 5.3 测试规范

- 单元测试覆盖率 > 80%
- 每个工具模块独立测试
- UI 自动化测试（关键路径）

---

## 六、里程碑与时间线

```
Sprint 1-2  (第1-3周)   ──→  迭代1：开发者工具套件
Sprint 3-4  (第4-6周)   ──→  迭代2：文本与文件处理
Sprint 5-6  (第7-9周)   ──→  迭代3：数据处理工具
Sprint 7-8  (第10-12周)  ──→  迭代4：网络与系统工具
Sprint 9-10 (第13-15周)  ──→  迭代5：图像处理工具
Sprint 11-12(第16-18周)  ──→  迭代6：生产力与安全工具
```

### 版本规划

| 版本 | 迭代 | 预计发布时间 | 新增功能数 |
|------|------|--------------|------------|
| v1.1 | 迭代1 | 第3周末 | 8 |
| v1.2 | 迭代2 | 第6周末 | 8 |
| v1.3 | 迭代3 | 第9周末 | 7 |
| v1.4 | 迭代4 | 第12周末 | 8 |
| v1.5 | 迭代5 | 第15周末 | 7 |
| v2.0 | 迭代6 | 第18周末 | 8 |

---

## 七、风险评估

| 风险项 | 影响 | 概率 | 应对措施 |
|--------|------|------|----------|
| 跨平台兼容性 | 高 | 中 | 优先实现 JVM 通用功能，平台特定功能降级处理 |
| 性能问题 | 中 | 中 | 大文件处理使用异步 + 流式处理 |
| 依赖冲突 | 低 | 低 | 使用 Gradle 版本目录统一管理 |
| UI 一致性 | 中 | 中 | 建立组件库，统一设计语言 |

---

## 八、验收标准

### 功能验收

- [ ] 所有工具功能正常运行
- [ ] 支持 Windows / macOS / Linux
- [ ] 支持亮色/暗色主题切换
- [ ] 支持键盘快捷键操作
- [ ] 支持拖拽文件操作

### 性能验收

- [ ] 应用启动时间 < 3秒
- [ ] 工具切换响应 < 500ms
- [ ] 大文件处理（100MB+）不卡顿
- [ ] 内存占用 < 512MB

### 质量验收

- [ ] 单元测试覆盖率 > 80%
- [ ] 无 P0/P1 级别 Bug
- [ ] 代码审查通过
- [ ] 文档完整

---

## 九、附录

### A. 快捷键规划

| 快捷键 | 功能 |
|--------|------|
| Ctrl+Shift+D | 打开开发者工具 |
| Ctrl+Shift+F | 打开文件处理 |
| Ctrl+Shift+T | 打开文本处理 |
| Ctrl+Shift+J | JSON 格式化 |
| Ctrl+Shift+B | Base64 编解码 |
| Ctrl+Shift+H | 哈希计算 |
| Ctrl+Shift+R | 正则测试 |
| Ctrl+Shift+S | 屏幕截图 |

### B. 参考资料

- [Jetpack Compose Desktop](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Material 3 Design](https://m3.material.io/)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)

---

## 十、v1.1 评审补充（2026-09）：实现差距勘误与完善/扩展建议

> 本节为对前九章计划的评审补充。经代码审查确认：**迭代 1-6 规划的 41 个工具中绝大部分已在代码中落地**，但存在收尾坑点、工程质量缺口与文档失同步问题。核心结论：**先补完、再扩展**。

### 10.1 现状勘误（文档与代码不同步）

| 文档说法 | 代码实际情况 | 需要动作 |
|----------|--------------|----------|
| 一、现状分析：仅 6 个模块已完成 | 桌面端已有 14 大模块、约 50 个子工具（84 个 Kotlin 文件），迭代 1-6 绝大部分功能已实现 | 更新本计划"现状分析"，后续按 10.5 新迭代推进 |
| DESKTOP_README：仅 5 个模块已实现 | 同上，严重滞后 | 重写 README 模块清单与运行说明 |
| DESKTOP_README：支持 `packageDeb` | `desktop/build.gradle.kts` 的 `targetFormats` 仅 Msi/Exe/Dmg，无 Linux 目标 | 补 Deb/Rpm 目标或修正文档 |
| DESKTOP_README：与 Android 端 100% 共享 Entity/接口 | `app` 模块未依赖 `:common`，entity/AI 服务/工具定义在两端各有一份代码 | 见 10.4 架构统一 |
| 五、测试规范 / 八、质量验收：覆盖率 > 80% | 全项目 0 个测试文件（jvmTest 已声明依赖但目录不存在） | 见 10.3 |

### 10.2 已实现功能收尾清单（P0：先修后扩）

| 序号 | 问题 | 位置 | 建议修复方案 | 工作量 |
|------|------|------|--------------|--------|
| 1 | Function Calling 结果回传三处 `Not implemented`，工具闭环断裂；桌面端也缺少 ToolExecutor（app 端有现成实现可移植），`AiChatScreen` 仅调用不带工具的流式接口 | `common/.../AiApiService.kt:617,627,637` | 实现 sendGeminiToolResults / sendOpenAiToolResults / sendClaudeToolResults；移植 app 端 `ai/tools/ToolExecutor.kt` 到 desktop 并注册 Koin、接入对话 | 3天 |
| 2 | `getNotesByTag` 返回空列表，标签筛选实际不可用 | `desktop/.../SqlDelightRepositories.kt:76` | 在 `Toolbox.sq` 增加 tag 过滤 JOIN 查询 | 0.5天 |
| 3 | OcrHistory / Measurement 仓储接口已定义、表已建，但桌面端无实现、未注册 Koin | `common/.../Repositories.kt` | 桌面端补 SQLDelight 实现并接线 | 1天 |
| 4 | Gemini/Claude 为假流式（整包返回后 30ms 逐词模拟打字机），仅 DeepSeek 走真 SSE | `AiApiService.kt` sendAiMessageStream | Gemini 用 `:streamGenerateContent?alt=sse`，Claude 用 `stream: true` SSE，复用现有逐行解析器 | 2天 |
| 5 | 密码条目明文存 SQLite（Android 端有 EncryptedSharedPreferences，桌面端无任何加密）；API Key 同样明文存 settings 表 | `desktop/.../SqlDelightRepositories.kt`（PasswordRepository） | 引入主密码（启动解锁）+ AES-GCM 加密 password_entries 与 api_key；最低限度先加密 api_key | 3天 |
| 6 | 设置页模型列表与 AiApiService 支持列表不一致（缺 deepseek-reasoner / claude-3-haiku；gemini-pro 等型号已过旧） | `desktop/.../SettingsScreen.kt:46` | 模型列表改为「预置 + 可编辑」，支持手填任意模型名（配合自定义 API URL 接任意网关） | 1天 |
| 7 | 遗留代码：InMemoryRepositories 未被 Koin 使用；AiViewModel 为不落库的旧实现但仍注册在 Koin | `desktop/.../InMemoryRepositories.kt`、`ui/ai/AiViewModel.kt` | 删除或标记 deprecated，避免误导后续开发 | 0.5天 |
| 8 | 数据库无迁移机制，仅 `CREATE TABLE IF NOT EXISTS`，后续加表/加列将无法升级用户旧数据 | `desktop/.../DatabaseFactory.kt` | 引入 SQLDelight `.sqm` 迁移文件 + `migrate()`；正式发布前必须就位 | 1天 |

### 10.3 工程质量完善（P0）

1. **版本控制（最高优先级）**：仓库仅有 1 个 commit（`d4c269f`），`desktop/`（84 文件）、`common/`（18 文件）及本计划、DESKTOP_README、theme.md 均处于 untracked 状态，存在丢失风险 → 立即分模块提交；确认 `.gitignore` 覆盖 `build/`、`.gradle/`、`local.properties`。
2. **测试从 0 到 1**：优先为纯函数工具补 `desktop/src/jvmTest`（Base64/URL/时间戳/哈希/UUID/文本转换/清理/密码强度/JSON↔CSV），无 UI 依赖、性价比最高；其次为 9 个 SQLDelight 仓储写内存 SQLite 测试。
3. **CI/CD**：新增 `.github/workflows`：windows/ubuntu/macos 矩阵执行 `desktop:test` + `desktop:packageDistributionForCurrentOS`，PR 强制跑测试。
4. **打包配置完善**：
   - `upgradeUuid = "a1b2c3d4-..."` 为占位符，正式发布前必须换成真实 UUID 并永久固定（否则用户升级会装出第二份应用）；
   - 无应用图标（desktop 无 resources 目录）→ 从 `logo_preview.html` 设计稿导出 .ico/.icns/.png 并配置 `iconFile`；
   - 补 Linux 目标（Deb/Rpm），与 README 声明对齐；
   - Windows 建议补 `dirChooser`、`shortcut` 等安装体验参数。
5. **依赖版本升级**：Kotlin 1.9.20 / Compose Multiplatform 1.5.12 已落后多个大版本，升级后可获得稳定的资源 API（i18n 前置条件）、性能修复与 K2 编译器收益。

### 10.4 架构级扩展（P1）

| 方向 | 现状问题 | 建议方案 |
|------|----------|----------|
| common 真共享 | app 不依赖 `:common`，两端 entity/AI 服务/工具定义是两份漂移的拷贝 | 给 common 增加 androidTarget，app 逐步改为消费 common；短期先共享纯模型与 ToolDefinitions（风险最低），长期统一 AiApiService |
| 跨端数据互通 | 桌面 SQLite（`~/.ai-toolbox/toolbox.db`）与 Android Room 私有库完全隔离，笔记/日程/密码无法迁移 | 见迭代 8（10.5） |
| 导航体验 | 侧边栏仅 2 项（总览/设置），14 个模块折叠在总览网格中，找工具成本随工具数线性上升 | ① Ctrl+K 命令面板（工具名直达 + 最近使用）；② 启动恢复上次打开的工具页；③ 侧边栏「常用工具」自定义置顶 |
| 设置项缺口 | 无数据目录配置/备份恢复、无 i18n、无托盘/自启动/自动更新 | 并入迭代 9（10.5） |

### 10.5 新增迭代 7-9 建议

#### 迭代 7：AI 能力深化（Sprint 13-14）

| 序号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| 7.1 | 真流式补全 | Gemini/Claude 原生 SSE 流式（含 10.2-4） | 2天 |
| 7.2 | Function Calling 落地 | 工具结果回传 + ToolExecutor 移植，AI 可实际创建笔记/记账/待办（含 10.2-1） | 3天 |
| 7.3 | AI 融入各工具 | 选中文本右键 AI 润色/翻译/解释；HTTP 响应体 AI 解读；正则 AI 生成；报错粘贴 AI 排查 | 4天 |
| 7.4 | 对话增强 | 会话导出 Markdown、会话内搜索、消息编辑重发、Token 用量与成本统计 | 3天 |
| 7.5 | 模型管理 | 模型列表可编辑、多 Provider 配置并存、连接测试按钮 | 2天 |

#### 迭代 8：跨端与数据资产（Sprint 15-16）

| 序号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| 8.1 | 全量导出/导入 | 笔记/日程/习惯/记账导出 ZIP（JSON），跨设备导入，含冲突策略 | 3天 |
| 8.2 | 密码库安全导出 | 主密码 AES-GCM 加密备份，导入需验密 | 2天 |
| 8.3 | 数据库迁移机制 | `.sqm` 迁移 + 迁移前自动备份回滚（含 10.2-8） | 1天 |
| 8.4 | 便携同步目录 | 可选「数据目录跟随同步盘」（OneDrive/坚果云），实现轻量跨端同步 | 2天 |

#### 迭代 9：体验与分发（Sprint 17-18）

| 序号 | 功能 | 描述 | 工作量 |
|------|------|------|--------|
| 9.1 | 命令面板 | Ctrl+K 全局工具搜索/最近使用（含 10.4-导航） | 3天 |
| 9.2 | i18n | 字符串资源化，中/英切换（需先完成 10.3-5 版本升级） | 4天 |
| 9.3 | 托盘与自启动 | 系统托盘常驻、最小化到托盘、开机自启动开关 | 2天 |
| 9.4 | 自动更新 | 基于 GitHub Releases 检查更新 + 下载安装引导 | 3天 |
| 9.5 | 诊断能力 | 崩溃日志（`~/.ai-toolbox/logs`）+ 设置页「导出诊断信息」 | 1天 |

### 10.6 增补后的版本规划

| 版本 | 内容 | 预计发布 |
|------|------|----------|
| v2.1 | 迭代 7 + 10.2/10.3 全部 P0 收尾 | 第 2 周末 |
| v2.2 | 迭代 8 跨端与数据 | 第 4 周末 |
| v3.0 | 迭代 9 体验与分发 + common 架构统一启动 | 第 6 周末 |

### 10.7 验收标准补充（追加至第八章）

- [ ] Function Calling 端到端可用（AI 创建的笔记出现在笔记列表）
- [ ] password_entries / api_key 不再明文落库
- [ ] desktop:test 在 CI 三平台全绿
- [ ] 安装包带正式图标与真实 upgradeUuid，Windows 升级安装不产生重复应用
- [ ] 旧版本数据库文件在新版本启动后自动迁移成功（含迁移测试用例）

---

**文档维护人**：CleonFu  
**最后更新**：2026-09（v1.1 评审补充）  
**下次评审**：P0 收尾清单（10.2 / 10.3）完成后