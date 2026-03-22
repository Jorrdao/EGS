package com.sos.messaging.model

import kotlinx.serialization.Serializable

@Serializable
data class Presence(
    val userId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long
)