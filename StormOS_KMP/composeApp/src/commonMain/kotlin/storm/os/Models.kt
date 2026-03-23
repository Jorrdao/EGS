package storm.os

import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceItem(
    val name: String,
    val description: String,
    val price: Double,
    val address: String,
    val contact_info: String,
    val latitude: Double,
    val longitude: Double
)