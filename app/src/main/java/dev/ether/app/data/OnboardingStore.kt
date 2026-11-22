package dev.ether.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether this device has seen the intro pages. Device-local UI state, not account data. */
class OnboardingStore(context: Context) {
    private val prefs = context.getSharedPreferences("ether", Context.MODE_PRIVATE)
    private val _seen = MutableStateFlow(prefs.getBoolean(KEY, false))
    val seen: StateFlow<Boolean> = _seen.asStateFlow()

    fun markSeen() {
        prefs.edit().putBoolean(KEY, true).apply()
        _seen.value = true
    }

    private companion object {
        const val KEY = "onboarding_seen"
    }
}
