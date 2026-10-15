package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CaregiverLink
import kotlinx.coroutines.flow.Flow

@Dao
interface CaregiverLinkDao {
    @Query("SELECT * FROM caregiver_links WHERE profileId = :profileId ORDER BY invitedAt DESC")
    fun getLinksForProfileFlow(profileId: String): Flow<List<CaregiverLink>>

    @Query("SELECT * FROM caregiver_links ORDER BY invitedAt DESC")
    fun getAllLinksFlow(): Flow<List<CaregiverLink>>

    @Query("SELECT * FROM caregiver_links WHERE id = :id LIMIT 1")
    suspend fun getLinkById(id: String): CaregiverLink?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(link: CaregiverLink)

    @Update
    suspend fun update(link: CaregiverLink)

    @Delete
    suspend fun delete(link: CaregiverLink)
}
