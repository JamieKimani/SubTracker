package com.example.trackifyv1.models

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.appPrefs by preferencesDataStore(name = "app_prefs")
private val KEY_DARK_MODE       = booleanPreferencesKey("dark_mode")
private val KEY_ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")

class ThemeViewModel : ViewModel() {

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _onboardingSeen = MutableStateFlow(true)
    val onboardingSeen: StateFlow<Boolean> = _onboardingSeen

    fun init(context: Context) {
        viewModelScope.launch {
            val prefs = context.appPrefs.data.first()
            _isDarkMode.value      = prefs[KEY_DARK_MODE] ?: true
            _onboardingSeen.value  = prefs[KEY_ONBOARDING_SEEN] ?: false
        }
    }

    fun toggle(context: Context) {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        viewModelScope.launch {
            context.appPrefs.edit { it[KEY_DARK_MODE] = newValue }
        }
    }

    fun markOnboardingSeen(context: Context) {
        _onboardingSeen.value = true
        viewModelScope.launch {
            context.appPrefs.edit { it[KEY_ONBOARDING_SEEN] = true }
        }
    }
}
