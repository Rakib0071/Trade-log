package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.edgeLogDataStore: DataStore<Preferences> by preferencesDataStore(name = "edgelog_prefs")

class AppPreferences(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val selectedAccountIdKey = stringPreferencesKey("selected_account_id_v2")

    val themeMode: Flow<AppThemeMode> = context.edgeLogDataStore.data.map { prefs ->
        val raw = prefs[themeModeKey] ?: AppThemeMode.DARK.name
        AppThemeMode.entries.firstOrNull { it.name == raw } ?: AppThemeMode.DARK
    }

    /** Empty string means All Accounts combined */
    val selectedAccountId: Flow<String> = context.edgeLogDataStore.data.map { prefs ->
        prefs[selectedAccountIdKey] ?: ALL_ACCOUNTS_ID
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.edgeLogDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun setSelectedAccountId(accountId: String) {
        context.edgeLogDataStore.edit { prefs ->
            prefs[selectedAccountIdKey] = accountId
        }
    }

    companion object {
        const val ALL_ACCOUNTS_ID = ""
    }
}
