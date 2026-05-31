package ru.yandex.maps.workshop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yandex.mapkit.kmp.AnimationFactory
import com.yandex.mapkit.kmp.AnimationType
import com.yandex.mapkit.kmp.geometry.BoundingBoxFactory
import com.yandex.mapkit.kmp.geometry.Geometry
import com.yandex.mapkit.kmp.geometry.GeometryFromBoundingBox
import com.yandex.mapkit.kmp.geometry.Point
import com.yandex.mapkit.kmp.geometry.PointFactory
import com.yandex.mapkit.kmp.geometry.mpLatitude
import com.yandex.mapkit.kmp.geometry.mpLongitude
import com.yandex.mapkit.kmp.map.CameraPositionFactory
import com.yandex.mapkit.kmp.map.MapObjectCollection
import com.yandex.mapkit.kmp.map.PlacemarkMapObject
import com.yandex.mapkit.kmp.map.geometry
import com.yandex.mapkit.kmp.map.map
import com.yandex.mapkit.kmp.map.mapObjects
import com.yandex.mapkit.kmp.map.mpTarget
import com.yandex.mapkit.kmp.map.userData
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.yandex.maps.workshop.common.CommonApp
import ru.yandex.maps.workshop.common.additional.context.PlatformContext
import ru.yandex.maps.workshop.common.agent.CameraCommand
import ru.yandex.maps.workshop.common.chat.ChatViewModel
import ru.yandex.maps.workshop.common.screen.GeneratePlacemarkDescriptionEvent
import ru.yandex.maps.workshop.common.screen.LongTapEvent
import ru.yandex.maps.workshop.common.screen.PlacemarkViewState
import ru.yandex.maps.workshop.common.screen.SelectPlacemarkEvent
import ru.yandex.maps.workshop.internal.PinIconFactory
import ru.yandex.maps.workshop.internal.map.Map
import ru.yandex.maps.workshop.internal.map.MapState
import ru.yandex.maps.workshop.internal.mapkit.bindToLifecycleOwner
import ru.yandex.maps.workshop.internal.mapkit.rememberAndInitializeMapKit
import ru.yandex.maps.workshop.internal.view.ChatScreen
import ru.yandex.maps.workshop.internal.view.ChatSummaryBar
import ru.yandex.maps.workshop.internal.view.PlacemarkPager

