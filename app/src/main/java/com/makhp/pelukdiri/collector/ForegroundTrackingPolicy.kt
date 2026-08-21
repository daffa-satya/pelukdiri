package com.makhp.pelukdiri.collector

object ForegroundTrackingPolicy {
    fun shouldRestart(
        resolvedPackage: String,
        currentPackage: String?,
        monitoredPackages: Set<String>,
        trackingJobActive: Boolean,
    ): Boolean = resolvedPackage == currentPackage &&
        resolvedPackage in monitoredPackages &&
        !trackingJobActive
}
