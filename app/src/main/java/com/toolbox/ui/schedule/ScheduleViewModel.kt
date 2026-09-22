package com.toolbox.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.data.local.entity.Schedule
import com.toolbox.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(Calendar.getInstance().timeInMillis)
    val selectedDate: StateFlow<Long> = _selectedDate

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _newTitle = MutableStateFlow("")
    val newTitle: StateFlow<String> = _newTitle

    private val _newDescription = MutableStateFlow("")
    val newDescription: StateFlow<String> = _newDescription

    private val _newTime = MutableStateFlow("")
    val newTime: StateFlow<String> = _newTime

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            scheduleRepository.getSchedulesByDate(getDayStart(_selectedDate.value)).collect { schedules ->
                _schedules.value = schedules
            }
        }
    }

    fun selectDate(date: Long) {
        _selectedDate.value = date
        loadSchedules()
    }

    fun showAddDialog() {
        _showAddDialog.value = true
        _newTitle.value = ""
        _newDescription.value = ""
        _newTime.value = ""
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
    }

    fun onTitleChange(title: String) {
        _newTitle.value = title
    }

    fun onDescriptionChange(description: String) {
        _newDescription.value = description
    }

    fun onTimeChange(time: String) {
        _newTime.value = time
    }

    fun addSchedule() {
        if (_newTitle.value.isBlank()) return

        viewModelScope.launch {
            val schedule = Schedule(
                title = _newTitle.value,
                description = _newDescription.value,
                date = getDayStart(_selectedDate.value),
                time = _newTime.value
            )
            scheduleRepository.insert(schedule)
            _showAddDialog.value = false
        }
    }

    fun toggleComplete(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepository.update(schedule.copy(isCompleted = !schedule.isCompleted))
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepository.delete(schedule)
        }
    }

    private fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
