package com.toolbox.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.data.local.entity.Note
import com.toolbox.data.local.entity.Tag
import com.toolbox.data.local.entity.Folder
import com.toolbox.data.repository.NoteRepository
import com.toolbox.data.repository.TagRepository
import com.toolbox.data.repository.FolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId

    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allFolders: StateFlow<List<Folder>> = folderRepository.getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notes: StateFlow<List<Note>> = combine(
        _searchQuery.debounce(300).distinctUntilChanged(),
        _selectedTagId,
        _selectedFolderId
    ) { query, tagId, folderId ->
        Triple(query, tagId, folderId)
    }.flatMapLatest { (query, tagId, folderId) ->
        when {
            tagId != null -> noteRepository.getNotesByTag(tagId)
            folderId != null -> noteRepository.getNotesByFolder(folderId)
            query.isBlank() -> noteRepository.getAllNotes()
            else -> noteRepository.searchNotes(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTagFilterChange(tagId: Long?) {
        _selectedTagId.value = tagId
    }

    fun onFolderFilterChange(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun clearFilters() {
        _selectedTagId.value = null
        _selectedFolderId.value = null
        _searchQuery.value = ""
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }
}
