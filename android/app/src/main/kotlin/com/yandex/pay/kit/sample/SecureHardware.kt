package com.yandex.pay.kit.sample

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory

internal object SecureHardware {

    private const val PROVIDER = "AndroidKeyStore"
    private const val PROBE_ALIAS = "ypay_kit_sample_secure_hw_probe"

    const val UNAVAILABLE_MESSAGE = "Secured key storage is unavailable. " +
        "Use physical device with TEE/StrongBox."

    fun isAvailable(): Boolean = try {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                PROBE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        val key = generator.generateKey()
        val factory = SecretKeyFactory.getInstance(key.algorithm, PROVIDER)
        val info = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
        val secure = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            info.securityLevel != KeyProperties.SECURITY_LEVEL_SOFTWARE &&
                info.securityLevel != KeyProperties.SECURITY_LEVEL_UNKNOWN
        } else {
            @Suppress("DEPRECATION")
            info.isInsideSecureHardware
        }
        secure
    } catch (e: Throwable) {
        Log.e(
            "SecureHardware",
            "Secure hardware probing resulted in error, fallback to hardware not available",
            e
        )
        return false
    } finally {
        KeyStore.getInstance(PROVIDER).apply { load(null) }.deleteEntry(PROBE_ALIAS)
    }
}
