package ru.yandex.maps.workshop.common

import com.yandex.mapkit.kmp.GeoObject
import com.yandex.mapkit.kmp.geometry.Point
import kotlinx.coroutines.suspendCancellableCoroutine

interface SearchManager {

    suspend fun search(
        query: String,
        center: Point,
        radius: Double,
    ): List<GeoObject>

    companion object {
        operator fun invoke(
            searchManager: com.yandex.mapkit.search.kmp.SearchManager
        ) : SearchManager = object : SearchManager {
            override suspend fun search(
                query: String,
                center: Point,
                radius: Double,
            ): List<GeoObject> = suspendCancellableCoroutine { continuation ->
                continuation.resumeWith(Result.success(emptyList()))
            }
        }
    }
}
