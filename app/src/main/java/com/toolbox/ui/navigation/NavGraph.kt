package com.toolbox.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.toolbox.ui.ai.AiChatScreen
import com.toolbox.ui.ai.AiToolResultScreen
import com.toolbox.ui.ai.AiToolsScreen
import com.toolbox.ui.ai.CustomPromptManagerScreen
import com.toolbox.ui.armeasurement.ArMeasurementScreen
import com.toolbox.ui.bmi.BmiScreen
import com.toolbox.ui.calculator.CalculatorScreen
import com.toolbox.ui.document.DocumentScreen
import com.toolbox.ui.document.OcrResultScreen
import com.toolbox.ui.document.OcrScannerScreen
import com.toolbox.ui.home.HomeScreen
import com.toolbox.ui.idcard.IdCardScreen
import com.toolbox.ui.life.LifeAssistantScreen
import com.toolbox.ui.life.age.AgeCalculatorScreen
import com.toolbox.ui.life.compass.CompassScreen
import com.toolbox.ui.life.level.LevelScreen
import com.toolbox.ui.life.noise.NoiseScreen
import com.toolbox.ui.life.parking.ParkingScreen
import com.toolbox.ui.life.qrcode.QrCodeScreen
import com.toolbox.ui.life.timer.TimerScreen
import com.toolbox.ui.life.todo.TodoScreen
import com.toolbox.ui.life.translator.TranslatorScreen
import com.toolbox.ui.life.water.WaterReminderScreen
import com.toolbox.ui.note.NoteDetailScreen
import com.toolbox.ui.note.NoteEditScreen
import com.toolbox.ui.note.NoteScreen
import com.toolbox.ui.password.PasswordEditScreen
import com.toolbox.ui.password.PasswordScreen
import com.toolbox.ui.personal.PersonalScreen
import com.toolbox.ui.pdf.PdfScreen
import com.toolbox.ui.schedule.CalendarScreen
import com.toolbox.ui.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    var lastBackTime by remember { mutableStateOf(0L) }

    fun safePopBack(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastBackTime < 800) return false
        lastBackTime = now
        return navController.popBackStack()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // 首页
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        // 笔记列表
        composable(Screen.Note.route) {
            NoteScreen(
                onNavigateToDetail = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                },
                onNavigateToEdit = { noteId ->
                    navController.navigate(Screen.NoteEdit.createRoute(noteId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 笔记详情
        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            NoteDetailScreen(
                noteId = noteId,
                onNavigateToEdit = { navController.navigate(Screen.NoteEdit.createRoute(noteId)) },
                onBack = { navController.popBackStack() }
            )
        }

        // 笔记编辑
        composable(
            route = Screen.NoteEdit.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            NoteEditScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }

        // 文档处理
        composable(Screen.Document.route) {
            DocumentScreen(
                onNavigateToOcr = { navController.navigate(Screen.OcrScanner.route) },
                onNavigateToPdf = { navController.navigate(Screen.PdfReader.route) }
            )
        }

        // OCR扫描
        composable(Screen.OcrScanner.route) {
            OcrScannerScreen(
                onTextRecognized = { text ->
                    navController.navigate(Screen.OcrResult.createRoute(text))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // OCR结果
        composable(
            route = Screen.OcrResult.route,
            arguments = listOf(navArgument("text") { type = NavType.StringType })
        ) { backStackEntry ->
            val text = backStackEntry.arguments?.getString("text") ?: ""
            OcrResultScreen(
                text = text,
                onBack = { navController.popBackStack() },
                onNavigateToAiAnalysis = { ocrText ->
                    navController.navigate(Screen.AiToolResult.createRoute("OCR文本分析", "请分析以下OCR识别的文本内容：\n$ocrText"))
                }
            )
        }

        // AI工具
        composable(Screen.AiTools.route) {
            AiToolsScreen(
                onNavigateToChat = { navController.navigate(Screen.AiChat.route) },
                onNavigateToCustomPromptManager = { navController.navigate(Screen.CustomPromptManager.route) },
                onNavigateToAiToolResult = { title, prompt ->
                    navController.navigate(Screen.AiToolResult.createRoute(title, prompt))
                }
            )
        }

        // AI对话
        composable(Screen.AiChat.route) {
            AiChatScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // AI工具结果
        composable(
            route = Screen.AiToolResult.route,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("prompt") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val prompt = backStackEntry.arguments?.getString("prompt") ?: ""
            AiToolResultScreen(
                title = title,
                prompt = prompt,
                onBack = { navController.popBackStack() }
            )
        }

        // 自定义Prompt管理
        composable(Screen.CustomPromptManager.route) {
            CustomPromptManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 计算转换
        composable(Screen.Calculator.route) {
            CalculatorScreen()
        }

        // BMI计算
        composable(Screen.Bmi.route) {
            BmiScreen()
        }

        // 日程安排
        composable(Screen.Schedule.route) {
            CalendarScreen()
        }

        // 个人数据
        composable(Screen.Personal.route) {
            PersonalScreen()
        }

        // 身份证识别
        composable(Screen.IdCard.route) {
            IdCardScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // PDF阅读器
        composable(Screen.PdfReader.route) {
            PdfScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 密码列表
        composable(Screen.Password.route) {
            PasswordScreen(
                onNavigateToEdit = { passwordId ->
                    navController.navigate(Screen.PasswordEdit.createRoute(passwordId ?: -1L))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 密码编辑
        composable(
            route = Screen.PasswordEdit.route,
            arguments = listOf(navArgument("passwordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val passwordId = backStackEntry.arguments?.getLong("passwordId") ?: -1L
            PasswordEditScreen(
                passwordId = passwordId,
                onBack = { navController.popBackStack() }
            )
        }

        // AR测量
        composable(Screen.ArMeasurement.route) {
            ArMeasurementScreen(
                onBack = { safePopBack() }
            )
        }

        // 生活小助手
        composable(Screen.LifeAssistant.route) {
            LifeAssistantScreen(
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }

        // 二维码工具
        composable(Screen.LifeQrCode.route) {
            QrCodeScreen(onBack = { navController.popBackStack() })
        }

        // 待办清单
        composable(Screen.LifeTodo.route) {
            TodoScreen(onBack = { navController.popBackStack() })
        }

        // 水平仪
        composable(Screen.LifeLevel.route) {
            LevelScreen(onBack = { navController.popBackStack() })
        }

        // 指南针
        composable(Screen.LifeCompass.route) {
            CompassScreen(onBack = { navController.popBackStack() })
        }

        // 番茄钟
        composable(Screen.LifeTimer.route) {
            TimerScreen(onBack = { navController.popBackStack() })
        }

        // 喝水提醒
        composable(Screen.LifeWater.route) {
            WaterReminderScreen(onBack = { navController.popBackStack() })
        }

        // 车牌记忆
        composable(Screen.LifeParking.route) {
            ParkingScreen(onBack = { navController.popBackStack() })
        }

        // 年龄计算
        composable(Screen.LifeAge.route) {
            AgeCalculatorScreen(onBack = { navController.popBackStack() })
        }

        // 噪音检测
        composable(Screen.LifeNoise.route) {
            NoiseScreen(onBack = { navController.popBackStack() })
        }

        // 翻译
        composable(Screen.LifeTranslator.route) {
            TranslatorScreen(onBack = { navController.popBackStack() })
        }

        // 设置
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
