package com.stemaker.arbeitsbericht.data.client

import androidx.room.*

@Dao
interface ClientDao {
    @Query("SELECT * FROM ClientDb ORDER BY name ASC")
    suspend fun getClients(): List<ClientDb>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clientDb: ClientDb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(clientDb: ClientDb)

    @Query("DELETE FROM ClientDb")
    fun deleteTable()

    @Query("DELETE FROM ClientDb WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Entity
data class ClientDb(
    @PrimaryKey val id: Int,
    var name: String = "",
    var street: String = "",
    var zip: String = "",
    var city: String = "",
    var distance: Int = 0,
    var useDistance: Boolean = false,
    var driveTime: String = "00:00",
    var useDriveTime: Boolean = false,
    var notes: String = ""
)