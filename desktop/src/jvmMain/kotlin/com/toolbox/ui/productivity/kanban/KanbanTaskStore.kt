package com.toolbox.ui.productivity.kanban

import com.toolbox.data.repository.SettingsRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class KanbanTask(
    val id: String,
    val title: String,
    val note: String,
    val column: Int,
    val updatedAt: Long
)

object KanbanTaskStore {
    const val SETTINGS_KEY = "kanban_tasks"
    
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun load(settings: SettingsRepository): List<KanbanTask> {
        val saved = settings.getSettingValue(SETTINGS_KEY, "")
        if (saved.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<KanbanTask>>(saved) }.getOrDefault(emptyList())
    }
    
    suspend fun save(settings: SettingsRepository, tasks: List<KanbanTask>) {
        settings.saveSetting(SETTINGS_KEY, json.encodeToString(tasks))
    }
    
    suspend fun add(settings: SettingsRepository, title: String, note: String): KanbanTask {
        val task = KanbanTask(
            id = UUID.randomUUID().toString(),
            title = title,
            note = note,
            column = 0,
            updatedAt = System.currentTimeMillis()
        )
        save(settings, load(settings) + task)
        return task
    }
}