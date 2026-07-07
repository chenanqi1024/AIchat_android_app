package vibe.ccc.aichat.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenStore(context: Context) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        "aichat_secure_auth",
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun read(): String? = sharedPreferences.getString(ACCESS_TOKEN_KEY, null)

    fun save(token: String) {
        sharedPreferences.edit()
            .putString(ACCESS_TOKEN_KEY, token)
            .apply()
    }

    fun delete() {
        sharedPreferences.edit()
            .remove(ACCESS_TOKEN_KEY)
            .apply()
    }

    private companion object {
        const val ACCESS_TOKEN_KEY = "accessToken"
    }
}
