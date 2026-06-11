package com.smarthome.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smarthome.core.database.entity.SceneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes")
    fun observeAllScenes(): Flow<List<SceneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateScenes(scenes: List<SceneEntity>)

    @Query("DELETE FROM scenes WHERE id = :sceneId")
    suspend fun deleteScene(sceneId: String)
}
