package com.kianirani.jarvis.brain

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kianirani.jarvis.brain.data.OnnxEmbedder
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OnnxEmbedderTest {
    @Test
    fun embeddingHasCorrectShape() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val model = File(ctx.filesDir, "brain-models/minilm-l12-v2.onnx")
        assumeTrue("model not downloaded; skipping", model.exists())
        val vectors = OnnxEmbedder(model).embed(listOf("hello world"))
        assertEquals(1, vectors.size)
        assertEquals(384, vectors.first().size)
    }
}
