package com.pronetwork.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildingDao {
    @Query("SELECT * FROM buildings ORDER BY name ASC")
    fun getAllBuildings(): Flow<List<Building>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(building: Building)

    @Update
    suspend fun update(building: Building)

    @Delete
    suspend fun delete(building: Building)
}
