package com.kianirani.jarvis.brain.discovery

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test

class QrPairingTest {

    @Test fun `qr matrix decodes back to the join payload`() {
        val payload = JoinPayload("192.168.1.7", 7799, "tok-123")
        val matrix = QrPairing.matrix(payload, size = 256)
        // rasterize to pixels and decode with zxing reader
        val pixels = IntArray(256 * 256) { i ->
            if (matrix[i / 256][i % 256]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(256, 256, pixels)))
        val text = QRCodeReader().decode(bitmap).text
        assertEquals(payload, JoinPayload.decode(text))
    }
}
