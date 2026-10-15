package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUserFlow(): Flow<User?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: User)

    @Update
    suspend fun update(user: User)

    @Query("UPDATE users SET isFamilyPlanSubscribed = :subscribed WHERE id = :userId")
    suspend fun setSubscription(userId: String, subscribed: Boolean)

    @Query("UPDATE users SET language = :language WHERE id = :userId")
    suspend fun updateLanguage(userId: String, language: String)

    @Query("DELETE FROM users")
    suspend fun clear()
}
