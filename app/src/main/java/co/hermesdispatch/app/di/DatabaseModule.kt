package co.hermesdispatch.app.di

import android.content.Context
import androidx.room.Room
import co.hermesdispatch.app.data.local.AppDatabase
import co.hermesdispatch.app.data.local.ScheduleDao
import co.hermesdispatch.app.data.local.TaskDao
import co.hermesdispatch.app.data.local.TaskLabelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "hermes_dispatch.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideScheduleDao(db: AppDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    fun provideTaskLabelDao(db: AppDatabase): TaskLabelDao = db.taskLabelDao()
}
