# AI 工具箱 - PC端

基于 Kotlin Multiplatform 和 Compose Multiplatform 开发的桌面版AI工具箱应用。

## 技术栈

| 技术 | 说明 |
|------|------|
| Kotlin Multiplatform | 跨平台开发框架 |
| Compose Multiplatform | 声明式UI框架 |
| SQLDelight | 跨平台数据库 |
| Ktor | 跨平台网络请求 |
| Koin | 依赖注入 |

## 项目结构

```
common/                    # 共享代码模块
├── src/commonMain/
│   ├── kotlin/com/toolbox/
│   │   ├── data/         # 数据层（Entity, Repository接口）
│   │   ├── ai/           # AI工具定义
│   │   └── util/         # 工具类
│   └── sqldelight/       # 数据库Schema

desktop/                   # PC端模块
├── src/jvmMain/
│   ├── kotlin/com/toolbox/
│   │   ├── ui/           # Compose Desktop UI
│   │   ├── di/           # Koin依赖注入
│   │   └── data/         # 平台特定实现
│   └── resources/        # 资源文件

app/                       # 原有Android模块（保留）
```

## 环境要求

- JDK 17 或更高版本
- Android Studio Hedgehog (2023.1.1) 或更高版本（可选，用于Android端）
- Kotlin 1.9.20

## 构建与运行

### 1. 克隆项目

```bash
git clone <repository-url>
cd common_tool
```

### 2. 运行Desktop应用

```bash
# Windows
./gradlew :desktop:run

# 或者构建发行版
./gradlew :desktop:packageDmg        # macOS
./gradlew :desktop:packageMsi        # Windows
./gradlew :desktop:packageDeb        # Linux
```

### 3. 构建Android应用

```bash
./gradlew :app:assembleDebug
```

## 功能模块

### 已实现
- ✅ 智能笔记 - Markdown编辑器，实时预览
- ✅ AI对话 - 支持Gemini/DeepSeek/Claude
- ✅ 计算器 - 科学计算器，单位换算
- ✅ 密码管理 - 密码存储和生成
- ✅ 日程管理 - 日程添加和提醒

### 待实现
- 🔄 文档处理 - OCR识别
- 🔄 习惯打卡
- 🔄 记账功能
- 🔄 个人数据统计

## 配置AI

1. 打开应用，进入 **设置**
2. 输入API Key（支持 Gemini / DeepSeek / Claude）
3. 选择模型
4. 开始使用

## 数据存储

PC端数据存储在用户目录下：
- Windows: `%USERPROFILE%\.ai-toolbox\toolbox.db`
- macOS: `~/.ai-toolbox/toolbox.db`
- Linux: `~/.ai-toolbox/toolbox.db`

## 开发说明

### 添加新功能

1. 在 `common` 模块定义数据模型和Repository接口
2. 在 `desktop` 模块实现Repository
3. 创建Compose UI
4. 在Koin模块中注册依赖

### 代码共享策略

- **100%共享**: Entity类、Repository接口、AI工具定义
- **90%共享**: ViewModel（需要小的平台适配）
- **平台特定**: UI实现、数据库驱动、网络客户端

## 许可证

本项目仅供学习交流使用。
