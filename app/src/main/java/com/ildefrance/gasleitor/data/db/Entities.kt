package com.ildefrance.gasleitor.data.db

import androidx.room.*

// ─── Entities ────────────────────────────────────────────────────────────────

@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: Int,
    val year: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)

@Entity(
    tableName = "readings",
    foreignKeys = [ForeignKey(
        entity = CycleEntity::class,
        parentColumns = ["id"],
        childColumns = ["cycleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cycleId")]
)
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val apartment: String,
    val floor: Int,
    val value: Double,
    val timestamp: Long = System.currentTimeMillis()
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface CycleDao {
    @Insert
    suspend fun insert(cycle: CycleEntity): Long

    @Update
    suspend fun update(cycle: CycleEntity)

    @Query("SELECT * FROM cycles ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): CycleEntity?

    @Query("SELECT * FROM cycles ORDER BY id DESC")
    suspend fun getAll(): List<CycleEntity>

    @Query("UPDATE cycles SET finishedAt = :time WHERE id = :id")
    suspend fun finish(id: Long, time: Long)

    // Returns any unfinished cycle for the given month/year (to resume)
    @Query("SELECT * FROM cycles WHERE month = :month AND year = :year AND finishedAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun getOpenCycleForMonth(month: Int, year: Int): CycleEntity?

    // Returns all cycles ordered newest first (for history)
    @Query("SELECT * FROM cycles ORDER BY year DESC, month DESC, id DESC")
    suspend fun getAllOrderedDesc(): List<CycleEntity>

    @Query("DELETE FROM cycles WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: ReadingEntity): Long

    @Query("SELECT * FROM readings WHERE cycleId = :cycleId ORDER BY floor DESC, apartment ASC")
    suspend fun getByCycle(cycleId: Long): List<ReadingEntity>

    @Query("SELECT * FROM readings WHERE cycleId = :cycleId AND apartment = :apartment LIMIT 1")
    suspend fun getByApartment(cycleId: Long, apartment: String): ReadingEntity?

    @Query("SELECT COUNT(*) FROM readings WHERE cycleId = :cycleId")
    suspend fun countByCycle(cycleId: Long): Int
}
