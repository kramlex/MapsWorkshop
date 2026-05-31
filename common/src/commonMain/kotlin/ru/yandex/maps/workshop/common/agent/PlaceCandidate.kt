package ru.yandex.maps.workshop.common.agent

import com.yandex.mapkit.kmp.geometry.Point
import com.yandex.mapkit.kmp.geometry.PointFactory

data class PlaceCandidate(
    val id: String,
    val title: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
) {
    val position: Point get() = PointFactory.create(latitude, longitude)
}
