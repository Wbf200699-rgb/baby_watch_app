package com.example.baby_watch.data.server

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class ServerCredentialStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ServerCredentials {
        val baseUrl = preferences.getString(KEY_BASE_URL, DEFAULT_SERVER_URL)
            ?.trim()
            ?.trimEnd('/')
            .orEmpty()
            .ifBlank { DEFAULT_SERVER_URL }
        val username = preferences.getString(KEY_USERNAME, "").orEmpty()
        val password = decryptPassword()
        return ServerCredentials(
            baseUrl = baseUrl,
            deviceId = FIXED_DEVICE_ID,
            username = username,
            password = password,
        )
    }

    fun view(): ServerConfigurationView {
        val credentials = load()
        return ServerConfigurationView(
            baseUrl = credentials.baseUrl,
            deviceId = credentials.deviceId,
            username = credentials.username,
            hasPassword = credentials.password.isNotBlank(),
        )
    }

    fun save(baseUrl: String, username: String, newPassword: String?) {
        val normalizedUrl = baseUrl.trim().trimEnd('/')
        val editor = preferences.edit()
            .putString(KEY_BASE_URL, normalizedUrl)
            .putString(KEY_USERNAME, username.trim())
        if (!newPassword.isNullOrEmpty()) {
            val encrypted = encrypt(newPassword)
            editor
                .putString(KEY_PASSWORD_CIPHERTEXT, encrypted.ciphertext)
                .putString(KEY_PASSWORD_IV, encrypted.iv)
        }
        editor.apply()
    }

    fun lastHandledEventId(): Long? {
        return if (preferences.contains(KEY_LAST_HANDLED_EVENT_ID)) {
            preferences.getLong(KEY_LAST_HANDLED_EVENT_ID, 0L)
        } else {
            null
        }
    }

    fun setLastHandledEventId(id: Long) {
        preferences.edit().putLong(KEY_LAST_HANDLED_EVENT_ID, id).apply()
    }

    private fun encrypt(plaintext: String): EncryptedValue {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedValue(
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
        )
    }

    private fun decryptPassword(): String {
        val ciphertextText = preferences.getString(KEY_PASSWORD_CIPHERTEXT, null) ?: return ""
        val ivText = preferences.getString(KEY_PASSWORD_IV, null) ?: return ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivText, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val plaintext = cipher.doFinal(Base64.decode(ciphertextText, Base64.NO_WRAP))
            plaintext.toString(Charsets.UTF_8)
        }.getOrElse { "" }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private data class EncryptedValue(
        val ciphertext: String,
        val iv: String,
    )

    private companion object {
        const val PREFERENCES_NAME = "mobile_server_settings"
        const val KEY_BASE_URL = "base_url"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD_CIPHERTEXT = "password_ciphertext"
        const val KEY_PASSWORD_IV = "password_iv"
        const val KEY_LAST_HANDLED_EVENT_ID = "last_handled_event_id"
        const val KEY_ALIAS = "baby_watch_mobile_server_credentials"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
