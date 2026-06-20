package com.kianirani.jarvis.router.local

/**
 * LM1 — the on-device inference contract (PRD §, "llama.cpp NDK"). The Kotlin seam a native
 * (JNI/llama.cpp or MediaPipe LLM) engine implements: load a GGUF model file, generate text,
 * cancel, and unload. Keeping inference behind this interface lets the [LocalInferenceBackend]
 * compile and route today, with the native library dropped in later — and lets the app degrade
 * gracefully (never "speechless") when no engine is present via [NoOpLocalModelEngine].
 */
interface LocalModelEngine {

    /** True once a model file has been loaded into the native runtime. */
    val isLoaded: Boolean

    /** Load the GGUF at [modelPath]; returns false if the engine/native lib isn't available. */
    suspend fun load(modelPath: String): Boolean

    /**
     * Generate a completion for [prompt]. [maxTokens] bounds the output; [onToken] streams
     * tokens as they arrive (for a live HUD). Returns the full text, or failure if no model is
     * loaded / generation failed.
     */
    suspend fun generate(prompt: String, maxTokens: Int = 512, onToken: (String) -> Unit = {}): Result<String>

    /** Cancel an in-flight generation. */
    fun cancel()

    /** Free the loaded model and native resources (call on idle / low memory, LM3/DS-W6). */
    fun unload()
}

/**
 * LM6 graceful fallback — the default engine when no native library is bundled (this server
 * build, or a device where the model isn't downloaded). Always reports "not loaded" and fails
 * generation with a clear message so the router substitutes to cloud/mesh instead of crashing.
 */
object NoOpLocalModelEngine : LocalModelEngine {
    override val isLoaded: Boolean = false
    override suspend fun load(modelPath: String): Boolean = false
    override suspend fun generate(prompt: String, maxTokens: Int, onToken: (String) -> Unit): Result<String> =
        Result.failure(IllegalStateException("on-device model not available — using cloud/mesh"))
    override fun cancel() = Unit
    override fun unload() = Unit
}
