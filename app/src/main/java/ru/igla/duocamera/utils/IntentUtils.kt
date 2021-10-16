package ru.igla.duocamera.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Created by lashkov on 30/06/16.
 * Copyright (c) 2016 igla LLC. All rights reserved.
 */
object IntentUtils {
    fun startActivitySafely(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(e)
            false
        } catch (e: SecurityException) {
            Log.e(e)
            false
        }
    }
}