package doichin.auth.lib

import doichin.auth.dto.UserCredentials
import java.security.spec.KeySpec
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val ALGORITHM = "PBKDF2WithHmacSHA512"
private const val ITERATIONS = 120_000
private const val KEY_LENGTH = 256
private const val STATIC_SALT = "{CpF{ALjf_=4%c@+cqcG@Uc:ejwwjM"

fun generatePasswordHash(password: String, salt: String): String {
    val combinedSalt = "$salt$STATIC_SALT".toByteArray()
    val factory: SecretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM)
    val spec: KeySpec = PBEKeySpec(password.toCharArray(), combinedSalt, ITERATIONS, KEY_LENGTH)
    val key: SecretKey = factory.generateSecret(spec)
    val hash: ByteArray = key.encoded
    return hash.toHexString()
}

fun verifyPassword(password: String, userCredentials: UserCredentials): Boolean {
    val passwordHash = generatePasswordHash(password, userCredentials.passwordSalt)
    return passwordHash == userCredentials.passwordHash
}

fun generateRandom(): String {
    val random = ByteArray(16)
    val secureRandom = SecureRandom()
    secureRandom.nextBytes(random)
    return random.toHexString()
}