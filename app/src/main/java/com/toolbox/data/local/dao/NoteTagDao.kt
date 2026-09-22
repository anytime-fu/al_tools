package com.toolbox.data.local.dao

import androidx.room.*
import com.toolbox.data.local.entity.NoteTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteTagDao {
    @Query("SELECT tagId FROM note_tag_cross_ref WHERE noteId = :noteId")
    fun getNoteTagIds(noteId: Long): Flow<List<Long>>

    @Query("SELECT n.* FROM notes n INNER JOIN note_tag_cross_ref nt ON n.id = nt.noteId WHERE nt.tagId = :tagId")
    fun getNotesByTagId(tagId: Long): Flow<List<com.toolbox.data.local.entity.Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTag(noteTagCrossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteNoteTags(noteId: Long)
}
