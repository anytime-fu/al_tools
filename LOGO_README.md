# AI工具箱 Logo 使用说明

## 文件列表

### 启动器图标（Android App Icon）
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` - 自适应图标配置
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` - 圆形自适应图标
- `app/src/main/res/drawable/ic_launcher_foreground.xml` - 前景矢量图

### 完整Logo
- `app/src/main/res/drawable/ic_logo_full.xml` - 完整logo矢量图（200x200dp）
- `app/src/main/res/drawable/ic_logo_simple.xml` - 简化版logo（48x48dp）
- `app/src/main/res/drawable/ic_logo_with_text.xml` - 带文字logo（300x100dp）
- `app/src/main/res/drawable/splash_screen.xml` - 启动画面背景

### SVG格式（可用于其他场景）
- `logo.svg` - 矢量格式logo

## 设计说明

### 配色方案
- 主色：`#2196F3` (蓝色)
- 辅色：`#03DAC5` (青绿色)
- 背景：白色

### 设计元素
1. **工具箱**：代表多功能工具集合
2. **AI大脑**：中央圆形代表人工智能核心
3. **神经节点**：周围的节点代表AI的连接能力
4. **连接线**：代表数据流动和智能处理

## 使用场景

### 1. Android应用图标
已配置为自适应图标，支持Android 8.0+设备。对于旧设备，使用PNG版本。

### 2. 启动画面
使用`splash_screen.xml`作为启动画面背景，中心显示完整logo。

### 3. 关于页面
在关于页面使用`ic_logo_full.xml`显示应用logo。

### 4. 网站/文档
使用`logo.svg`用于网站、文档或其他需要矢量格式的场景。

## 自定义

如需修改颜色，编辑以下文件：
- `app/src/main/res/values/colors.xml` - 主题颜色
- `app/src/main/res/drawable/ic_launcher_foreground.xml` - 启动器图标颜色
- `logo.svg` - SVG格式颜色

## 生成PNG

如需生成PNG格式的logo，可以：
1. 使用在线SVG转PNG工具
2. 在Android Studio中右键点击SVG文件 → Create Android Vector Asset
3. 使用命令行工具如`inkscape`或`rsvg-convert`

示例命令（使用rsvg-convert）：
```bash
rsvg-convert -w 512 -h 512 logo.svg -o logo.png
```
