package com.toolbox.ui.armeasurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.data.local.entity.Measurement
import com.toolbox.data.repository.MeasurementRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

data class ArPoint(val x: Float, val y: Float, val z: Float)

enum class MeasureUnit(val label: String, val suffix: String, val factor: Double) {
    CM("厘米", "cm", 100.0),
    M("米", "m", 1.0),
    FT("英尺", "ft", 3.28084)
}

enum class ArMeasureMode(val label: String) {
    TWO_POINT("两点测量"),
    MULTI_POINT("多点测量")
}

data class SegmentInfo(val from: Int, val to: Int, val distanceCm: Double)

data class ArMeasurementUiState(
    val points: List<ArPoint> = emptyList(),
    val segments: List<SegmentInfo> = emptyList(),
    val totalDistanceCm: Double = 0.0,
    val currentUnit: MeasureUnit = MeasureUnit.CM,
    val measureMode: ArMeasureMode = ArMeasureMode.TWO_POINT,
    val isSaving: Boolean = false,
    val savedMessage: String? = null,
    val history: List<Measurement> = emptyList(),
    val showHistory: Boolean = false
)

@HiltViewModel
class ArMeasurementViewModel @Inject constructor(
    private val repository: MeasurementRepository,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArMeasurementUiState())
    val uiState: StateFlow<ArMeasurementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllMeasurements().collect { list ->
                _uiState.value = _uiState.value.copy(history = list)
            }
        }
    }

    fun addPoint(x: Float, y: Float, z: Float) {
        val current = _uiState.value
        val newPoint = ArPoint(x, y, z)
        val newPoints = current.points + newPoint

        val newSegments = if (newPoints.size >= 2) {
            val from = newPoints[newPoints.size - 2]
            val to = newPoints[newPoints.size - 1]
            val dist = distanceCm(from, to)
            current.segments + SegmentInfo(newPoints.size - 2, newPoints.size - 1, dist)
        } else {
            current.segments
        }

        val totalDistance = newSegments.sumOf { it.distanceCm }

        _uiState.value = current.copy(
            points = newPoints,
            segments = newSegments,
            totalDistanceCm = totalDistance
        )
    }

    fun undoLastPoint() {
        val current = _uiState.value
        if (current.points.isEmpty()) return

        val newPoints = current.points.dropLast(1)
        val newSegments = if (newPoints.size >= 2) {
            val from = newPoints[newPoints.size - 2]
            val to = newPoints[newPoints.size - 1]
            val dist = distanceCm(from, to)
            current.segments.dropLast(1) + SegmentInfo(newPoints.size - 2, newPoints.size - 1, dist)
        } else {
            emptyList()
        }

        _uiState.value = current.copy(
            points = newPoints,
            segments = newSegments,
            totalDistanceCm = newSegments.sumOf { it.distanceCm }
        )
    }

    fun clearPoints() {
        _uiState.value = _uiState.value.copy(
            points = emptyList(),
            segments = emptyList(),
            totalDistanceCm = 0.0
        )
    }

    fun setUnit(unit: MeasureUnit) {
        _uiState.value = _uiState.value.copy(currentUnit = unit)
    }

    fun setMode(mode: ArMeasureMode) {
        _uiState.value = _uiState.value.copy(measureMode = mode)
    }

    fun toggleHistory() {
        _uiState.value = _uiState.value.copy(showHistory = !_uiState.value.showHistory)
    }

    fun saveMeasurement(title: String) {
        val state = _uiState.value
        if (state.points.size < 2) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            try {
                val pointsJson = gson.toJson(state.points)
                val measurement = Measurement(
                    title = title.ifBlank { "测量 ${System.currentTimeMillis()}" },
                    pointsJson = pointsJson,
                    totalDistanceCm = state.totalDistanceCm,
                    pointCount = state.points.size,
                    unit = state.currentUnit.name
                )
                repository.insertMeasurement(measurement)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedMessage = "保存成功"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    fun saveReferenceMeasurement(title: String, distanceCm: Float, pointCount: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val measurement = Measurement(
                    title = title.ifBlank { "测量 ${System.currentTimeMillis()}" },
                    pointsJson = "[]",
                    totalDistanceCm = distanceCm.toDouble(),
                    pointCount = pointCount,
                    unit = _uiState.value.currentUnit.name
                )
                repository.insertMeasurement(measurement)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedMessage = "保存成功"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    fun clearSavedMessage() {
        _uiState.value = _uiState.value.copy(savedMessage = null)
    }

    fun deleteMeasurement(measurement: Measurement) {
        viewModelScope.launch {
            repository.deleteMeasurement(measurement)
        }
    }

    fun formatDistance(distanceCm: Double, unit: MeasureUnit): String {
        val converted = when (unit) {
            MeasureUnit.CM -> distanceCm
            MeasureUnit.M -> distanceCm / 100.0
            MeasureUnit.FT -> distanceCm / 100.0 * 3.28084
        }
        return when {
            converted >= 100 -> String.format("%.1f", converted)
            converted >= 10 -> String.format("%.2f", converted)
            else -> String.format("%.2f", converted)
        }
    }

    private fun distanceCm(a: ArPoint, b: ArPoint): Double {
        val dx = (a.x - b.x).toDouble()
        val dy = (a.y - b.y).toDouble()
        val dz = (a.z - b.z).toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz) * 100.0
    }
}
