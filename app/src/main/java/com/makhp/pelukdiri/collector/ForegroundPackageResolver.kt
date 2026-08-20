package com.makhp.pelukdiri.collector

object ForegroundPackageResolver {
    /**
     * Accessibility events can arrive after their source window has already lost focus.
     * The package owning the current accessibility root is therefore authoritative; the
     * event package is only a fallback while Android has no active root available.
     */
    fun resolve(
        eventPackage: String,
        activeWindowPackage: String?
    ): String = activeWindowPackage ?: eventPackage
}
