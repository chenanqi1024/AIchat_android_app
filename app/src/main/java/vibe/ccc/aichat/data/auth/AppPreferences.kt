package vibe.ccc.aichat.data.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vibe.ccc.aichat.data.model.AppUser

private val Context.aiChatDataStore by preferencesDataStore(name = "aichat_preferences")

class AppPreferences(context: Context) {
    private val dataStore = context.applicationContext.aiChatDataStore

    val hasSeenOnboarding: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAS_SEEN_ONBOARDING] ?: false
    }

    val selectedRoleId: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SELECTED_ROLE_ID] ?: 0
    }

    val user: Flow<AppUser?> = dataStore.data.map { preferences ->
        val id = preferences[USER_ID] ?: 0
        val countryCode = preferences[USER_COUNTRY_CODE]
        val phoneNumber = preferences[USER_PHONE_NUMBER]
        if (id > 0 && countryCode != null && phoneNumber != null) {
            AppUser(id = id, countryCode = countryCode, phoneNumber = phoneNumber)
        } else {
            null
        }
    }

    suspend fun setHasSeenOnboarding(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING] = value
        }
    }

    suspend fun setSelectedRoleId(roleId: Int) {
        dataStore.edit { preferences ->
            preferences[SELECTED_ROLE_ID] = roleId
        }
    }

    suspend fun persistUser(user: AppUser) {
        dataStore.edit { preferences ->
            preferences[USER_ID] = user.id
            preferences[USER_COUNTRY_CODE] = user.countryCode
            preferences[USER_PHONE_NUMBER] = user.phoneNumber
        }
    }

    suspend fun clearUser() {
        dataStore.edit { preferences ->
            preferences.remove(USER_ID)
            preferences.remove(USER_COUNTRY_CODE)
            preferences.remove(USER_PHONE_NUMBER)
        }
    }

    private companion object {
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("hasSeenOnboarding")
        val SELECTED_ROLE_ID = intPreferencesKey("selectedRoleId")
        val USER_ID = intPreferencesKey("auth.user.id")
        val USER_COUNTRY_CODE = stringPreferencesKey("auth.user.countryCode")
        val USER_PHONE_NUMBER = stringPreferencesKey("auth.user.phoneNumber")
    }
}
