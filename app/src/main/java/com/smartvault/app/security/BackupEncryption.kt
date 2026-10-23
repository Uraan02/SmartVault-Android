package com.smartvault.app.security

import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object BackupEncryption {
    private const val ITERATIONS = 20_000
    private const val KEY_SIZE = 256
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    fun encryptBackup(plainJson: String, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_SIZE)
        val iv = ByteArray(IV_SIZE)
        val random = SecureRandom()
        random.nextBytes(salt)
        random.nextBytes(iv)

        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE, iv))

        val cipherText = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        // Format: [Salt (16 bytes)] [IV (12 bytes)] [Ciphertext (remaining)]
        val combined = ByteArray(SALT_SIZE + IV_SIZE + cipherText.size)
        System.arraycopy(salt, 0, combined, 0, SALT_SIZE)
        System.arraycopy(iv, 0, combined, SALT_SIZE, IV_SIZE)
        System.arraycopy(cipherText, 0, combined, SALT_SIZE + IV_SIZE, cipherText.size)
        return combined
    }

    fun decryptBackup(encryptedBytes: ByteArray, password: CharArray): String {
        if (encryptedBytes.size < SALT_SIZE + IV_SIZE) {
            throw IllegalArgumentException("Invalid backup file format or file corrupted.")
        }

        val salt = ByteArray(SALT_SIZE)
        val iv = ByteArray(IV_SIZE)
        val cipherTextSize = encryptedBytes.size - SALT_SIZE - IV_SIZE
        val cipherText = ByteArray(cipherTextSize)

        System.arraycopy(encryptedBytes, 0, salt, 0, SALT_SIZE)
        System.arraycopy(encryptedBytes, 0 + SALT_SIZE, iv, 0, IV_SIZE)
        System.arraycopy(encryptedBytes, SALT_SIZE + IV_SIZE, cipherText, 0, cipherTextSize)

        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE, iv))

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
