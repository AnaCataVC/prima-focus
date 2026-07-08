package com.ancata.prima_focus.utils

object Constants {
    const val PREF_FILE = "prima_focus_prefs"
    
    // Preferences Keys
    const val PREF_NOTIFICATION_FREQUENCY = "notification_frequency"
    const val PREF_DISCONNECT_MODE_ENABLED = "disconnect_mode_enabled"
    const val PREF_DISCONNECT_START_TIME = "disconnect_start_time"
    const val PREF_DISCONNECT_END_TIME = "disconnect_end_time"
    const val PREF_MANUAL_BOOST_AMOUNT = "manual_boost_amount"
    const val PREF_DEFAULT_RECURRENCE = "default_recurrence"
    const val PREF_DEFAULT_ESTIMATED_MINUTES = "default_estimated_minutes"
    const val PREF_DEFAULT_SUBTASKS_COUNT = "default_subtasks_count"
    const val PREF_AUTO_SPLIT = "auto_split"
    const val PREF_NON_POSTPONABLE_HEALTH = "non_postponable_health"
    const val PREF_NON_POSTPONABLE_URGENT = "non_postponable_urgent"
    const val PREF_DISABLED_CATEGORIES = "disabled_categories"

    // Worker & Notifications
    const val WORKER_NOTIFICATION = "NotificationWorker"
    const val NOTIFICATION_CHANNEL_ID = "prima_focus_channel"
    const val NOTIFICATION_ID = 101
}
