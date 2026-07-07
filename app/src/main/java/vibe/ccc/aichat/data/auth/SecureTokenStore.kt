package vibe.ccc.aichat.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenStore(context: Context) {
    private val sharedPreferences: SharedPreferences? = runCatching {
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "aichat_secure_auth",
            MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()

    fun read(): String? = runCatching {
        sharedPreferences?.getString(ACCESS_TOKEN_KEY, null)
    }.getOrNull()

    fun save(token: String) {
        runCatching {
            sharedPreferences?.edit()
                ?.putString(ACCESS_TOKEN_KEY, token)
                ?.apply()
        }
    }

    fun delete() {
        runCatching {
            sharedPreferences?.edit()
                ?.remove(ACCESS_TOKEN_KEY)
                ?.apply()
        }
    }

    private companion object {
        const val ACCESS_TOKEN_KEY = "accessToken"
    }
}
