package com.ildefrance.gasleitor.data.model

data class Reading(
    val id: Long = 0,
    val cycleId: Long,
    val apartment: String,
    val floor: Int,
    val value: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class ReadingCycle(
    val id: Long = 0,
    val month: Int,
    val year: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)

data class ApartmentStatus(
    val apartment: String,
    val floor: Int,
    val reading: Double?,
    val isDone: Boolean
)
