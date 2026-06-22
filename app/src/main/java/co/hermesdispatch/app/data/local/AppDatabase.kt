package co.hermesdispatch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        ScheduleEntity::class,
        TaskLabelEntity::class,
        InboxItemStateEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun taskLabelDao(): TaskLabelDao
    abstract fun inboxStateDao(): InboxStateDao
}
