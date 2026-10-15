package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE profileId = :profileId AND isActive = 1 ORDER BY name ASC")
    fun getMedicationsByProfileFlow(profileId: String): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE profileId = :profileId AND isActive = 1 ORDER BY name ASC")
    suspend fun getMedicationsByProfile(profileId: String): List<Medication>

    @Query("SELECT * FROM medications WHERE isActive = 1")
    suspend fun getAllActiveMedications(): List<Medication>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    fun getMedicationByIdFlow(id: String): Flow<Medication?>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: String): Medication?

    @Query("SELECT * FROM medications WHERE profileId = :profileId AND remainingSupply IS NOT NULL AND remainingSupply <= lowSupplyThreshold AND isActive = 1")
    fun getLowSupplyMedicationsFlow(profileId: String): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE remainingSupply IS NOT NULL AND remainingSupply <= lowSupplyThreshold AND hasNotifiedLowSupply = 0 AND isActive = 1")
    suspend fun getUnnotifiedLowSupplyMedications(): List<Medication>

    @Query("UPDATE medications SET hasNotifiedLowSupply = 1 WHERE id = :id")
    suspend fun markLowSupplyNotified(id: String)

    @Query("UPDATE medications SET remainingSupply = MAX(0, remainingSupply - 1) WHERE id = :id AND remainingSupply IS NOT NULL")
    suspend fun decrementSupply(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(medication: Medication)

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("UPDATE medications SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: String)
}
