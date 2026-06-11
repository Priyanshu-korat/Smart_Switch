package com.smarthome.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smarthome.core.database.entity.PendingCommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingCommandDao {
    @Query("SELECT * FROM pending_commands ORDER BY timestamp ASC")
    fun getPendingCommands(): List<PendingCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: PendingCommandEntity)

    @Query("DELETE FROM pending_commands WHERE commandId = :commandId")
    suspend fun deleteCommand(commandId: String)
    
    @Query("DELETE FROM pending_commands WHERE timestamp < :expirationTime")
    suspend fun deleteExpiredCommands(expirationTime: Long)
}
