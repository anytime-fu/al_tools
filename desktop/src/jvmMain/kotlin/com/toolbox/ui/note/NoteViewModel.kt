package com.toolbox.ui.note

import com.toolbox.data.local.entity.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NoteViewModel {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    init {
        // Load sample data
        _notes.value = listOf(
            Note(id = 1, title = "示例笔记1", content = "这是第一个示例笔记的内容"),
            Note(id = 2, title = "示例笔记2", content = "这是第二个示例笔记的内容"),
            Note(id = 3, title = "学习笔记", content = "Kotlin Multiplatform 学习记录")
        )
    }
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun addNote(title: String, content: String) {
        val newNote = Note(
            id = (_notes.value.maxOfOrNull { it.id } ?: 0) + 1,
            title = title,
            content = content
        )
        _notes.value = _notes.value + newNote
    }
    
    fun updateNote(note: Note) {
        _notes.value = _notes.value.map { 
            if (it.id == note.id) note else it 
        }
    }
    
    fun deleteNote(note: Note) {
        _notes.value = _notes.value.filter { it.id != note.id }
    }
}
