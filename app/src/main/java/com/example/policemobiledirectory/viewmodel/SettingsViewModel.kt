package com.example.policemobiledirectory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel responsible for UI settings:
 * - Theme (dark/light mode)
 * - Font scale
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    fun adjustFontScale(increase: Boolean) {
        val step = 0.1f
        val current = _fontScale.value
        _fontScale.value = when {
            increase -> (current + step).coerceAtMost(1.8f)
            else -> (current - step).coerceAtLeast(0.8f)
        }
    }

    fun setFontScale(scale: Float) {
        _fontScale.value = scale.coerceIn(0.8f, 1.8f)
    }
}



