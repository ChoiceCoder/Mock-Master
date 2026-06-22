package com.govtprep.app.di

import com.govtprep.app.data.remote.AuthRepository
import com.govtprep.app.data.remote.ExamRepository
import com.govtprep.app.data.remote.TestRepository
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
    fun provideAuthRepository(): AuthRepository = AuthRepository()

    @Provides
    @Singleton
    fun provideExamRepository(): ExamRepository = ExamRepository()

    @Provides
    @Singleton
    fun provideTestRepository(): TestRepository = TestRepository()
}
