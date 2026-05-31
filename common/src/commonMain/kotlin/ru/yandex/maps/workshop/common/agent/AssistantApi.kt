package ru.yandex.maps.workshop.common.agent

import com.yandex.mapkit.kmp.geometry.Point
import com.yandex.mapkit.kmp.geometry.mpLatitude
import com.yandex.mapkit.kmp.geometry.mpLongitude
import com.yandex.mapkit.kmp.geometry.point
import com.yandex.mapkit.kmp.mpDescriptionText
import com.yandex.mapkit.kmp.mpGeometry
import com.yandex.mapkit.kmp.mpName
import ru.yandex.maps.workshop.common.SearchManager
import ru.yandex.maps.workshop.common.internal.IconId
import ru.yandex.maps.workshop.common.internal.PlacemarkRepository
import ru.yandex.maps.workshop.common.model.Placemark

/**
 * High-level capabilities the chat agent can invoke to interact with the map.
 *
 * This is the surface the agent toolset is built on: each tool ultimately maps to one of these
 * methods. It is intentionally small and side-effecting — searching the map and adding placemarks —
 * rather than exposing the underlying [SearchManager] or [PlacemarkRepository] directly.
 *
 * Constructed internally (see `CommonApp`); not meant to be instantiated by feature code.
 */
class AssistantApi internal constructor(
    private val searchManager: SearchManager,
    private val placemarkRepository: PlacemarkRepository,
    private val mapCameraController: MapCameraController,
) {

    /**
     * Searches for places matching [query] around a center point.
     *
     * @param query free-text search query (e.g. "coffee", "parks").
     * @param near the center to search around; defaults to the camera's current target, falling
     *  back to [Placemark.WorkshopPlacemark]'s position when the camera target is unknown.
     * @param radiusMeters search radius in meters; defaults to [DEFAULT_RADIUS_METERS].
     * @return matching places as [PlaceCandidate]s. Results without a point geometry or a
     *  non-blank name are dropped, so the list may be shorter than the raw search response.
     */
    suspend fun searchPlaces(
        query: String,
        near: Point? = null,
        radiusMeters: Double? = null,
    ): List<PlaceCandidate> {
        val center = near
            ?: mapCameraController.currentTarget.value
            ?: Placemark.WorkshopPlacemark.position
        val radius = radiusMeters ?: DEFAULT_RADIUS_METERS

        return searchManager.search(query, center, radius).mapNotNull { obj ->
            val point = obj.mpGeometry.firstNotNullOfOrNull { it.point } ?: return@mapNotNull null
            val title = obj.mpName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            PlaceCandidate(
                id = "yandex_${point.mpLatitude}_${point.mpLongitude}",
                title = title,
                address = obj.mpDescriptionText?.takeIf { it.isNotBlank() },
                latitude = point.mpLatitude,
                longitude = point.mpLongitude,
            )
        }
    }

    /**
     * Adds (or updates) a placemark on the map for each of the given [places].
     *
     * Placemark ids are derived from each candidate's id (prefixed with `agent_`), so calling this
     * again for the same candidates updates the existing placemarks rather than duplicating them.
     * The placemark title combines the candidate's title with its address when one is available.
     *
     * @param places the candidates to display, typically from [searchPlaces].
     * @return the placemark ids created or updated, in the same order as [places].
     */
    fun showOnMap(places: List<PlaceCandidate>): List<String> = places.map { candidate ->
        val placemarkId = "agent_${candidate.id}"
        val title = candidate.address
            ?.takeIf { it.isNotBlank() }
            ?.let { "${candidate.title} • $it" }
            ?: candidate.title
        placemarkRepository.addOrUpdatePlacemark(
            Placemark(
                id = placemarkId,
                latitude = candidate.latitude,
                longitude = candidate.longitude,
                title = title,
                iconId = IconId.randomNewPlacemarkIconId(),
            )
        )
        placemarkId
    }

    /**
     * Moves the camera to center on a single [point].
     *
     * @param point the geographic point to focus on.
     * @param zoom optional zoom level; when `null` the current zoom is kept.
     */
    fun focusOnPoint(point: Point, zoom: Float? = null) {
        mapCameraController.focusOnPoint(point, zoom)
    }

    /**
     * Moves the camera so that all the given [points] fit within the viewport.
     *
     * No-op when [points] is empty.
     *
     * @param points the geographic points to bring into view.
     */
    fun focusOnPoints(points: List<Point>) {
        mapCameraController.focusOnPoints(points)
    }

    private companion object {
        const val DEFAULT_RADIUS_METERS = 1500.0
    }
}
