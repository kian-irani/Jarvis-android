package com.kianirani.jarvis.router.local

import com.kianirani.jarvis.router.registry.ModelSeed

/**
 * VISION BRAIN V1 — Module 17 (Local AI Engine / LM2): the on-demand model catalogue.
 *
 * The on-device model is downloaded after install (not bundled in the APK), with a
 * pinned SHA-256 and resume support — the same fail-closed pattern as the MiniLM
 * embedder. Two options:
 *  - **Qwen2.5-0.5B-Instruct Q4** — primary. ~0.4 GB, Apache-2.0 (clean licence for
 *    in-app distribution); its id matches [ModelSeed.LOCAL_DEFAULT_ID] so the registry's
 *    seeded local model becomes "real" once downloaded.
 *  - **Gemma 3 1B Q4** — optional quality tier (~0.7 GB).
 *
 * NOTE: [sha256] must be the real hash of the pinned release artifact before the
 * download is offered in the Setup Wizard (B.5). Until pinned it stays fail-closed —
 * a mismatch refuses the file rather than running an unverified model.
 */
data class LocalModelDescriptor(
    val id: String,
    val displayName: String,
    val url: String,
    /** Pinned SHA-256 (lowercase hex) of the GGUF artifact. */
    val sha256: String,
    val sizeBytes: Long,
) {
    /** Where the file lands inside the models dir. */
    val fileName: String get() = "$id.gguf"
}

object LocalModelCatalog {

    /** Default lightweight model — id aligned with the registry's seeded local spec. */
    val QWEN_0_5B = LocalModelDescriptor(
        id = ModelSeed.LOCAL_DEFAULT_ID, // "qwen2.5-0.5b-instruct-q4"
        displayName = "Vision Local (Qwen2.5 0.5B Q4)",
        url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
        sha256 = "", // TODO: pin the real release hash before offering the download (fail-closed until then).
        sizeBytes = 400_000_000L,
    )

    /** Optional higher-quality tier. */
    val GEMMA_1B = LocalModelDescriptor(
        id = "gemma-3-1b-q4",
        displayName = "Vision Local (Gemma 3 1B Q4)",
        url = "https://huggingface.co/google/gemma-3-1b-it-qat-q4_0-gguf/resolve/main/gemma-3-1b-it-q4_0.gguf",
        sha256 = "",
        sizeBytes = 720_000_000L,
    )

    val all: List<LocalModelDescriptor> = listOf(QWEN_0_5B, GEMMA_1B)

    /** The model Vision downloads by default. */
    val default: LocalModelDescriptor = QWEN_0_5B
}