class MapScreenMutableState(
    private val onPlacemarkTap: (String) -> Unit
) {

    val mapState by mutableStateOf(MapState())
    private var collection: MapObjectCollection? by mutableStateOf(null)
    private val placemarkObjects = mutableMapOf<String, PlacemarkMapObject>()

    fun collection(): MapObjectCollection = collection
        ?: (mapState.map!!.map.mapObjects.addCollection()).also { new ->
            collection = new
        }

    private val listener = ObjectTapListenerWrapper { mapObject ->
        val id = mapObject.userData as? String ?: return@ObjectTapListenerWrapper
        onPlacemarkTap(id)
    }

    fun allPlacemarkObjects(): Set<String> = placemarkObjects.keys
    fun getPlacemarkObject(id: String): PlacemarkMapObject? = placemarkObjects[id]
    fun setPlacemarkObject(id: String, obj: PlacemarkMapObject) {
        obj.userData = id
        obj.addTapListener(listener)
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
            openAiApiKey = BuildKonfig.openAiApiKey,
            openAiModel = BuildKonfig.openAiModel,
            openAiBaseUrl = BuildKonfig.openAiBaseUrl,
            context = platformContext,
        )
    }

    rememberAndInitializeMapKit(
        apiKey = BuildKonfig.mapkitToken
    ).bindToLifecycleOwner()

    val viewModel = remember { app.createMainViewModel() }
    val chatViewModel = remember { app.createChatViewModel() }

    val mapScreenMutableState = remember {
        MapScreenMutableState(
            onPlacemarkTap = { viewModel.dispatch(SelectPlacemarkEvent(it) )}
        )
    }

    val state by viewModel.viewStates().collectAsState()
    val placemarks = state.placemarks

    LaunchedEffect(app.mapCameraController) {
        app.mapCameraController.commands.collect { command ->
            when (command) {
                is CameraCommand.FocusPoint ->
                    mapScreenMutableState.moveToPointAnimated(command.point, command.zoom)
                is CameraCommand.FocusBounds ->
                    mapScreenMutableState.moveToBoundsAnimated(command.points)
            }
        }
    }

    MaterialTheme {
        Box {
            MapWithPlacemarks(
                mapScreenMutableState = mapScreenMutableState,
                placemarks = placemarks,
                selectedPlacemarkId = state.selectedPlacemarkId,
                onLongTap = { point ->
                    viewModel.dispatch(LongTapEvent(point))
                },
                onCameraMoved = { cameraPosition, _ ->
                    app.mapCameraController.notifyTarget(cameraPosition.mpTarget)
                },
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
                    onGenerateClick = { id ->
                        viewModel.dispatch(GeneratePlacemarkDescriptionEvent(id))
                    }
                )
            }

            ChatOverlay(
                viewModel = chatViewModel,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatOverlay(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    var isChatOpen by remember { mutableStateOf(false) }
    val chatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chatState by viewModel.viewStates().collectAsState()

    AnimatedVisibility(!isChatOpen) {
        ChatSummaryBar(
            state = chatState,
            onClick = { isChatOpen = true },
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    if (isChatOpen) {
        ModalBottomSheet(
            onDismissRequest = { isChatOpen = false },
            sheetState = chatSheetState,
        ) {
            ChatScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth(),
            )
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
    onCameraMoved: MapMoveAction = { _, _ -> },
) {
    val pinIconFactory = PinIconFactory.create()

    val tapListener = remember { TapListenerWrapper(onLongTap, onTap) }
    val cameraListener = remember { CameraListenerWrapper(onCameraMoved) }
    Map(
        state = mapScreenMutableState.mapState,
        onCreate = {
            it.map.addInputListener(tapListener)
            it.map.addCameraListener(cameraListener)
        },
        onRelease = {
            it?.map?.removeInputListener(tapListener)
            it?.map?.removeCameraListener(cameraListener)
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
    moveToPointAnimated(placemark.position, zoom = 16f)
}

private fun MapScreenMutableState.moveToPointAnimated(point: Point, zoom: Float? = null) {
    val map = mapState.map?.map ?: return
    map.move(
        CameraPositionFactory.create(
            target = point,
            zoom = zoom ?: 16f,
            azimuth = 0f,
            tilt = 0f,
        ),
        AnimationFactory.create(AnimationType.SMOOTH, 1f),
        null
    )
}

private fun MapScreenMutableState.moveToBoundsAnimated(points: List<Point>) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        moveToPointAnimated(points.first(), zoom = 16f)
        return
    }
    val map = mapState.map?.map ?: return
    map.move(
        map.cameraPosition(boundingBoxOf(points)),
        AnimationFactory.create(AnimationType.SMOOTH, 1f),
        null
    )
}

private fun boundingBoxOf(points: List<Point>): Geometry {
    var minLat = Double.POSITIVE_INFINITY
    var maxLat = Double.NEGATIVE_INFINITY
    var minLon = Double.POSITIVE_INFINITY
    var maxLon = Double.NEGATIVE_INFINITY
    for (p in points) {
        if (p.mpLatitude < minLat) minLat = p.mpLatitude
        if (p.mpLatitude > maxLat) maxLat = p.mpLatitude
        if (p.mpLongitude < minLon) minLon = p.mpLongitude
        if (p.mpLongitude > maxLon) maxLon = p.mpLongitude
    }
    val latPad = (maxLat - minLat) * 0.15 + 0.0005
    val lonPad = (maxLon - minLon) * 0.15 + 0.0005
    return GeometryFromBoundingBox(
        BoundingBoxFactory.create(
            PointFactory.create(minLat - latPad, minLon - lonPad),
            PointFactory.create(maxLat + latPad, maxLon + lonPad),
        )
    )
}
