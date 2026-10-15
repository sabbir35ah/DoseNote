package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY CASE relationship WHEN 'SELF' THEN 0 WHEN 'PARENT' THEN 1 WHEN 'CHILD' THEN 2 ELSE 3 END")
    fun getAllProfilesFlow(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: String): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): Profile?

    @Query("SELECT COUNT(*) FROM profiles WHERE isCaregiverShared = 0")
    suspend fun getOwnedProfileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: Profile)

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)

    @Query("DELETE FROM profiles")
    suspend fun clear()
}
