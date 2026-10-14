package com.n2.oemlauncher.wear.data

import kotlinx.serialization.Serializable

@Serializable
data class UsageRecord(val launchCount: Int = 0, val lastLaunchedMillis: Long = 0)

typealias UsageStats = Map<String, UsageRecord>
