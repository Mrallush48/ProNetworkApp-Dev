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
interface ClientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client): Long

    @Upsert
    suspend fun upsert(client: Client)

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): LiveData<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: String): Client?

    @Query("SELECT COUNT(*) FROM clients")
    fun getClientsCount(): LiveData<Int>

    @Query("SELECT * FROM clients WHERE name LIKE '%' || :search || '%' ORDER BY name ASC")
    fun searchClients(search: String): LiveData<List<Client>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    suspend fun getAllClientsDirect(): List<Client>

    @Query("""
        SELECT * FROM clients 
        WHERE buildingId = :buildingId 
        AND startMonth <= :month 
        AND (endMonth IS NULL OR endMonth > :month) 
        ORDER BY name ASC
    """)
    fun getClientsByBuildingAndMonth(buildingId: String, month: String): Flow<List<Client>>

    @Query("""
        SELECT * FROM clients 
        WHERE buildingId = :buildingId 
        AND startMonth <= :month 
        AND (endMonth IS NULL OR endMonth > :month) 
        AND (name LIKE '%' || :query || '%' OR subscriptionNumber LIKE '%' || :query || '%') 
        ORDER BY name ASC
    """)
    fun searchClients(buildingId: String, month: String, query: String): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id IN (:ids)")
    suspend fun getClientsByIds(ids: List<String>): List<Client>

    /**
     * Optimistic Locking: updates only if version matches.
     * Returns 1 if updated, 0 if version mismatch.
     */
    @Query("""
        UPDATE clients SET 
            name = :name,
            subscriptionNumber = :subscriptionNumber,
            roomNumber = :roomNumber,
            mobile = :mobile,
            price = :price,
            firstMonthAmount = :firstMonthAmount,
            buildingId = :buildingId,
            startMonth = :startMonth,
            startDay = :startDay,
            endMonth = :endMonth,
            isPaid = :isPaid,
            paymentDate = :paymentDate,
            phone = :phone,
            address = :address,
            packageType = :packageType,
            notes = :notes,
            updatedAt = :updatedAt,
            version = :newVersion,
            checksum = :checksum
        WHERE id = :id AND version = :expectedVersion
    """)
    suspend fun updateWithVersionCheck(
        id: String,
        name: String,
        subscriptionNumber: String,
        roomNumber: String?,
        mobile: String?,
        price: Double,
        firstMonthAmount: Double?,
        buildingId: String,
        startMonth: String,
        startDay: Int,
        endMonth: String?,
        isPaid: Boolean,
        paymentDate: Long?,
        phone: String,
        address: String,
        packageType: String,
        notes: String,
        updatedAt: Long,
        newVersion: Int,
        expectedVersion: Int,
        checksum: String
    ): Int
}
