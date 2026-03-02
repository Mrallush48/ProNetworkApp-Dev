package com.pronetwork.util

import com.github.f4b6a3.uuid.UuidCreator

/**
 * Generates a globally unique, time-ordered ID (UUIDv7 - RFC 9562).
 *
 * Properties:
 * - Globally unique across all devices (no collisions)
 * - Time-ordered (preserves B-tree index performance in SQLite/PostgreSQL)
 * - Uses SecureRandom for the random portion
 *
 * @return UUIDv7 as String (e.g., "01912b6c-5c80-7f00-a123-456789abcdef")
 */
fun generateId(): String = UuidCreator.getTimeOrderedEpoch().toString()
