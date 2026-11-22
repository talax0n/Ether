package dev.ether.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Device-local UI memory, not account data: whether the intro was seen, and the last level shown on Profile. */
class DeviceStore(context: Context) {
    private val prefs = context.getSharedPreferences("ether", Context.MODE_PRIVATE)
    private val _seen = MutableStateFlow(prefs.getBoolean(ONBOARDING_SEEN, false))
    val onboardingSeen: StateFlow<Boolean> = _seen.asStateFlow()

    fun markOnboardingSeen() {
        prefs.edit().putBoolean(ONBOARDING_SEEN, true).apply()
        _seen.value = true
    }

    /** 0 until Profile has shown a level on this device. */
    var lastSeenLevel: Int
        get() = prefs.getInt(LAST_SEEN_LEVEL, 0)
        set(value) = prefs.edit().putInt(LAST_SEEN_LEVEL, value).apply()

    private companion object {
        const val ONBOARDING_SEEN = "onboarding_seen"
        const val LAST_SEEN_LEVEL = "last_seen_level"
    }
}
