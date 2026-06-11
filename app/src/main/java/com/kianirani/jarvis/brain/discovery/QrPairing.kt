package com.kianirani.jarvis.brain.discovery

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Brain-side QR pairing (spec 2026-06-11-discovery-ux §5): encodes a
 * [JoinPayload] as a QR bit matrix the UI rasterizes. Pure JVM (zxing core),
 * so generation is unit-testable without Android.
 */
object QrPairing {
    /** Boolean matrix of the QR for [payload]; `true` = dark module. */
    fun matrix(payload: JoinPayload, size: Int = 512): Array<BooleanArray> {
        val m = QRCodeWriter().encode(payload.encode(), BarcodeFormat.QR_CODE, size, size)
        return Array(m.height) { y -> BooleanArray(m.width) { x -> m.get(x, y) } }
    }
}
