package ru.yandex.maps.workshop.internal.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mapkit.kmp.map.MapWindow
import com.yandex.mapkit.kmp.mapview.toCommon
import com.yandex.mapkit.mapview.MapView

@Composable
actual fun NativeMap(
    state: MapState,
    onCreate: (MapWindow) -> Unit,
    onRelease: (MapWindow?) -> Unit
) {
    AndroidView(
        factory = { context ->
            MapView(context).also { mapView ->
                val mapWindow = mapView.toCommon().mapWindow.also { state.map = it }
                onCreate(mapWindow)
            }
        },
        update = {},
        onRelease = {
            onRelease(state.map)
            state.map = null
        }
    )
}
