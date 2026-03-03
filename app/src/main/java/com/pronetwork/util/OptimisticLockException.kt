package com.pronetwork.util

/**
 * Thrown when an update fails due to a version mismatch.
 * This means another device has modified the same record.
 *
 * Repositories throw this → ViewModels catch it → UI notifies the user.
 */
class OptimisticLockException(
    val entityType: String,
    val entityId: String,
    message: String = "$entityType $entityId was modified by another device"
) : Exception(message)
