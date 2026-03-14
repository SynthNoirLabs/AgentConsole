package com.example.agentconsole.di

import com.example.agentconsole.TermuxRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
