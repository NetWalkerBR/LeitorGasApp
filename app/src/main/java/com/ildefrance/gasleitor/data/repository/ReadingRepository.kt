package com.ildefrance.gasleitor.data.repository

import android.content.Context
import com.ildefrance.gasleitor.data.db.AppDatabase
import com.ildefrance.gasleitor.data.db.CycleEntity
import com.ildefrance.gasleitor.data.db.ReadingEntity
import com.ildefrance.gasleitor.data.model.ApartmentStatus
import com.ildefrance.gasleitor.data.model.Reading
import com.ildefrance.gasleitor.data.model.ReadingCycle
import com.ildefrance.gasleitor.util.ApartmentHelper

class ReadingRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val cycleDao = db.cycleDao()
    private val readingDao = db.readingDao()

    suspend fun startCycle(month: Int, year: Int): Long {
        val entity = CycleEntity(month = month, year = year)
        return cycleDao.insert(entity)
    }

    suspend fun finishCycle(cycleId: Long) {
        cycleDao.finish(cycleId, System.currentTimeMillis())
    }

    suspend fun getLatestCycle(): ReadingCycle? {
        return cycleDao.getLatest()?.let {
            ReadingCycle(it.id, it.month, it.year, it.startedAt, it.finishedAt)
        }
    }

    // Returns an open (unfinished) cycle for the given month/year, or null
    suspend fun getOpenCycleForMonth(month: Int, year: Int): ReadingCycle? {
        return cycleDao.getOpenCycleForMonth(month, year)?.let {
            ReadingCycle(it.id, it.month, it.year, it.startedAt, it.finishedAt)
        }
    }

    // Returns all cycles ordered newest first (for history screen)
    suspend fun getAllCycles(): List<ReadingCycle> {
        return cycleDao.getAllOrderedDesc().map {
            ReadingCycle(it.id, it.month, it.year, it.startedAt, it.finishedAt)
        }
    }

    suspend fun getCycleById(cycleId: Long): ReadingCycle? {
        return cycleDao.getAll().firstOrNull { it.id == cycleId }?.let {
            ReadingCycle(it.id, it.month, it.year, it.startedAt, it.finishedAt)
        }
    }

    suspend fun saveReading(cycleId: Long, apartment: String, floor: Int, value: Double): Long {
        val entity = ReadingEntity(
            cycleId = cycleId,
            apartment = apartment,
            floor = floor,
            value = value
        )
        return readingDao.insert(entity)
    }

    suspend fun getReadingForApartment(cycleId: Long, apartment: String): Reading? {
        return readingDao.getByApartment(cycleId, apartment)?.let {
            Reading(it.id, it.cycleId, it.apartment, it.floor, it.value, it.timestamp)
        }
    }

    suspend fun getApartmentStatusList(cycleId: Long): List<ApartmentStatus> {
        val doneReadings = readingDao.getByCycle(cycleId)
            .associateBy { it.apartment }

        return ApartmentHelper.getAllApartments().map { (apt, floor) ->
            val reading = doneReadings[apt]
            ApartmentStatus(
                apartment = apt,
                floor = floor,
                reading = reading?.value,
                isDone = reading != null
            )
        }
    }

    suspend fun getAllReadings(cycleId: Long): List<Reading> {
        return readingDao.getByCycle(cycleId).map {
            Reading(it.id, it.cycleId, it.apartment, it.floor, it.value, it.timestamp)
        }
    }

    suspend fun getCompletedCount(cycleId: Long): Int {
        return readingDao.countByCycle(cycleId)
    }

    suspend fun deleteCycle(cycleId: Long) {
        // Readings are deleted automatically via CASCADE foreign key
        cycleDao.deleteById(cycleId)
    }
}
