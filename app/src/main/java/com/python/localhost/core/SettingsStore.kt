package com.python.localhost.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.python.localhost.data.AppSettings
import java.io.File

/**
 * Stores global app settings (plain JSON) and the GitHub token (encrypted at rest
 * using Android Keystore via EncryptedSharedPreferences). Tokens are NEVER written
 * to project files, logs, or source control.
 */
class SettingsStore(
    context: Context,
    private val appDirs: AppDirs,
    private val json: JsonStore,
) {
    private val file = File(appDirs.settings, "settings.json")

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "pymobile_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getSettings(): AppSettings {
        val s = json.read(file, AppSettings::class.java) ?: return AppSettings()
        // Migrate the old (incorrect) Chaquopy-only default index to the corrected pair.
        return if (s.pipIndexUrl == "https://chaquo.com/pypi-17.0" && s.pipExtraIndexUrl.isEmpty()) {
            s.copy(pipIndexUrl = "https://pypi.org/simple", pipExtraIndexUrl = "https://chaquo.com/pypi-13.1")
        } else {
            s
        }
    }

    fun saveSettings(s: AppSettings) = json.write(file, s)

    fun getGitHubToken(): String? = encryptedPrefs.getString("github_token", null)

    fun setGitHubToken(token: String?) {
        encryptedPrefs.edit().putString("github_token", token).apply()
    }
}
