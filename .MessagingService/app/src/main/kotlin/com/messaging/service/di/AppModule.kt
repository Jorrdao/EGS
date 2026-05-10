package com.messaging.service.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.work.WorkManager
import com.messaging.service.db.MessageDao
import com.messaging.service.db.MessageDatabase
import com.messaging.service.online.api.TokenProvider
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
    fun provideBluetoothManager(
        @ApplicationContext context: Context
    ): BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    @Provides
    @Singleton
    fun provideBluetoothAdapter(bluetoothManager: BluetoothManager): BluetoothAdapter =
        bluetoothManager.adapter

    @Provides
    @Singleton
    fun provideMessageDatabase(@ApplicationContext context: Context): MessageDatabase =
        MessageDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideMessageDao(db: MessageDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    /**
     * Replace this with your actual token source (SharedPreferences, DataStore,
     * authentication manager, etc.)
     */
    @Provides
    @Singleton
    fun provideTokenProvider(): TokenProvider = object : TokenProvider {
        override fun getToken(): String? = null   // TODO: inject real token
    }
}
