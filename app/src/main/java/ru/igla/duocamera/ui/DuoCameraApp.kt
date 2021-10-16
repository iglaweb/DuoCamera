package ru.igla.duocamera.ui

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import ru.igla.duocamera.BuildConfig
import ru.igla.duocamera.utils.DevelopReportingTree
import timber.log.Timber

/**
 * Created by lashkov on 11/10/20.
 * Copyright (c) 2021 igla. All rights reserved.
 */
class DuoCameraApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DevelopReportingTree())
        }
        if (BuildConfig.DEBUG && enableStrictMode) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }
    }

    companion object {
        private const val enableStrictMode = false
    }
}