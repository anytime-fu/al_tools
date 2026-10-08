package com.toolbox.ui.note

import com.toolbox.ui.components.appClickable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.data.local.entity.Note
import com.toolbox.data.repository.NoteRepository
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(onBack: (() -> Unit)? = null) {
    val noteRepository: NoteRepository by inject(NoteRepository::class.java)
    val coroutineScope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    
    // Get notes from repository
    val notes by noteRepository.getAllNotes().collectAsState(initial = emptyList())
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Left panel - Note list
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Back / title row
            if (onBack != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "笔记",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                placeholder = { Text("搜索笔记...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            
            // Add note button
            Button(
                onClick = {
                    coroutineScope.launch {
                        val newNote = Note(
                            title = "新笔记",
                            content = ""
                        )
                        val id = noteRepository.insertNote(newNote)
                        selectedNote = newNote.copy(id = id)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
                Spacer(modifier = Modifier.width(8.dp))
                Text("新建笔记")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Note list
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                val filteredNotes = if (searchQuery.isBlank()) {
                    notes
                } else {
                    notes.filter { 
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
                    }
                }
                
                items(filteredNotes) { note ->
                    NoteListItem(
                        note = note,
                        isSelected = selectedNote?.id == note.id,
                        onClick = { selectedNote = note },
                        onDelete = {
                            coroutineScope.launch {
                                noteRepository.deleteNote(note)
                                if (selectedNote?.id == note.id) {
                                    selectedNote = null
                                }
                            }
                        }
                    )
                }
            }
        }
        
        // Right panel - Note editor
        if (selectedNote != null) {
            NoteEditor(
                note = selectedNote!!,
                onNoteChange = { updatedNote ->
                    coroutineScope.launch {
                        noteRepository.updateNote(updatedNote)
                        selectedNote = updatedNote
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "选择或创建一个笔记",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NoteListItem(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .appClickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "无标题" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content.take(50) + if (note.content.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun NoteEditor(
    note: Note,
    onNoteChange: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { 
                title = it
                onNoteChange(note.copy(title = it, updatedAt = System.currentTimeMillis()))
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("笔记标题") },
            textStyle = MaterialTheme.typography.headlineSmall,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = content,
            onValueChange = { 
                content = it
                onNoteChange(note.copy(content = it, updatedAt = System.currentTimeMillis()))
            },
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text("开始写作...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
