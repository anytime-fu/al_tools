# AI 工具箱 (AI Toolbox)

一款**隐私优先、离线可用**的 Android 多功能工具箱应用，整合 AI 能力与本地工具，无需账号、无广告、即装即用。

## 界面展示
<img width="1156" height="2510" alt="cc5a52b63dc88ee4c1f7d82fc1b576dc" src="https://github.com/user-attachments/assets/c04e858f-36b9-4c1e-807a-bcc76df0df6c" />
<img width="1156" height="2510" alt="ec759e465c761bf82137268cdef151d1" src="https://github.com/user-attachments/assets/90e9fd63-7d3d-493f-86f5-9eb90e295b0b" />
<img width="1156" height="2510" alt="0d053aa8e0810ec1d7a3577569976a1b" src="https://github.com/user-attachments/assets/2f8ba031-b746-4856-b603-bd52476aa1f5" />
<img width="1156" height="2510" alt="d285d0145f11e8cec205ff532c96d056" src="https://github.com/user-attachments/assets/e0b9dfa5-6180-4f83-85e2-a8d16ac0f3a9" />


## ✨ 特性

- 🔒 **隐私安全** - 所有数据本地存储，AI 调用不留存
- 📴 **离线可用** - 核心功能无需联网
- 🚫 **轻量纯净** - 无广告、无账号、无推送
- 🤖 **AI 增强** - 每个模块可调用大模型辅助

## 📱 功能模块

### 📝 智能笔记
- Markdown 编辑器，实时预览
- AI 辅助：续写、润色、总结、翻译
- 标签分类、文件夹管理
- 导出：MD / TXT / PDF

### 📄 文档处理
- OCR 拍照识别（支持中英文）
- PDF 阅读器
- 识别结果编辑/导出

### 🤖 AI 工具箱
- 文本摘要
- 全文翻译（支持多语言）
- 代码解释 / 代码生成
- 自定义 Prompt
- AI 对话（支持 Gemini / DeepSeek / Claude）

### 🧮 计算转换
- 科学计算器
- BMI 计算
- 单位换算

### 📊 个人数据
- 习惯打卡
- 简单记账
- 日程管理
- 密码管理

### 🛠️ 实用工具
- AR 测量（支持参照物测量）
- 身份证识别
- 二维码生成
- 指南针 / 水平仪
- 番茄钟 / 喝水提醒
- 噪音检测 / 翻译

## 🛠️ 技术栈

| 技术 | 说明 |
|------|------|
| Kotlin | 开发语言 |
| Jetpack Compose | 声明式 UI |
| Material 3 | 设计规范 |
| Hilt | 依赖注入 |
| Room | SQLite ORM |
| Retrofit + OkHttp | 网络请求 |
| ML Kit | OCR 离线识别 |
| Markwon | Markdown 渲染 |
| CameraX | 相机功能 |
| SceneView + ARCore | AR 测量 |

## 📦 项目结构

```
app/src/main/java/com/toolbox/
├── ai/tools/           # AI 工具定义（Function Calling）
├── data/
│   ├── local/          # 数据库（Room）
│   │   ├── dao/        # 数据访问对象
│   │   └── entity/     # 实体类
│   ├── remote/         # 网络服务
│   └── repository/     # 数据仓库
├── di/                 # 依赖注入
├── ui/
│   ├── home/           # 首页
│   ├── note/           # 笔记模块
│   ├── document/       # 文档处理
│   ├── ai/             # AI 工具
│   ├── calculator/     # 计算器
│   ├── armeasurement/  # AR 测量
│   ├── password/       # 密码管理
│   ├── schedule/       # 日程管理
│   ├── settings/       # 设置
│   └── components/     # 通用组件
└── util/               # 工具类
```

## 🚀 构建与运行

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- 最低支持 Android 8.0 (API 26)

### 构建步骤

1. 克隆项目
```bash
git clone <repository-url>
cd common_tool
```

2. 用 Android Studio 打开项目

3. 同步 Gradle 依赖

4. 运行项目
```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

### APK 输出路径

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## 📋 权限说明

| 权限 | 用途 |
|------|------|
| INTERNET | AI API 调用 |
| CAMERA | OCR 拍照 / AR 测量 |
| READ_EXTERNAL_STORAGE | 读取文件 |
| WRITE_EXTERNAL_STORAGE | 保存文件 |
| RECORD_AUDIO | 语音输入 |

## 🤖 AI 配置

1. 打开应用，进入 **设置**
2. 选择 **AI 模型配置**
3. 输入 API Key（支持 Gemini / DeepSeek / Claude）
4. 选择模型，开始使用

## 📄 许可证

本项目仅供学习交流使用。

## 📧 联系方式

如有问题或建议，请提交 Issue。
