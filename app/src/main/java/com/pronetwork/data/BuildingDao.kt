package com.pronetwork.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.room.Upsert

@Dao
interface BuildingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(building: Building): Long

    // === A3: Upsert آمن من CASCADE ===
    @Upsert
    suspend fun upsert(building: Building)

    @Update
    suspend fun update(building: Building)

    @Delete
    suspend fun delete(building: Building)

    @Query("SELECT * FROM buildings ORDER BY name ASC")
    fun getAllBuildings(): LiveData<List<Building>>

    @Query("SELECT * FROM buildings ORDER BY name ASC")
    fun getAllBuildingsFlow(): Flow<List<Building>>

    @Query("SELECT * FROM buildings WHERE id = :id")
    suspend fun getBuildingById(id: Int): Building?

    @Query("SELECT * FROM buildings WHERE name LIKE '%' || :search || '%' ORDER BY name ASC")
    fun searchBuildings(search: String): LiveData<List<Building>>

    @Query("SELECT * FROM buildings WHERE name LIKE '%' || :search || '%' ORDER BY name ASC")
    fun searchBuildingsFlow(search: String): Flow<List<Building>>

    @Query("SELECT * FROM buildings ORDER BY name ASC")
    suspend fun getAllBuildingsDirect(): List<Building>

}
