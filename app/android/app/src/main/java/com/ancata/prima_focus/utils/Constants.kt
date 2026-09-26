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

    const val PREF_NON_POSTPONABLE_HEALTH = "non_postponable_health"
    const val PREF_NON_POSTPONABLE_URGENT = "non_postponable_urgent"
    const val PREF_DISABLED_CATEGORIES = "disabled_categories"
    const val PREF_HISTORY_TRACKING_ENABLED = "history_tracking_enabled"
    const val PREF_CATEGORY_EMOJIS = "category_emojis"
    const val PREF_SETTINGS_UPDATED_AT = "settings_updated_at"
    const val PREF_THEME_MODE = "theme_mode"
    const val PREF_DYNAMIC_COLOR = "dynamic_color"
    const val PREF_SKIP_MISSED_OCCURRENCES = "skip_missed_occurrences"
    const val PREF_DEVICE_ID = "device_id"

    const val FALLBACK_CATEGORY_EMOJI = "📌"

    /** Default emoji per category (see docs/categories.md); users can override them in Settings. */
    val DEFAULT_CATEGORY_EMOJIS = mapOf(
        "trabajo" to "💼",
        "salud" to "💊",
        "amigos" to "🤝",
        "pareja" to "❤️",
        "familia" to "🏡",
        "crecimiento personal" to "🌱",
        "casa" to "🧹",
        "trámites" to "📄",
        "finanzas" to "💰"
    )

    // Worker & Notifications
    const val WORKER_NOTIFICATION = "NotificationWorker"
    const val WORKER_RECURRENCE = "RecurrenceReconciliationWorker"
    const val NOTIFICATION_CHANNEL_ID = "prima_focus_channel"
    const val NOTIFICATION_ID = 101

    // Widget Actions & Extras
    const val ACTION_WIDGET_COMPLETE_TASK = "com.ancata.prima_focus.ACTION_WIDGET_COMPLETE_TASK"
    const val ACTION_UNDO_COMPLETE_TASK = "com.ancata.prima_focus.ACTION_UNDO_COMPLETE_TASK"
    const val UNDO_NOTIFICATION_ID = 102
    const val UNDO_NOTIFICATION_TIMEOUT_MS = 5000L
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_PREFILLED_CATEGORY = "extra_prefilled_category"
}
