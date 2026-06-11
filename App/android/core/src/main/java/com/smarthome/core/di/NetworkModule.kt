package com.smarthome.core.di

import com.smarthome.core.provisioning.ProvisioningRepository
import com.smarthome.core.provisioning.ProvisioningRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindProvisioningRepository(
        impl: ProvisioningRepositoryImpl
    ): ProvisioningRepository
}
