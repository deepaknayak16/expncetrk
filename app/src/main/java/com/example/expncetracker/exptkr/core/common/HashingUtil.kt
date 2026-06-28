package com.example.expncetracker.exptkr.core.common

import java.security.MessageDigest

object HashingUtil {
    /**
     * Generates a SHA-256 hash based purely on the sender and raw body.
     * This is completely invariant to timestamp source (PDU vs ContentProvider)
     * or minor regex parsing differences.
     */
    fun generateSmsHash(senderAddress: String?, rawBody: String?): String {
        if (senderAddress == null || rawBody == null) return ""
        
        val input = "${senderAddress.uppercase().trim()}|${rawBody.trim()}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
