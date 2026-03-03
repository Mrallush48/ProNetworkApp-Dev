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
    suspend fun getBuildingById(id: String): Building?

    @Query("SELECT * FROM buildings WHERE name LIKE '%' || :search || '%' ORDER BY name ASC")
    fun searchBuildings(search: String): LiveData<List<Building>>

    @Query("SELECT * FROM buildings WHERE name LIKE '%' || :search || '%' ORDER BY name ASC")
    fun searchBuildingsFlow(search: String): Flow<List<Building>>

    @Query("SELECT * FROM buildings ORDER BY name ASC")
    suspend fun getAllBuildingsDirect(): List<Building>

    /**
     * Optimistic Locking: updates only if version matches.
     * Returns 1 if updated, 0 if version mismatch (another device modified it).
     */
    @Query("""
        UPDATE buildings SET 
            name = :name, 
            location = :location, 
            notes = :notes, 
            floors = :floors, 
            managerName = :managerName, 
            updatedAt = :updatedAt, 
            version = :newVersion
        WHERE id = :id AND version = :expectedVersion
    """)
    suspend fun updateWithVersionCheck(
        id: String,
        name: String,
        location: String,
        notes: String,
        floors: Int,
        managerName: String,
        updatedAt: Long,
        newVersion: Int,
        expectedVersion: Int
    ): Int
}
