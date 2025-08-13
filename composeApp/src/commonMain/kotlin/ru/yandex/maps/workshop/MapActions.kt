package ru.yandex.maps.workshop

import com.yandex.mapkit.kmp.geometry.Point
import com.yandex.mapkit.kmp.map.CameraPosition
import com.yandex.mapkit.kmp.map.MapObject
import com.yandex.mapkit.kmp.map.MapObjectTapListener

typealias MapObjectTapAction = (MapObject) -> Unit
typealias MapTapAction = (Point) -> Unit
typealias MapMoveAction = (cameraPosition: CameraPosition, finished: Boolean) -> Unit

class CameraListenerWrapper(private val onCameraMoved: MapMoveAction) {
    // TODO
}

class LongTapListenerWrapper(private val longTapListener: MapTapAction) {
    // TODO
}


class ObjectTapListenerWrapper(private val tapAction: MapObjectTapAction): MapObjectTapListener {
    override fun onMapObjectTap(
        mapObject: MapObject,
        point: Point
    ): Boolean {
        // TODO
        return true
    }
}
