package com.toolbox.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.toolbox.data.local.AppDatabase
import com.toolbox.data.local.entity.*
import com.toolbox.di.EncryptedPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: List<Note> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val noteTagCrossRefs: List<NoteTagCrossRef> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitRecords: List<HabitRecord> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val ocrHistory: List<OcrHistory> = emptyList(),
    val chatSessions: List<ChatSession> = emptyList(),
    val chatMessages: List<ChatMessageEntity> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val passwordEntries: List<PasswordEntry> = emptyList(),
    val measurements: List<Measurement> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @EncryptedPrefs private val prefs: SharedPreferences,
    private val database: AppDatabase
) : ViewModel() {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _selectedModel = MutableStateFlow("gemini-pro")
    val selectedModel: StateFlow<String> = _selectedModel

    private val _customApiUrl = MutableStateFlow("")
    val customApiUrl: StateFlow<String> = _customApiUrl

    private val _darkMode = MutableStateFlow(0)
    val darkMode: StateFlow<Int> = _darkMode

    private val _cacheCleared = MutableStateFlow(false)
    val cacheCleared: StateFlow<Boolean> = _cacheCleared

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus

    private val _restoreStatus = MutableStateFlow<String?>(null)
    val restoreStatus: StateFlow<String?> = _restoreStatus

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _apiKey.value = prefs.getString("api_key", "") ?: ""
        _selectedModel.value = prefs.getString("selected_model", "gemini-pro") ?: "gemini-pro"
        _customApiUrl.value = prefs.getString("custom_api_url", "") ?: ""
        _darkMode.value = prefs.getInt("dark_mode", 0)
    }

    fun onApiKeyChange(key: String) {
        _apiKey.value = key
        prefs.edit().putString("api_key", key).apply()
    }

    fun onModelChange(model: String) {
        _selectedModel.value = model
        prefs.edit().putString("selected_model", model).apply()
    }

    fun onDarkModeChange(mode: Int) {
        _darkMode.value = mode
        prefs.edit().putInt("dark_mode", mode).apply()
    }

    fun clearApiKey() {
        _apiKey.value = ""
        prefs.edit().remove("api_key").apply()
    }

    fun onCustomApiUrlChange(url: String) {
        _customApiUrl.value = url
        prefs.edit().putString("custom_api_url", url).apply()
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val cacheDir = context.cacheDir
                    cacheDir.listFiles()?.forEach { file ->
                        if (file.isDirectory) {
                            file.deleteRecursively()
                        } else {
                            file.delete()
                        }
                    }
                }
                _cacheCleared.value = true
            } catch (e: Exception) {
                _cacheCleared.value = false
            }
        }
    }

    fun clearCacheCleared() {
        _cacheCleared.value = false
    }

    fun backupData(uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val backupData = BackupData(
                        notes = safeQuery { database.noteDao().getAllNotes().first() },
                        folders = safeQuery { database.folderDao().getAllFolders().first() },
                        tags = safeQuery { database.tagDao().getAllTags().first() },
                        noteTagCrossRefs = emptyList(),
                        habits = safeQuery { database.habitDao().getAllHabits().first() },
                        habitRecords = emptyList(),
                        categories = safeQuery { database.categoryDao().getAllCategories().first() },
                        transactions = safeQuery { database.transactionDao().getAllTransactions().first() },
                        ocrHistory = safeQuery { database.ocrHistoryDao().getAllHistory().first() },
                        chatSessions = safeQuery { database.chatDao().getAllSessions().first() },
                        chatMessages = emptyList(),
                        schedules = safeQuery { database.scheduleDao().getAllSchedules().first() },
                        passwordEntries = safeQuery { database.passwordDao().getAllPasswords().first() },
                        measurements = safeQuery { database.measurementDao().getAllMeasurements().first() }
                    )

                    val json = gson.toJson(backupData)

                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        ZipOutputStream(outputStream).use { zip ->
                            zip.putNextEntry(ZipEntry("backup.json"))
                            zip.write(json.toByteArray(Charsets.UTF_8))
                            zip.closeEntry()

                            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                            listOf("toolbox_prefs.xml", "toolbox_secure_prefs.xml").forEach { fileName ->
                                val file = File(prefsDir, fileName)
                                if (file.exists()) {
                                    zip.putNextEntry(ZipEntry("shared_prefs/$fileName"))
                                    file.inputStream().use { it.copyTo(zip) }
                                    zip.closeEntry()
                                }
                            }
                        }
                    }
                }
                _backupStatus.value = "备份成功"
            } catch (e: Exception) {
                _backupStatus.value = "备份失败: ${e.message}"
            }
        }
    }

    fun restoreData(uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    var json: String? = null

                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zip ->
                            var entry = zip.nextEntry
                            while (entry != null) {
                                when {
                                    entry.name == "backup.json" -> {
                                        json = zip.readBytes().toString(Charsets.UTF_8)
                                    }
                                    entry.name.startsWith("shared_prefs/") -> {
                                        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                                        prefsDir.mkdirs()
                                        val fileName = entry.name.removePrefix("shared_prefs/")
                                        FileOutputStream(File(prefsDir, fileName)).use { out ->
                                            zip.copyTo(out)
                                        }
                                    }
                                }
                                entry = zip.nextEntry
                            }
                        }
                    }

                    if (json == null) {
                        throw Exception("备份文件中没有找到数据")
                    }

                    val type = object : TypeToken<BackupData>() {}.type
                    val backupData: BackupData = gson.fromJson(json, type)

                    database.noteDao().let { dao ->
                        backupData.notes.forEach { dao.insertNote(it) }
                    }
                    database.folderDao().let { dao ->
                        backupData.folders.forEach { dao.insertFolder(it) }
                    }
                    database.tagDao().let { dao ->
                        backupData.tags.forEach { dao.insertTag(it) }
                    }
                    database.categoryDao().let { dao ->
                        backupData.categories.forEach { dao.insertCategory(it) }
                    }
                    database.transactionDao().let { dao ->
                        backupData.transactions.forEach { dao.insertTransaction(it) }
                    }
                    database.scheduleDao().let { dao ->
                        backupData.schedules.forEach { dao.insert(it) }
                    }
                    database.passwordDao().let { dao ->
                        backupData.passwordEntries.forEach { dao.insertPassword(it) }
                    }
                    database.measurementDao().let { dao ->
                        backupData.measurements.forEach { dao.insertMeasurement(it) }
                    }
                }
                _restoreStatus.value = "恢复成功，请重启应用"
            } catch (e: Exception) {
                _restoreStatus.value = "恢复失败: ${e.message}"
            }
        }
    }

    private suspend fun <T> safeQuery(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            @Suppress("UNCHECKED_CAST")
            emptyList<Any>() as T
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }

    fun clearRestoreStatus() {
        _restoreStatus.value = null
    }

    fun resetDatabase() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val dbFile = context.getDatabasePath("toolbox_database")
                    val walFile = File(dbFile.path + "-wal")
                    val shmFile = File(dbFile.path + "-shm")
                    val journalFile = File(dbFile.path + "-journal")

                    listOf(dbFile, walFile, shmFile, journalFile).forEach { file ->
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
                _restoreStatus.value = "数据库已重置，请重启应用"
            } catch (e: Exception) {
                _restoreStatus.value = "重置失败: ${e.message}"
            }
        }
    }
}
