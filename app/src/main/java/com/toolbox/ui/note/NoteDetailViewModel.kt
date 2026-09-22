package com.toolbox.ui.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.data.local.entity.Note
import com.toolbox.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: -1L

    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note

    init {
        if (noteId != -1L) {
            loadNote()
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            _note.value = noteRepository.getNoteById(noteId)
        }
    }
}
