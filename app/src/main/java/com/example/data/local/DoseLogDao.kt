package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DoseLog
import com.example.data.model.DoseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Query("""
        SELECT * FROM dose_logs 
        WHERE profileId = :profileId AND scheduledAt >= :startTime AND scheduledAt <= :endTime 
        ORDER BY scheduledAt ASC
    """)
    fun getLogsForProfileAndRangeFlow(profileId: String, startTime: Long, endTime: Long): Flow<List<DoseLog>>

    @Query("""
        SELECT * FROM dose_logs 
        WHERE profileId = :profileId AND scheduledAt >= :startTime AND scheduledAt <= :endTime 
        ORDER BY scheduledAt ASC
    """)
    suspend fun getLogsForProfileAndRange(profileId: String, startTime: Long, endTime: Long): List<DoseLog>

    @Query("SELECT * FROM dose_logs WHERE id = :id LIMIT 1")
    suspend fun getDoseLogById(id: String): DoseLog?

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId AND scheduledAt = :scheduledAt LIMIT 1")
    suspend fun getLogByMedAndSchedule(medicationId: String, scheduledAt: Long): DoseLog?

    @Query("""
        SELECT * FROM dose_logs 
        WHERE scheduledAt >= :fromTime AND scheduledAt <= :toTime AND status = 'PENDING'
        ORDER BY scheduledAt ASC
    """)
    suspend fun getPendingDoseLogsInRange(fromTime: Long, toTime: Long): List<DoseLog>

    @Query("""
        UPDATE dose_logs 
        SET status = 'MISSED' 
        WHERE status = 'PENDING' AND scheduledAt < :thresholdTime
    """)
    suspend fun markMissedDosesBefore(thresholdTime: Long): Int

    @Query("""
        UPDATE dose_logs 
        SET status = :status, respondedAt = :respondedAt, note = :note 
        WHERE id = :id
    """)
    suspend fun updateStatus(id: String, status: DoseStatus, respondedAt: Long, note: String? = null)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(logs: List<DoseLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: DoseLog)

    @Update
    suspend fun update(log: DoseLog)

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId AND scheduledAt > :fromTime")
    suspend fun deleteFutureLogsForMedication(medicationId: String, fromTime: Long)

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId")
    suspend fun deleteAllLogsForMedication(medicationId: String)
}
