package ru.yandex.maps.workshop.common.model

import com.yandex.mapkit.kmp.geometry.Point
import com.yandex.mapkit.kmp.geometry.PointFactory
import com.yandex.mapkit.kmp.geometry.mpLatitude
import com.yandex.mapkit.kmp.geometry.mpLongitude
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.yandex.maps.workshop.common.internal.IconId

@Serializable
data class Placemark(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String? = null,
    val iconId: IconId = IconId.DefaultIcon1
) {

    constructor(
        id: String,
        position: Point,
        title: String,
        description: String? = null
    ) : this(id, position.mpLatitude, position.mpLongitude, title, description)

    @Transient
    val position: Point = PointFactory.create(latitude, longitude)

    companion object {
        val WorkshopPlacemark = Placemark(
            id = "workshop",
            latitude = 55.733715,
            longitude = 37.587738,
            title = "Workshop",
        )
    }
}


