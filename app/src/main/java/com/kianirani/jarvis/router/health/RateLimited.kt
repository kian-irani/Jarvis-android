package com.kianirani.jarvis.router.health

/**
 * VISION BRAIN V1 (VB4) — a backend's signal that it was rate-limited (HTTP 429).
 *
 * Carrying the server's `Retry-After` lets the [AvailabilityGraph] pin the model's
 * cooldown to exactly the window the provider asked for, instead of guessing with the
 * exponential backoff used for generic errors. A [Backend][com.kianirani.jarvis.router.backend.Backend]
 * may fail a [Result] with this so the router cools the right model for the right time.
 */
class RateLimited(
    val retryAfterMs: Long,
    message: String = "RATE_LIMITED — retry after ${retryAfterMs}ms",
) : Exception(message)
