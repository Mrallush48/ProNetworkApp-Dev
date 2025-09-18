package com.pronetwork.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    // جلب عملاء المبنى للشهر المختار (startMonth <= month AND (endMonth IS NULL OR endMonth > month))
    @Query("""
        SELECT * FROM clients
        WHERE buildingId = :buildingId
        AND startMonth <= :month
        AND (endMonth IS NULL OR endMonth > :month)
        ORDER BY name ASC
    """)
    fun getClientsByBuildingAndMonth(buildingId: Int, month: String): Flow<List<Client>>

    // بحث داخل نفس نطاق الشهر
    @Query("""
        SELECT * FROM clients
        WHERE buildingId = :buildingId
        AND startMonth <= :month
        AND (endMonth IS NULL OR endMonth > :month)
        AND (name LIKE '%' || :query || '%' OR subscriptionNumber LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun searchClients(buildingId: Int, month: String, query: String): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client)

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)
}
