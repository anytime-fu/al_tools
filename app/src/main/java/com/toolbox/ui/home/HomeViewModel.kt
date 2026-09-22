package com.toolbox.ui.home

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.toolbox.data.repository.NoteRepository
import com.toolbox.data.local.entity.Note
import com.toolbox.di.PlainPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    @PlainPrefs private val prefs: SharedPreferences,
    private val gson: Gson
) : ViewModel() {

    val recentNotes: Flow<List<Note>> = noteRepository.getAllNotes()

    private val _toolOrder = MutableStateFlow<List<String>>(emptyList())
    val toolOrder: StateFlow<List<String>> = _toolOrder

    init {
        loadToolOrder()
    }

    private fun loadToolOrder() {
        val savedOrder = prefs.getString("tool_order", null)
        if (savedOrder != null) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                val saved = gson.fromJson<List<String>>(savedOrder, type)
                val defaults = getDefaultToolOrder()
                val merged = saved.toMutableList()
                defaults.forEach { id ->
                    if (id !in merged) {
                        merged.add(id)
                    }
                }
                _toolOrder.value = merged
            } catch (e: Exception) {
                _toolOrder.value = getDefaultToolOrder()
            }
        } else {
            _toolOrder.value = getDefaultToolOrder()
        }
    }

    private fun getDefaultToolOrder(): List<String> {
        return listOf("note", "document", "ai_tools", "calculator", "bmi", "schedule", "personal", "id_card", "password", "ar_measurement", "life_assistant")
    }

    fun saveToolOrder(order: List<String>) {
        _toolOrder.value = order
        prefs.edit().putString("tool_order", gson.toJson(order)).apply()
    }

    fun reorderTools(fromIndex: Int, toIndex: Int) {
        val currentOrder = _toolOrder.value.toMutableList()
        if (fromIndex in currentOrder.indices && toIndex in currentOrder.indices) {
            val item = currentOrder.removeAt(fromIndex)
            currentOrder.add(toIndex, item)
            saveToolOrder(currentOrder)
        }
    }
}
