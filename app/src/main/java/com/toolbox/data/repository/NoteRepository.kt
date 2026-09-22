package com.toolbox.data.repository

import com.toolbox.data.local.dao.NoteDao
import com.toolbox.data.local.dao.NoteTagDao
import com.toolbox.data.local.entity.Note
import com.toolbox.data.local.entity.NoteTagCrossRef
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val noteTagDao: NoteTagDao
) {
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    fun getNotesByFolder(folderId: Long): Flow<List<Note>> = noteDao.getNotesByFolder(folderId)

    fun getNotesByTag(tagId: Long): Flow<List<Note>> = noteTagDao.getNotesByTagId(tagId)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)

    fun getNoteTags(noteId: Long): Flow<List<Long>> = noteTagDao.getNoteTagIds(noteId)

    suspend fun insertNoteTag(noteTagCrossRef: NoteTagCrossRef) = noteTagDao.insertNoteTag(noteTagCrossRef)

    suspend fun deleteNoteTags(noteId: Long) = noteTagDao.deleteNoteTags(noteId)
}
