package com.ancata.prima_focus.widget

import android.content.Context
import android.util.Log

/** Single entry point to redraw every data-driven home screen widget after the task list changes. */
object WidgetUpdater {
    private const val TAG = "WidgetUpdater"

    suspend fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        try {
            TopThreeTasksWidget.refresh(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh TopThreeTasksWidget", e)
        }
        try {
            OldestDeferredTaskWidget.refresh(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh OldestDeferredTaskWidget", e)
        }
    }
}
