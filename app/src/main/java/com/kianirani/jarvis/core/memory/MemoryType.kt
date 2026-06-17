package com.kianirani.jarvis.core.memory

/**
 * CF4 — kinds of long-term memory Vision keeps about the user (PRD Part 4).
 * Persisted as the row `type` string; [fromName] parses it back safely.
 */
enum class MemoryType {
    FACT, // "My VPS IP is 1.2.3.4"
    PREFERENCE, // "User dislikes neumorphism"
    PERSON, // "Ali = developer friend"
    PROJECT, // "Vision OS project context"
    EPISODIC, // "Yesterday we discussed X"
    HABIT, // "User checks the phone at 8am"
    WORKING, // current conversation scratch (short-lived)
    ;

    companion object {
        fun fromName(name: String?): MemoryType =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FACT
    }
}
