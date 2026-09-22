package com.toolbox.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.data.local.entity.PasswordEntry
import com.toolbox.data.repository.PasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val passwords: StateFlow<List<PasswordEntry>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            passwordRepository.getAllPasswords()
        } else {
            passwordRepository.searchPasswords(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _editingPassword = MutableStateFlow<PasswordEntry?>(null)
    val editingPassword: StateFlow<PasswordEntry?> = _editingPassword

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun loadPassword(id: Long) {
        viewModelScope.launch {
            _editingPassword.value = passwordRepository.getPasswordById(id)
        }
    }

    fun clearEditingPassword() {
        _editingPassword.value = null
    }

    fun savePassword(id: Long?, appName: String, account: String, password: String, note: String) {
        viewModelScope.launch {
            if (id != null && id > 0) {
                val existing = passwordRepository.getPasswordById(id)
                if (existing != null) {
                    passwordRepository.updatePassword(
                        existing.copy(
                            appName = appName,
                            account = account,
                            password = password,
                            note = note,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } else {
                passwordRepository.insertPassword(
                    PasswordEntry(
                        appName = appName,
                        account = account,
                        password = password,
                        note = note
                    )
                )
            }
        }
    }

    fun deletePassword(password: PasswordEntry) {
        viewModelScope.launch {
            passwordRepository.deletePassword(password)
        }
    }
}
