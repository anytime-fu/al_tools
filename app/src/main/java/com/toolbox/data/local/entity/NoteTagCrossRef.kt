package com.toolbox.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"]
)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagId: Long
)
