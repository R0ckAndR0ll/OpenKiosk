package com.openkiosk.tv

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Seguridad del anfitrión:
 *  - PIN de 6 dígitos guardado como hash PBKDF2 con sal (nunca en texto plano).
 *  - Bloqueo progresivo tras intentos fallidos (sobrevive reinicios).
 *  - Código de recuperación de un solo uso (también como hash).
 *  - Autenticador TOTP (RFC 6238) como alternativa al PIN y para recuperarlo.
 *
 * No existe PIN maestro ni reseteo remoto sin firma: el último recurso es borrar los datos de la
 * app con ADB desde una computadora ya autorizada en la TV (acceso físico).
 */
class SecurityStore(context: Context) {

    enum class Result { OK_PIN, OK_TOTP, OK_RECOVERY, WRONG, LOCKED }

    private val sp = context.getSharedPreferences("openkiosk_sec", Context.MODE_PRIVATE)
    private val legacy = context.getSharedPreferences("openkiosk", Context.MODE_PRIVATE)

    private val legacyPin: String get() = legacy.getString("pin", "") ?: ""

    val hasPin: Boolean get() = sp.contains(K_PIN_HASH) || legacyPin.isNotEmpty()

    /** PIN antiguo (v0.1, 4–8 dígitos en texto plano): hay que cambiarlo por uno de 6 dígitos. */
    val mustChangePin: Boolean get() = !sp.contains(K_PIN_HASH) && legacyPin.isNotEmpty()

    val hasTotp: Boolean get() = sp.contains(K_TOTP)
    val hasRecovery: Boolean get() = sp.contains(K_REC_HASH)

    /** Hay PIN pero falta el código de recuperación o decidir sobre el autenticador. */
    val setupPending: Boolean get() = hasPin && (!hasRecovery || (!hasTotp && !sp.getBoolean(K_TOTP_SKIPPED, false)))

    fun setPin(pin: String) {
        require(isValidPin(pin))
        val salt = randomBytes(16)
        sp.edit()
            .putString(K_PIN_SALT, b64(salt))
            .putString(K_PIN_HASH, b64(pbkdf2(pin, salt)))
            .apply()
        legacy.edit().remove("pin").apply()
    }

    /** Milisegundos que faltan para poder volver a intentar (0 = no está bloqueado). */
    fun lockRemainingMs(): Long = maxOf(0L, sp.getLong(K_LOCK_UNTIL, 0L) - System.currentTimeMillis())

    /** Acepta el PIN o el código actual del autenticador. */
    fun verifyPinOrTotp(input: String): Result {
        if (lockRemainingMs() > 0) return Result.LOCKED
        val code = input.trim()
        if (checkPin(code)) {
            resetFailures()
            return Result.OK_PIN
        }
        val secret = sp.getString(K_TOTP, null)
        if (secret != null) {
            val matched = Totp.match(secret, code, sp.getLong(K_TOTP_LAST, -1L))
            if (matched >= 0) {
                sp.edit().putLong(K_TOTP_LAST, matched).apply()
                resetFailures()
                return Result.OK_TOTP
            }
        }
        registerFailure()
        return if (lockRemainingMs() > 0) Result.LOCKED else Result.WRONG
    }

    /** Código de recuperación de un solo uso: si es correcto se invalida y hay que generar otro. */
    fun verifyRecovery(input: String): Result {
        if (lockRemainingMs() > 0) return Result.LOCKED
        val salt = sp.getString(K_REC_SALT, null)?.let { unb64(it) }
        val hash = sp.getString(K_REC_HASH, null)?.let { unb64(it) }
        val normalized = normalizeRecovery(input)
        if (salt != null && hash != null && normalized.length == RECOVERY_LEN &&
            MessageDigest.isEqual(pbkdf2(normalized, salt), hash)
        ) {
            sp.edit().remove(K_REC_HASH).remove(K_REC_SALT).apply()
            resetFailures()
            return Result.OK_RECOVERY
        }
        registerFailure()
        return if (lockRemainingMs() > 0) Result.LOCKED else Result.WRONG
    }

    /** Genera un código nuevo, guarda solo su hash y lo devuelve para mostrarlo UNA vez. */
    fun newRecoveryCode(): String {
        val rnd = SecureRandom()
        val raw = (1..RECOVERY_LEN).map { RECOVERY_ALPHABET[rnd.nextInt(RECOVERY_ALPHABET.length)] }.joinToString("")
        val salt = randomBytes(16)
        sp.edit()
            .putString(K_REC_SALT, b64(salt))
            .putString(K_REC_HASH, b64(pbkdf2(raw, salt)))
            .apply()
        return raw.chunked(4).joinToString("-")
    }

    fun enableTotp(secret: String, usedCounter: Long) {
        sp.edit()
            .putString(K_TOTP, secret)
            .putLong(K_TOTP_LAST, usedCounter)
            .putBoolean(K_TOTP_SKIPPED, false)
            .apply()
    }

    fun skipTotp() {
        sp.edit().remove(K_TOTP).remove(K_TOTP_LAST).putBoolean(K_TOTP_SKIPPED, true).apply()
    }

    private fun checkPin(input: String): Boolean {
        if (mustChangePin) {
            return MessageDigest.isEqual(input.toByteArray(), legacyPin.toByteArray())
        }
        val salt = sp.getString(K_PIN_SALT, null)?.let { unb64(it) } ?: return false
        val hash = sp.getString(K_PIN_HASH, null)?.let { unb64(it) } ?: return false
        if (!isValidPin(input)) return false
        return MessageDigest.isEqual(pbkdf2(input, salt), hash)
    }

    private fun registerFailure() {
        val fails = sp.getInt(K_FAILS, 0) + 1
        val lockMs = when {
            fails < 5 -> 0L
            fails == 5 -> 60_000L
            fails == 6 -> 5 * 60_000L
            fails == 7 -> 15 * 60_000L
            else -> 60 * 60_000L
        }
        sp.edit()
            .putInt(K_FAILS, fails)
            .putLong(K_LOCK_UNTIL, if (lockMs > 0) System.currentTimeMillis() + lockMs else 0L)
            .apply()
    }

    private fun resetFailures() {
        sp.edit().putInt(K_FAILS, 0).putLong(K_LOCK_UNTIL, 0L).apply()
    }

    companion object {
        private const val K_PIN_HASH = "pin_hash"
        private const val K_PIN_SALT = "pin_salt"
        private const val K_REC_HASH = "rec_hash"
        private const val K_REC_SALT = "rec_salt"
        private const val K_TOTP = "totp_secret"
        private const val K_TOTP_LAST = "totp_last_counter"
        private const val K_TOTP_SKIPPED = "totp_skipped"
        private const val K_FAILS = "fail_count"
        private const val K_LOCK_UNTIL = "lock_until"

        private const val ITERATIONS = 20_000
        private const val RECOVERY_LEN = 12
        // Sin caracteres que se confunden (0/O, 1/I/L).
        private const val RECOVERY_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

        fun isValidPin(p: String): Boolean = p.matches(Regex("\\d{6}"))

        fun normalizeRecovery(s: String): String =
            s.uppercase().filter { it.isLetterOrDigit() }

        private fun pbkdf2(secret: String, salt: ByteArray): ByteArray {
            val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, 256)
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        }

        fun randomBytes(n: Int): ByteArray = ByteArray(n).also { SecureRandom().nextBytes(it) }
        private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
        private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)
    }
}
