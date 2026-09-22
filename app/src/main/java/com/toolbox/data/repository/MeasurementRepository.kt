package com.toolbox.data.repository

import com.toolbox.data.local.dao.MeasurementDao
import com.toolbox.data.local.entity.Measurement
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementRepository @Inject constructor(
    private val measurementDao: MeasurementDao
) {
    fun getAllMeasurements(): Flow<List<Measurement>> = measurementDao.getAllMeasurements()

    suspend fun getMeasurementById(id: Long): Measurement? =
        measurementDao.getMeasurementById(id)

    suspend fun insertMeasurement(measurement: Measurement): Long =
        measurementDao.insertMeasurement(measurement)

    suspend fun deleteMeasurement(measurement: Measurement) =
        measurementDao.deleteMeasurement(measurement)

    suspend fun deleteAllMeasurements() = measurementDao.deleteAllMeasurements()
}
