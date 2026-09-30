package com.example.personalfinances

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.personalfinances.data.backup.AutoBackupScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

/**
 * Application entry point.
 *
 * It provides WorkManager's configuration so background workers (the automatic backup) can be
 * built by Hilt, and starts [AutoBackupScheduler], which watches the data for changes for as long
 * as the process lives. The scheduler only queues work; WorkManager runs it even if the app is
 * closed before it fires.
 */
@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var autoBackupScheduler: AutoBackupScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        autoBackupScheduler.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }
}
