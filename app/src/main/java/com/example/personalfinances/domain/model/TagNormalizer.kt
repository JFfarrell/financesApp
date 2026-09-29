package com.example.personalfinances.domain.model

/**
 * Normalises user-typed tag text into the canonical stored form: surrounding whitespace, commas
 * and a leading '#' are removed, the text is lowercased, and any inner whitespace becomes a
 * hyphen (so "Eating Out" is stored as "eating-out").
 *
 * Tags are compared by exact match, so every entry point should pass user input through this
 * first. Returns null when nothing is left after cleaning.
 */
fun normalizeTag(raw: String): String? =
    raw.trim().trim(',').trim().trimStart('#').trim()
        .lowercase()
        .replace(Regex("\\s+"), "-")
        .takeIf { it.isNotEmpty() }
