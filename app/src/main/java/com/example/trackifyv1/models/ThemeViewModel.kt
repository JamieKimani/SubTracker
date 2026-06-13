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

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")

class ThemeViewModel : ViewModel() {

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun init(context: Context) {
        viewModelScope.launch {
            val stored = context.themeDataStore.data.first()[KEY_DARK_MODE]
            _isDarkMode.value = stored ?: true
        }
    }

    fun toggle(context: Context) {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        viewModelScope.launch {
            context.themeDataStore.edit { it[KEY_DARK_MODE] = newValue }
        }
    }
}
