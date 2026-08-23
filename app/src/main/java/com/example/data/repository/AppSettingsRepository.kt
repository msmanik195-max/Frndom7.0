package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("frndom_app_settings", Context.MODE_PRIVATE)

    private val _autoPlayVideos = MutableStateFlow(prefs.getBoolean(KEY_AUTOPLAY_VIDEOS, true))
    val autoPlayVideos: StateFlow<Boolean> = _autoPlayVideos.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _dataSaverEnabled = MutableStateFlow(prefs.getBoolean(KEY_DATA_SAVER_ENABLED, false))
    val dataSaverEnabled: StateFlow<Boolean> = _dataSaverEnabled.asStateFlow()

    fun setAutoPlayVideos(enabled: Boolean) {
        _autoPlayVideos.value = enabled
        prefs.edit().putBoolean(KEY_AUTOPLAY_VIDEOS, enabled).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun setDataSaverEnabled(enabled: Boolean) {
        _dataSaverEnabled.value = enabled
        prefs.edit().putBoolean(KEY_DATA_SAVER_ENABLED, enabled).apply()
    }

    companion object {
        private const val KEY_AUTOPLAY_VIDEOS = "autoplay_videos"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DATA_SAVER_ENABLED = "data_saver_enabled"

        @Volatile
        private var instance: AppSettingsRepository? = null

        fun getInstance(context: Context): AppSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: AppSettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
