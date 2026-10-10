package com.ancata.prima_focus.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.ancata.prima_focus.core.sync.SyncSettings
import com.ancata.prima_focus.utils.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemeSettings(val mode: ThemeMode, val dynamicColor: Boolean)

/**
 * Single access point for the app's SharedPreferences. It is a process-wide singleton so the
 * observable values (theme, emojis, history tracking) stay consistent between the activity,
 * the ViewModel, workers and widgets.
 */
class UserPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _themeSettings = MutableStateFlow(readThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    private val _categoryEmojis = MutableStateFlow(readCategoryEmojis())
    val categoryEmojis: StateFlow<Map<String, String>> = _categoryEmojis.asStateFlow()

    private val _isHistoryTrackingEnabled = MutableStateFlow(
        prefs.getBoolean(Constants.PREF_HISTORY_TRACKING_ENABLED, true)
    )
    val isHistoryTrackingEnabled: StateFlow<Boolean> = _isHistoryTrackingEnabled.asStateFlow()

    private val _isLongPendingCelebrationEnabled = MutableStateFlow(
        prefs.getBoolean(Constants.PREF_LONG_PENDING_CELEBRATION_ENABLED, true)
    )
    val isLongPendingCelebrationEnabled: StateFlow<Boolean> = _isLongPendingCelebrationEnabled.asStateFlow()

    private val _longPendingThresholdDays = MutableStateFlow(
        prefs.getInt(Constants.PREF_LONG_PENDING_THRESHOLD_DAYS, Constants.DEFAULT_LONG_PENDING_THRESHOLD_DAYS)
    )
    val longPendingThresholdDays: StateFlow<Int> = _longPendingThresholdDays.asStateFlow()

    var notificationFrequency: Int
        get() {
            val freq = prefs.getInt(Constants.PREF_NOTIFICATION_FREQUENCY, Constants.DEFAULT_NOTIFICATION_FREQUENCY)
            return if (freq !in listOf(-1, 90, 180, 300)) Constants.DEFAULT_NOTIFICATION_FREQUENCY else freq
        }
        set(value) = prefs.edit().putInt(Constants.PREF_NOTIFICATION_FREQUENCY, value).apply()

    var isDisconnectModeEnabled: Boolean
        get() = prefs.getBoolean(Constants.PREF_DISCONNECT_MODE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(Constants.PREF_DISCONNECT_MODE_ENABLED, value).apply()

    var disconnectStartTime: String
        get() = prefs.getString(Constants.PREF_DISCONNECT_START_TIME, "22:00") ?: "22:00"
        set(value) = prefs.edit().putString(Constants.PREF_DISCONNECT_START_TIME, value).apply()

    var disconnectEndTime: String
        get() = prefs.getString(Constants.PREF_DISCONNECT_END_TIME, "08:00") ?: "08:00"
        set(value) = prefs.edit().putString(Constants.PREF_DISCONNECT_END_TIME, value).apply()

    var manualBoostAmount: Double
        get() = prefs.getFloat(Constants.PREF_MANUAL_BOOST_AMOUNT, 10.0f).toDouble()
        set(value) = prefs.edit().putFloat(Constants.PREF_MANUAL_BOOST_AMOUNT, value.toFloat()).apply()

    var nonPostponableHealth: Boolean
        get() = prefs.getBoolean(Constants.PREF_NON_POSTPONABLE_HEALTH, true)
        set(value) = prefs.edit().putBoolean(Constants.PREF_NON_POSTPONABLE_HEALTH, value).apply()

    var nonPostponableUrgent: Boolean
        get() = prefs.getBoolean(Constants.PREF_NON_POSTPONABLE_URGENT, true)
        set(value) = prefs.edit().putBoolean(Constants.PREF_NON_POSTPONABLE_URGENT, value).apply()

    var historyTrackingEnabled: Boolean
        get() = _isHistoryTrackingEnabled.value
        set(value) {
            prefs.edit().putBoolean(Constants.PREF_HISTORY_TRACKING_ENABLED, value).apply()
            _isHistoryTrackingEnabled.value = value
        }

    var longPendingCelebrationEnabled: Boolean
        get() = _isLongPendingCelebrationEnabled.value
        set(value) {
            prefs.edit().putBoolean(Constants.PREF_LONG_PENDING_CELEBRATION_ENABLED, value).apply()
            _isLongPendingCelebrationEnabled.value = value
        }

    var longPendingThresholdDaysValue: Int
        get() = _longPendingThresholdDays.value
        set(value) {
            prefs.edit().putInt(Constants.PREF_LONG_PENDING_THRESHOLD_DAYS, value).apply()
            _longPendingThresholdDays.value = value
        }

    /** Global default for recurring tasks whose own missedPolicy is null. */
    var skipMissedOccurrences: Boolean
        get() = prefs.getBoolean(Constants.PREF_SKIP_MISSED_OCCURRENCES, true)
        set(value) {
            prefs.edit().putBoolean(Constants.PREF_SKIP_MISSED_OCCURRENCES, value).apply()
            touchSyncedSettings()
        }

    /** Timestamp of the last change to a setting that is synced between devices. */
    val settingsUpdatedAt: Long
        get() = prefs.getLong(Constants.PREF_SETTINGS_UPDATED_AT, 0L)

    /** Stable per-install identifier, generated lazily on first access. */
    val deviceId: String
        get() = prefs.getString(Constants.PREF_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(Constants.PREF_DEVICE_ID, it).apply()
        }

    fun getDisabledCategories(): Set<String> =
        prefs.getStringSet(Constants.PREF_DISABLED_CATEGORIES, emptySet()) ?: emptySet()

    fun setCategoryDisabled(category: String, disabled: Boolean) {
        val current = getDisabledCategories().toMutableSet()
        if (disabled) current.add(category) else current.remove(category)
        prefs.edit().putStringSet(Constants.PREF_DISABLED_CATEGORIES, current).apply()
    }

    fun categoryEmoji(category: String): String =
        _categoryEmojis.value[category.lowercase()] ?: Constants.FALLBACK_CATEGORY_EMOJI

    fun setCategoryEmoji(category: String, emoji: String) {
        writeCategoryEmojis(_categoryEmojis.value + (category.lowercase() to emoji))
        touchSyncedSettings()
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(Constants.PREF_THEME_MODE, mode.name).apply()
        _themeSettings.value = readThemeSettings()
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.PREF_DYNAMIC_COLOR, enabled).apply()
        _themeSettings.value = readThemeSettings()
    }

    /** Settings shared across devices. Theme is deliberately excluded: it is a per-device choice. */
    fun syncableSettings(): SyncSettings = SyncSettings(
        categoryEmojis = _categoryEmojis.value,
        skipMissedOccurrences = skipMissedOccurrences,
        updatedAt = settingsUpdatedAt
    )

    /** Applies settings received from a peer, keeping the peer's timestamp so both sides converge. */
    fun applySyncedSettings(settings: SyncSettings) {
        writeCategoryEmojis(Constants.DEFAULT_CATEGORY_EMOJIS + settings.categoryEmojis)
        prefs.edit()
            .putBoolean(Constants.PREF_SKIP_MISSED_OCCURRENCES, settings.skipMissedOccurrences)
            .putLong(Constants.PREF_SETTINGS_UPDATED_AT, settings.updatedAt)
            .apply()
    }

    private fun touchSyncedSettings() {
        prefs.edit().putLong(Constants.PREF_SETTINGS_UPDATED_AT, System.currentTimeMillis()).apply()
    }

    private fun writeCategoryEmojis(emojis: Map<String, String>) {
        prefs.edit().putString(Constants.PREF_CATEGORY_EMOJIS, gson.toJson(emojis)).apply()
        _categoryEmojis.value = emojis
    }

    private fun readCategoryEmojis(): Map<String, String> {
        val stored: Map<String, String> = prefs.getString(Constants.PREF_CATEGORY_EMOJIS, null)?.let { json ->
            runCatching {
                gson.fromJson<Map<String, String>>(json, object : TypeToken<Map<String, String>>() {}.type)
            }.getOrNull()
        } ?: emptyMap()
        return Constants.DEFAULT_CATEGORY_EMOJIS + stored
    }

    private fun readThemeSettings(): ThemeSettings {
        val mode = prefs.getString(Constants.PREF_THEME_MODE, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.SYSTEM
        return ThemeSettings(mode, prefs.getBoolean(Constants.PREF_DYNAMIC_COLOR, false))
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferences(context).also { INSTANCE = it }
            }
    }
}
