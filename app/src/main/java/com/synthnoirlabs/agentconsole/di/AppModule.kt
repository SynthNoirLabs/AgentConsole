package com.synthnoirlabs.agentconsole.di

import android.content.Context
import androidx.room.Room
import com.synthnoirlabs.agentconsole.TermuxRepository
import com.synthnoirlabs.agentconsole.data.AppDatabase
import com.synthnoirlabs.agentconsole.data.ExecutionHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTermuxRepository(): TermuxRepository {
        return TermuxRepository()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "agent_console.db").build()
    }

    @Provides
    fun provideExecutionHistoryDao(database: AppDatabase): ExecutionHistoryDao {
        return database.executionHistoryDao()
    }
}
