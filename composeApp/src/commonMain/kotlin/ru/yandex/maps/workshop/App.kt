package ru.yandex.maps.workshop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yandex.mapkit.kmp.AnimationFactory
import com.yandex.mapkit.kmp.AnimationType
import com.yandex.mapkit.kmp.map.CameraListener
import com.yandex.mapkit.kmp.map.CameraPositionFactory
import com.yandex.mapkit.kmp.map.InputListener
import com.yandex.mapkit.kmp.map.MapObjectCollection
import com.yandex.mapkit.kmp.map.PlacemarkMapObject
import com.yandex.mapkit.kmp.map.geometry
import com.yandex.mapkit.kmp.map.map
import com.yandex.mapkit.kmp.map.mapObjects
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.yandex.maps.workshop.common.CommonApp
import ru.yandex.maps.workshop.common.additional.context.PlatformContext
import ru.yandex.maps.workshop.common.model.Placemark
import ru.yandex.maps.workshop.common.screen.LongTapEvent
import ru.yandex.maps.workshop.common.screen.PlacemarkViewState
import ru.yandex.maps.workshop.common.screen.SelectPlacemarkEvent
import ru.yandex.maps.workshop.internal.PinIconFactory
import ru.yandex.maps.workshop.internal.map.Map
import ru.yandex.maps.workshop.internal.map.NativeMap
import ru.yandex.maps.workshop.internal.map.MapState
import ru.yandex.maps.workshop.internal.mapkit.bindToLifecycleOwner
import ru.yandex.maps.workshop.internal.mapkit.rememberAndInitializeMapKit
import ru.yandex.maps.workshop.internal.view.PlacemarkPager

class MapScreenMutableState {
    val mapState by mutableStateOf(MapState())
    private var collection: MapObjectCollection? by mutableStateOf(null)
    private val placemarkObjects = mutableMapOf<String, PlacemarkMapObject>()

    fun collection(): MapObjectCollection = collection
        ?: (mapState.map!!.map.mapObjects.addCollection()).also { new ->
            collection = new
        }

    fun allPlacemarkObjects(): Set<String> = placemarkObjects.keys
    fun getPlacemarkObject(id: String): PlacemarkMapObject? = placemarkObjects[id]
    fun setPlacemarkObject(id: String, obj: PlacemarkMapObject) {
        placemarkObjects[id] = obj
    }

    fun removePlacemarkObject(id: String) {
        placemarkObjects[id]?.let { placemark ->
            collection().remove(placemark)
        }
        placemarkObjects.remove(id)
    }
}

@Composable
expect fun rememberPlatformContext(): PlatformContext

@Composable
@Preview
fun App() {
    val platformContext = rememberPlatformContext()
    val app = remember {
        CommonApp(
            iamToken = BuildKonfig.gptToken,
            folderId = BuildKonfig.folderId,
            context = platformContext,
        )
    }

    rememberAndInitializeMapKit(
        apiKey = BuildKonfig.mapkitToken
    ).bindToLifecycleOwner()

    val mapScreenMutableState = remember { MapScreenMutableState() }

    val viewModel = remember { app.createMainViewModel() }

    val state by viewModel.viewStates().collectAsState()
    val placemarks = state.placemarks

    MaterialTheme {
        Box {
            MapWithPlacemarks(
                mapScreenMutableState = mapScreenMutableState,
                placemarks = placemarks,
                selectedPlacemarkId = state.selectedPlacemarkId,
                onLongTap = { point ->
                    viewModel.dispatch(LongTapEvent(point))
                }
            )
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            ) {
                PlacemarkPager(
                    placemarks = state.placemarks,
                    selectedPlacemarkId = state.selectedPlacemarkId,
                    onCardSelected = { id ->
                        viewModel.dispatch(SelectPlacemarkEvent(id))
                    },
                    onCardClick = { id ->
                        mapScreenMutableState.moveToPlacemarkAnimated(placemarks, id)
                    },
                    onGenerateClick = {
                        //TODO
                    },
                )
            }
        }
    }
}

@Composable
fun MapWithPlacemarks(
    mapScreenMutableState: MapScreenMutableState,
    placemarks: List<PlacemarkViewState>,
    selectedPlacemarkId: String?,
    onLongTap: MapTapAction = {},
    onTap: MapTapAction = {},
) {
    val pinIconFactory = PinIconFactory.create()

    val listener = remember { TapListenerWrapper(onLongTap, onTap) }
    Map(
        state = mapScreenMutableState.mapState,
        onCreate = {
            it.map.addInputListener(listener)
        },
        onRelease = {
            it?.map?.removeInputListener(listener)
        }
    )

    LaunchedEffect(selectedPlacemarkId) {
        mapScreenMutableState.moveToPlacemarkAnimated(placemarks, selectedPlacemarkId)
    }

    LaunchedEffect(placemarks, selectedPlacemarkId) {
        val collection = mapScreenMutableState.collection()

        val actualIds = placemarks.map(PlacemarkViewState::id).toSet()
        val existingIds = mapScreenMutableState.allPlacemarkObjects()
        val toRemoveIds = (existingIds - actualIds)

        toRemoveIds.forEach(mapScreenMutableState::removePlacemarkObject)
        placemarks.forEach { placemarkModel ->
            val id = placemarkModel.id
            val obj = mapScreenMutableState.getPlacemarkObject(id)
            val isSelected = id == selectedPlacemarkId

            val (image, pinStyle) = pinIconFactory.iconAndStyleFor(
                iconId = placemarkModel.iconId,
                selected = isSelected
            )

            if (obj == null) {
                collection.addPlacemark().also { mapObject ->
                    mapObject.geometry = placemarkModel.position
                    mapObject.setIcon(image, pinStyle)
                    mapScreenMutableState.setPlacemarkObject(id, mapObject)
                }
            } else {
                if (obj.geometry != placemarkModel.position) obj.geometry = placemarkModel.position
                obj.setIcon(image, pinStyle)
            }
        }
    }
}

private fun MapScreenMutableState.moveToPlacemarkAnimated(
    placemarks: List<PlacemarkViewState>,
    selectedPlacemarkId: String?
) {
    val placemark = placemarks.find { it.id == selectedPlacemarkId } ?: return
    val mapWindow = mapState.map
    val map = mapWindow?.map ?: return

    map.move(
        CameraPositionFactory.create(
            target = placemark.position,
            zoom = 16f,
            azimuth = 0f,
            tilt = 0f,
        ),
        AnimationFactory.create(AnimationType.SMOOTH, 1f),
        null
    )
}
