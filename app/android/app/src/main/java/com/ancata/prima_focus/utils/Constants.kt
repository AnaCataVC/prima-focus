package com.ancata.prima_focus.utils

object Constants {
    const val PREF_FILE = "prima_focus_prefs"
    
    // Preferences Keys
    const val PREF_NOTIFICATION_FREQUENCY = "notification_frequency"
    const val DEFAULT_NOTIFICATION_FREQUENCY = 90
    const val PREF_DISCONNECT_MODE_ENABLED = "disconnect_mode_enabled"
    const val PREF_DISCONNECT_START_TIME = "disconnect_start_time"
    const val PREF_DISCONNECT_END_TIME = "disconnect_end_time"
    const val PREF_MANUAL_BOOST_AMOUNT = "manual_boost_amount"

    const val PREF_AUTO_SPLIT = "auto_split"
    const val PREF_NON_POSTPONABLE_HEALTH = "non_postponable_health"
    const val PREF_NON_POSTPONABLE_URGENT = "non_postponable_urgent"
    const val PREF_DISABLED_CATEGORIES = "disabled_categories"
    const val PREF_HISTORY_TRACKING_ENABLED = "history_tracking_enabled"

    // Worker & Notifications
    const val WORKER_NOTIFICATION = "NotificationWorker"
    const val WORKER_RECURRENCE = "RecurrenceReconciliationWorker"
    const val NOTIFICATION_CHANNEL_ID = "prima_focus_channel"
    const val NOTIFICATION_ID = 101
}
