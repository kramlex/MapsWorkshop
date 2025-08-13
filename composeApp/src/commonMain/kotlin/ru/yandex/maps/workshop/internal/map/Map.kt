package ru.yandex.maps.workshop.internal.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.yandex.mapkit.kmp.ScreenPointFactory
import com.yandex.mapkit.kmp.geometry.Point
import com.yandex.mapkit.kmp.map.MapWindow
import ru.yandex.maps.workshop.MapMoveAction
import ru.yandex.maps.workshop.MapTapAction

class MapState(val coordinates: Point? = null) {
    var map by mutableStateOf<MapWindow?>(null)

    companion object {
        val Saver: Saver<MapState, Point> = Saver(
            save = { it.coordinates },
            restore = { MapState(it) },
        )
    }

    fun screenToWorld(x: Float, y: Float): Point? {
        return map?.screenToWorld(ScreenPointFactory.create(x, y))
    }
}

@Composable
fun Map(
    state: MapState = rememberMapState(),
    onCreate: (MapWindow) -> Unit = {},
    onRelease: (MapWindow?) -> Unit = {}
) {
    NativeMap(
        state = state,
        onCreate = onCreate,
        onRelease = onRelease,
    )
}

@Composable
expect fun NativeMap(
    state: MapState = rememberMapState(),
    onCreate: (MapWindow) -> Unit,
    onRelease: (MapWindow?) -> Unit
)

@Composable
inline fun rememberMapState(
    key: String? = null,
    crossinline init: MapState.() -> Unit = {}
): MapState = rememberSaveable(key = key, saver = MapState.Saver) {
    MapState().apply(init)
}
