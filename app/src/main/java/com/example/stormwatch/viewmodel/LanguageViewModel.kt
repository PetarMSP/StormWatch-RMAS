package com.example.stormwatch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stormwatch.util.LanguagePrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguageViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = LanguagePrefs(app.applicationContext)

    val isSerbian: StateFlow<Boolean> =
        prefs.isSerbianFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    fun setLanguageSerbian(value: Boolean) {
        viewModelScope.launch { prefs.setIsSerbian(value) }
    }
}