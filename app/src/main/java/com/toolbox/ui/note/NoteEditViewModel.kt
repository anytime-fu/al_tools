package com.toolbox.ui.note

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.data.local.entity.Folder
import com.toolbox.data.local.entity.Note
import com.toolbox.data.local.entity.NoteTagCrossRef
import com.toolbox.data.local.entity.Tag
import com.toolbox.data.remote.AiApiService
import com.toolbox.data.repository.FolderRepository
import com.toolbox.data.repository.NoteRepository
import com.toolbox.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val folderRepository: FolderRepository,
    private val aiApiService: AiApiService
) : ViewModel() {

    fun getContext(): Context = context

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: -1L

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveComplete = MutableStateFlow(false)
    val saveComplete: StateFlow<Boolean> = _saveComplete

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds

    private val _allFolders = MutableStateFlow<List<Folder>>(emptyList())
    val allFolders: StateFlow<List<Folder>> = _allFolders

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId

    private var originalNote: Note? = null

    init {
        loadAllTags()
        loadAllFolders()
        if (noteId != -1L) {
            loadNote()
            loadNoteTags()
        }
    }

    private fun loadAllTags() {
        viewModelScope.launch {
            tagRepository.getAllTags().collect { tags ->
                _allTags.value = tags
            }
        }
    }

    private fun loadAllFolders() {
        viewModelScope.launch {
            folderRepository.getAllFolders().collect { folders ->
                _allFolders.value = folders
            }
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            noteRepository.getNoteById(noteId)?.let { note ->
                originalNote = note
                _title.value = note.title
                _content.value = note.content
                _selectedFolderId.value = note.folderId
            }
        }
    }

    private fun loadNoteTags() {
        viewModelScope.launch {
            noteRepository.getNoteTags(noteId).collect { tagIds ->
                _selectedTagIds.value = tagIds.toSet()
            }
        }
    }

    fun onTitleChange(title: String) {
        _title.value = title
    }

    fun onContentChange(content: String) {
        _content.value = content
    }

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val folder = Folder(name = name)
            folderRepository.insertFolder(folder)
        }
    }

    fun toggleTag(tagId: Long) {
        val currentTags = _selectedTagIds.value.toMutableSet()
        if (currentTags.contains(tagId)) {
            currentTags.remove(tagId)
        } else {
            currentTags.add(tagId)
        }
        _selectedTagIds.value = currentTags
    }

    fun createTag(name: String, color: Long) {
        viewModelScope.launch {
            val tag = Tag(name = name, color = color)
            tagRepository.insertTag(tag)
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
        }
    }

    fun aiAssist(promptType: String, selectedText: String = "") {
        viewModelScope.launch {
            _isAiProcessing.value = true
            _aiError.value = null

            try {
                val prompt = when (promptType) {
                    "continue" -> "请续写以下内容，保持风格一致：\n${_content.value}"
                    "polish" -> "请润色以下文本，使其更通顺优雅：\n${selectedText.ifBlank { _content.value }}"
                    "summarize" -> "请总结以下内容的核心要点：\n${_content.value}"
                    "translate_en" -> "请将以下内容翻译成英文：\n${selectedText.ifBlank { _content.value }}"
                    "translate_cn" -> "请将以下内容翻译成中文：\n${selectedText.ifBlank { _content.value }}"
                    else -> ""
                }

                val result = aiApiService.sendSinglePrompt(prompt)
                
                result.fold(
                    onSuccess = { aiText ->
                        if (aiText.isNotBlank()) {
                            _content.value = if (selectedText.isBlank() && promptType == "continue") {
                                _content.value + "\n" + aiText
                            } else {
                                aiText
                            }
                        }
                    },
                    onFailure = { exception ->
                        _aiError.value = exception.message ?: "AI处理失败"
                    }
                )
            } catch (e: Exception) {
                _aiError.value = "错误: ${e.message}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun clearAiError() {
        _aiError.value = null
    }

    fun exportNote(context: Context, format: String) {
        viewModelScope.launch {
            try {
                val fileName = "${_title.value.ifEmpty { "笔记" }}.$format"
                val content = if (format == "md") {
                    "# ${_title.value}\n\n${_content.value}"
                } else {
                    "${_title.value}\n\n${_content.value}"
                }
                
                val file = java.io.File(context.cacheDir, fileName)
                file.writeText(content)
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = if (format == "md") "text/markdown" else "text/plain"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_TITLE, fileName)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(android.content.Intent.createChooser(intent, "分享笔记"))
            } catch (e: Exception) {
                _aiError.value = "导出失败: ${e.message}"
            }
        }
    }

    fun save() {
        if (_title.value.isBlank()) return

        viewModelScope.launch {
            _isSaving.value = true

            val now = System.currentTimeMillis()
            val note = Note(
                id = if (noteId != -1L) noteId else 0,
                title = _title.value,
                content = _content.value,
                folderId = _selectedFolderId.value,
                isPinned = if (noteId != -1L) originalNote?.isPinned ?: false else false,
                updatedAt = now,
                createdAt = if (noteId != -1L && originalNote != null) originalNote!!.createdAt else now
            )

            val savedNoteId = noteRepository.insertNote(note)

            // 保存标签关联
            val noteIdToUse = if (noteId != -1L) noteId else savedNoteId
            noteRepository.deleteNoteTags(noteIdToUse)
            _selectedTagIds.value.forEach { tagId ->
                noteRepository.insertNoteTag(NoteTagCrossRef(noteId = noteIdToUse, tagId = tagId))
            }

            _isSaving.value = false
            _saveComplete.value = true
        }
    }
}
