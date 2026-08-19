package com.makhp.pelukdiri.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterventionLockManager @Inject constructor() {
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    /**
     * Attempts to acquire the lock. 
     * Returns true if successful, false if already locked.
     */
    fun acquireLock(): Boolean {
        synchronized(this) {
            if (_isLocked.value) return false
            _isLocked.value = true
            return true
        }
    }

    /**
     * Releases the lock.
     */
    fun releaseLock() {
        synchronized(this) {
            _isLocked.value = false
        }
    }
}
