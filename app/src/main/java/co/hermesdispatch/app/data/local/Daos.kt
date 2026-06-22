package co.hermesdispatch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun clear()

    /** Replace the whole cache so it always reflects the active profile only. */
    @Transaction
    suspend fun replaceAll(tasks: List<TaskEntity>) {
        clear()
        upsertAll(tasks)
    }
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY name")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(schedules: List<ScheduleEntity>)

    @Query("DELETE FROM schedules")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(schedules: List<ScheduleEntity>) {
        clear()
        upsertAll(schedules)
    }
}
