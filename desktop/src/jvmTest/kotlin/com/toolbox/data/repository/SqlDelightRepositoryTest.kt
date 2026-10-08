package com.toolbox.data.repository

import com.toolbox.data.local.createInMemoryDatabase
import com.toolbox.data.local.entity.Measurement
import com.toolbox.data.local.entity.Note
import com.toolbox.data.local.entity.OcrHistory
import com.toolbox.data.local.entity.PasswordEntry
import com.toolbox.data.local.entity.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SqlDelightRepositoryTest {

    @Test
    fun noteCrudAndSearch() = runBlocking {
        val db = createInMemoryDatabase()
        val repo = SqlDelightNoteRepository(db)
        val id = repo.insertNote(Note(title = "测试笔记", content = "内容甲"))
        assertTrue(id > 0)
        assertEquals("测试笔记", repo.getNoteById(id)?.title)
        repo.updateNote(repo.getNoteById(id)!!.copy(title = "改标题", updatedAt = 999L))
        assertEquals("改标题", repo.getNoteById(id)?.title)
        assertEquals(1, repo.searchNotes("改").first().size)
        repo.deleteNoteById(id)
        assertEquals(0, repo.getAllNotes().first().size)
    }

    @Test
    fun getNotesByTagFiltersByTag() = runBlocking {
        val db = createInMemoryDatabase()
        val noteRepo = SqlDelightNoteRepository(db)
        val tagRepo = SqlDelightTagRepository(db)
        
        val noteA = noteRepo.insertNote(Note(title = "A"))
        noteRepo.insertNote(Note(title = "B"))
        val tagId = tagRepo.insertTag(Tag(name = "工作"))
        db.toolboxQueries.insertNoteTag(noteA, tagId)
        
        val byTag = noteRepo.getNotesByTag(tagId).first()
        assertEquals(1, byTag.size)
        assertEquals("A", byTag.first().title)
        assertEquals(0, noteRepo.getNotesByTag(999L).first().size)
    }

    @Test
    fun passwordEntryIsEncryptedAtRest() = runBlocking {
        val db = createInMemoryDatabase()
        val repo = SqlDelightPasswordRepository(db)
        val id = repo.insertPasswordEntry(
            PasswordEntry(appName = "App", account = "user", password = "pw-123")
        )
        val raw = db.toolboxQueries.getPasswordEntryById(id).executeAsOne()
        assertNotEquals("pw-123", raw.password)
        assertEquals("pw-123", repo.getPasswordEntryById(id)?.password)
    }

    @Test
    fun settingsRoundTrip() = runBlocking {
        val db = createInMemoryDatabase()
        val repo = SqlDelightSettingsRepository(db)
        repo.saveSetting("k", "v")
        assertEquals("v", repo.getSettingValue("k", ""))
        repo.saveSetting("k", "v2")
        assertEquals("v2", repo.getSettingValue("k", ""))
        repo.deleteSetting("k")
        assertEquals("fallback", repo.getSettingValue("k", "fallback"))
    }

    @Test
    fun ocrAndMeasurementReposWork() = runBlocking {
        val db = createInMemoryDatabase()
        val ocr = SqlDelightOcrHistoryRepository(db)
        val id = ocr.insertOcrHistory(OcrHistory(recognizedText = "文字"))
        assertEquals("文字", ocr.getOcrHistoryById(id)?.recognizedText)
        
        val m = SqlDelightMeasurementRepository(db)
        val mid = m.insertMeasurement(Measurement(name = "身高", value = 170.5, unit = "cm", type = "body"))
        assertEquals(170.5, m.getMeasurementById(mid)?.value)
        assertEquals(1, m.getMeasurementsByType("body").first().size)
    }
}