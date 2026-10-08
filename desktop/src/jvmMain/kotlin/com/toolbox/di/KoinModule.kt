package com.toolbox.di

import com.toolbox.ai.tools.ToolExecutor
import com.toolbox.data.local.DatabaseFactory
import com.toolbox.data.local.ToolboxDatabase
import com.toolbox.data.remote.AiApiService
import com.toolbox.data.remote.AiApiServiceImpl
import com.toolbox.data.remote.AiConfig
import com.toolbox.data.remote.AiConfigManager
import com.toolbox.data.repository.*
import com.toolbox.ui.note.NoteViewModel
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    // Database
    single<ToolboxDatabase> {
        DatabaseFactory.createDatabase()
    }
    
    // HTTP Client
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }
    }
    
    // AI Service
    single<AiApiService> {
        AiApiServiceImpl(get(), AiConfigManager.config.value)
    }
    
    // Repositories (using SQLDelight implementations)
    single<NoteRepository> {
        SqlDelightNoteRepository(get())
    }
    
    single<FolderRepository> {
        SqlDelightFolderRepository(get())
    }
    
    single<TagRepository> {
        SqlDelightTagRepository(get())
    }
    
    single<HabitRepository> {
        SqlDelightHabitRepository(get())
    }
    
    single<TransactionRepository> {
        SqlDelightTransactionRepository(get())
    }
    
    single<ScheduleRepository> {
        SqlDelightScheduleRepository(get())
    }
    
    single<PasswordRepository> {
        SqlDelightPasswordRepository(get())
    }
    
    single<ChatRepository> {
        SqlDelightChatRepository(get())
    }
    
    single<SettingsRepository> {
        SqlDelightSettingsRepository(get())
    }
    
    single<OcrHistoryRepository> {
        SqlDelightOcrHistoryRepository(get())
    }
    
    single<MeasurementRepository> {
        SqlDelightMeasurementRepository(get())
    }
    
    // AI Tool executor
    single {
        ToolExecutor(
            noteRepository = get(),
            transactionRepository = get(),
            habitRepository = get(),
            settingsRepository = get(),
            passwordRepository = get(),
            scheduleRepository = get()
        )
    }
    
    // ViewModels
    single {
        NoteViewModel()
    }
}

fun initKoin() {
    startKoin {
        modules(appModule)
    }
}
