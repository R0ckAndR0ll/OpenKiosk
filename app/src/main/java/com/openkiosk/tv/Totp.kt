package com.openkiosk.tv

import android.net.Uri
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Códigos de autenticador (TOTP, RFC 6238): HMAC-SHA1, 6 dígitos, cada 30 segundos.
 * Compatible con Google Authenticator, Microsoft Authenticator, Authy, 1Password, etc.
 * Funciona sin internet: la TV y el teléfono solo necesitan tener la hora correcta.
 */
object Totp {
    private const val PERIOD_S = 30L
    private const val DIGITS = 6

    fun generateSecret(): String = Base32.encode(SecurityStore.randomBytes(20))

    fun counterAt(timeMs: Long = System.currentTimeMillis()): Long = timeMs / 1000L / PERIOD_S

    fun code(secret: String, counter: Long): String {
        val key = Base32.decode(secret)
        val msg = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            msg[i] = (c and 0xff).toByte()
            c = c shr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val h = mac.doFinal(msg)
        val o = h[h.size - 1].toInt() and 0x0f
        val bin = ((h[o].toInt() and 0x7f) shl 24) or
            ((h[o + 1].toInt() and 0xff) shl 16) or
            ((h[o + 2].toInt() and 0xff) shl 8) or
            (h[o + 3].toInt() and 0xff)
        val value = bin % 1_000_000
        return value.toString().padStart(DIGITS, '0')
    }

    /**
     * Acepta el código de la ventana actual o una contigua (±30 s de desfase de reloj).
     * Un código ya usado no se puede reutilizar (protección contra repetición).
     * @return el contador que coincidió, o -1.
     */
    fun match(secret: String, input: String, lastUsedCounter: Long): Long {
        if (!input.matches(Regex("\\d{6}"))) return -1
        val now = counterAt()
        for (d in -1L..1L) {
            val c = now + d
            if (c > lastUsedCounter && code(secret, c) == input) return c
        }
        return -1
    }

    fun uri(secret: String, label: String): String =
        "otpauth://totp/" + Uri.encode("OpenKiosk:$label") +
            "?secret=$secret&issuer=OpenKiosk&algorithm=SHA1&digits=$DIGITS&period=$PERIOD_S"
}

/** Base32 (RFC 4648) sin relleno, el formato estándar de los secretos de autenticador. */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(data: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                sb.append(ALPHABET[(buffer shr (bits - 5)) and 31])
                bits -= 5
            }
        }
        if (bits > 0) sb.append(ALPHABET[(buffer shl (5 - bits)) and 31])
        return sb.toString()
    }

    fun decode(s: String): ByteArray {
        val clean = s.uppercase().filter { it != '=' && it != ' ' }
        val out = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (ch in clean) {
            val v = ALPHABET.indexOf(ch)
            require(v >= 0) { "Base32 inválido" }
            buffer = (buffer shl 5) or v
            bits += 5
            if (bits >= 8) {
                out.write((buffer shr (bits - 8)) and 0xff)
                bits -= 8
            }
        }
        return out.toByteArray()
    }
}
