package com.toolbox.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Note : Screen("note", "笔记", Icons.Default.Note)
    object Document : Screen("document", "文档", Icons.Default.Description)
    object AiTools : Screen("ai_tools", "AI工具", Icons.Default.SmartToy)
    object Calculator : Screen("calculator", "计算", Icons.Default.Calculate)
    object Personal : Screen("personal", "个人", Icons.Default.Person)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
    object Bmi : Screen("bmi", "BMI", Icons.Default.MonitorWeight)
    object Schedule : Screen("schedule", "日程", Icons.Default.CalendarMonth)
    object PdfReader : Screen("pdf_reader", "PDF阅读", Icons.Default.PictureAsPdf)

    object NoteDetail : Screen("note_detail/{noteId}", "笔记详情", Icons.Default.Note) {
        fun createRoute(noteId: Long = -1L): String = "note_detail/$noteId"
    }

    object NoteEdit : Screen("note_edit/{noteId}", "编辑笔记", Icons.Default.Edit) {
        fun createRoute(noteId: Long = -1L): String = "note_edit/$noteId"
    }

    object AiChat : Screen("ai_chat", "AI对话", Icons.Default.Chat)
    object OcrScanner : Screen("ocr_scanner", "OCR扫描", Icons.Default.CameraAlt)
    object OcrResult : Screen("ocr_result/{text}", "识别结果", Icons.Default.TextSnippet) {
        fun createRoute(text: String): String = "ocr_result/${Uri.encode(text)}"
    }
    object IdCard : Screen("id_card", "身份证识别", Icons.Default.CreditCard)
    object CustomPromptManager : Screen("custom_prompt_manager", "管理Prompt", Icons.Default.Tune)
    object AiToolResult : Screen("ai_tool_result/{title}/{prompt}", "AI工具结果", Icons.Default.Build) {
        fun createRoute(title: String, prompt: String): String =
            "ai_tool_result/${Uri.encode(title)}/${Uri.encode(prompt)}"
    }

    object Password : Screen("password", "密码管理", Icons.Default.Lock)
    object PasswordEdit : Screen("password_edit/{passwordId}", "编辑密码", Icons.Default.Edit) {
        fun createRoute(passwordId: Long = -1L): String = "password_edit/$passwordId"
    }

    object ArMeasurement : Screen("ar_measurement", "AR测量", Icons.Default.Straighten)

    object LifeAssistant : Screen("life_assistant", "生活小助手", Icons.Default.Favorite)
    object LifeQrCode : Screen("life_qrcode", "二维码", Icons.Default.QrCode)
    object LifeTodo : Screen("life_todo", "待办清单", Icons.Default.CheckCircle)
    object LifeLevel : Screen("life_level", "水平仪", Icons.Default.Straighten)
    object LifeCompass : Screen("life_compass", "指南针", Icons.Default.Explore)
    object LifeTimer : Screen("life_timer", "番茄钟", Icons.Default.Timer)
    object LifeWater : Screen("life_water", "喝水提醒", Icons.Default.WaterDrop)
    object LifeParking : Screen("life_parking", "车牌记忆", Icons.Default.LocalParking)
    object LifeAge : Screen("life_age", "年龄计算", Icons.Default.Cake)
    object LifeNoise : Screen("life_noise", "噪音检测", Icons.Default.Mic)
    object LifeTranslator : Screen("life_translator", "翻译", Icons.Default.Translate)
}
