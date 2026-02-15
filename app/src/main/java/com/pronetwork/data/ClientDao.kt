package com.pronetwork.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client)

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): LiveData<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Int): Client?

    @Query("SELECT COUNT(*) FROM clients")
    fun getClientsCount(): LiveData<Int>

    @Query("SELECT * FROM clients WHERE name LIKE '%' || :search || '%' ORDER BY name ASC")
    fun searchClients(search: String): LiveData<List<Client>>

    // New: Get clients by building and month (Flow)
    @Query(
        """
        SELECT * FROM clients
        WHERE buildingId = :buildingId
        AND startMonth <= :month
        AND (endMonth IS NULL OR endMonth > :month)
        ORDER BY name ASC
    """
    )
    fun getClientsByBuildingAndMonth(buildingId: Int, month: String): Flow<List<Client>>

    // New: Search clients by name or subscription number in a building and month (Flow)
    @Query(
        """
        SELECT * FROM clients
        WHERE buildingId = :buildingId
        AND startMonth <= :month
        AND (endMonth IS NULL OR endMonth > :month)
        AND (name LIKE '%' || :query || '%' OR subscriptionNumber LIKE '%' || :query || '%')
        ORDER BY name ASC
    """
    )
    fun searchClients(buildingId: Int, month: String, query: String): Flow<List<Client>>
}
