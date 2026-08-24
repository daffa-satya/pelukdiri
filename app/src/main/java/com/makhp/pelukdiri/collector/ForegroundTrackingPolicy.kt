package com.makhp.pelukdiri.collector

object ForegroundTrackingPolicy {
    fun shouldTrack(
        resolvedPackage: String,
        ownPackage: String,
        monitoredPackages: Set<String>,
        excludedPackages: Set<String>,
    ): Boolean = resolvedPackage != ownPackage &&
        resolvedPackage in monitoredPackages &&
        resolvedPackage !in excludedPackages

    fun shouldRestart(
        resolvedPackage: String,
        currentPackage: String?,
        monitoredPackages: Set<String>,
        trackingJobActive: Boolean,
    ): Boolean = resolvedPackage == currentPackage &&
        resolvedPackage in monitoredPackages &&
        !trackingJobActive
}
