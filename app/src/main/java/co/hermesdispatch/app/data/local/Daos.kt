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
interface TaskLabelDao {
    @Query("SELECT * FROM task_labels")
    fun observeAll(): Flow<List<TaskLabelEntity>>

    @Query("SELECT label FROM task_labels WHERE sessionId = :sessionId")
    fun observeLabel(sessionId: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(label: TaskLabelEntity)

    @Query("DELETE FROM task_labels WHERE sessionId = :sessionId")
    suspend fun delete(sessionId: String)
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
