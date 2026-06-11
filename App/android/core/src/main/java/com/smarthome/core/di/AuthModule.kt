package com.smarthome.core.di

import com.smarthome.core.datastore.DataStoreTokenManager
import com.smarthome.core.datastore.TokenManager
import com.smarthome.core.repository.AuthRepository
import com.smarthome.core.repository.CognitoAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    abstract fun bindTokenManager(
        dataStoreTokenManager: DataStoreTokenManager
    ): TokenManager

    @Binds
    abstract fun bindAuthRepository(
        cognitoAuthRepository: CognitoAuthRepository
    ): AuthRepository
}
