package ru.yandex.maps.workshop.common.agent

import com.yandex.mapkit.kmp.geometry.Point
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CameraCommand {
    data class FocusPoint(val point: Point, val zoom: Float? = null) : CameraCommand
    data class FocusBounds(val points: List<Point>) : CameraCommand
}

class MapCameraController {

    private val _commands = MutableSharedFlow<CameraCommand>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val commands: SharedFlow<CameraCommand> = _commands.asSharedFlow()

    private val _currentTarget = MutableStateFlow<Point?>(null)
    val currentTarget: StateFlow<Point?> = _currentTarget.asStateFlow()

    fun focusOnPoint(point: Point, zoom: Float? = null) {
        _commands.tryEmit(CameraCommand.FocusPoint(point, zoom))
    }

    fun focusOnPoints(points: List<Point>) {
        if (points.isEmpty()) return
        _commands.tryEmit(CameraCommand.FocusBounds(points))
    }

    fun notifyTarget(point: Point) {
        _currentTarget.value = point
    }
}
