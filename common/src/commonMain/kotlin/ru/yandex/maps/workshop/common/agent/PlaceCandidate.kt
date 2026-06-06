package ru.yandex.maps.workshop.common.agent

import kotlinx.serialization.Serializable

@Serializable
data class PlaceCandidate(
    val id: String,
    val title: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
)
