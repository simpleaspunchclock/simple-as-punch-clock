package com.simpleas.punchclock

import java.security.MessageDigest

object SecurityUtils {
    fun normalizeRecoveryAnswer(value: String): String {
        return value.trim().lowercase()
    }

    fun hashRecoveryAnswer(value: String): String {
        val normalized = normalizeRecoveryAnswer(value)
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}