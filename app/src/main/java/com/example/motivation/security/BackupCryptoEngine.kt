package com.example.motivation.security

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoEngine {

    private const val HEADER_STRING = "FOCUS_BACKUP"
    private val HEADER_BYTES = HEADER_STRING.toByteArray(Charsets.UTF_8)
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BIT = 128

    /**
     * Derives a 256-bit AES key from the user's password and a salt.
     */
    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM and prepends the header, salt, and IV.
     */
    fun encrypt(plainBytes: ByteArray, password: CharArray): ByteArray {
        val random = SecureRandom()
        
        // Generate random 16-byte salt
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        
        // Derive cryptographic key
        val secretKey = deriveKey(password, salt)
        
        // Generate random 12-byte IV
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)
        
        // Initialize AES/GCM cipher
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        
        // Perform encryption
        val cipherText = cipher.doFinal(plainBytes)
        
        // Assemble final output byte array
        val outputStream = ByteArrayOutputStream()
        outputStream.write(HEADER_BYTES)
        
        // Write salt size and salt
        outputStream.write(ByteBuffer.allocate(4).putInt(salt.size).array())
        outputStream.write(salt)
        
        // Write IV size and IV
        outputStream.write(ByteBuffer.allocate(4).putInt(iv.size).array())
        outputStream.write(iv)
        
        // Write encrypted cipher text
        outputStream.write(cipherText)
        
        return outputStream.toByteArray()
    }

    /**
     * Decrypts encrypted bytes using the user's password, validating the header format.
     */
    fun decrypt(encryptedBytes: ByteArray, password: CharArray): ByteArray {
        val byteBuffer = ByteBuffer.wrap(encryptedBytes)
        
        // Read and verify magic header bytes
        val fileHeaderBytes = ByteArray(HEADER_BYTES.size)
        if (byteBuffer.remaining() < fileHeaderBytes.size) {
            throw IllegalArgumentException("File too small: Missing header.")
        }
        byteBuffer.get(fileHeaderBytes)
        val fileHeader = String(fileHeaderBytes, Charsets.UTF_8)
        if (fileHeader != HEADER_STRING) {
            throw IllegalArgumentException("Invalid backup file: Header mismatch.")
        }
        
        // Read salt size and salt
        if (byteBuffer.remaining() < 4) {
            throw IllegalArgumentException("Corrupted backup file: Missing salt size.")
        }
        val saltSize = byteBuffer.int
        if (saltSize != SALT_LENGTH) {
            throw IllegalArgumentException("Corrupted backup file: Unsupported salt size ($saltSize).")
        }
        if (byteBuffer.remaining() < saltSize) {
            throw IllegalArgumentException("Corrupted backup file: Missing salt bytes.")
        }
        val salt = ByteArray(saltSize)
        byteBuffer.get(salt)
        
        // Read IV size and IV
        if (byteBuffer.remaining() < 4) {
            throw IllegalArgumentException("Corrupted backup file: Missing IV size.")
        }
        val ivSize = byteBuffer.int
        if (ivSize != IV_LENGTH) {
            throw IllegalArgumentException("Corrupted backup file: Unsupported IV size ($ivSize).")
        }
        if (byteBuffer.remaining() < ivSize) {
            throw IllegalArgumentException("Corrupted backup file: Missing IV bytes.")
        }
        val iv = ByteArray(ivSize)
        byteBuffer.get(iv)
        
        // Read remaining encrypted payload
        val cipherTextSize = byteBuffer.remaining()
        if (cipherTextSize <= 0) {
            throw IllegalArgumentException("Corrupted backup file: Empty encrypted payload.")
        }
        val cipherText = ByteArray(cipherTextSize)
        byteBuffer.get(cipherText)
        
        // Derive key using the extracted salt
        val secretKey = deriveKey(password, salt)
        
        // Initialize AES/GCM cipher in decrypt mode
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        
        // Perform decryption
        return cipher.doFinal(cipherText)
    }
}
