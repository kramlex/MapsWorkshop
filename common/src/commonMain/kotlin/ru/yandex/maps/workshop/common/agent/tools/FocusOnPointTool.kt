package ru.yandex.maps.workshop.common.agent.tools

import com.yandex.mapkit.kmp.geometry.PointFactory
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import ru.yandex.maps.workshop.common.agent.AssistantApi

object FocusOnPointTool : AgentTool {

    override val spec = toolSpecs(
        name = "focus_on_point",
        description = "Move the map camera to center on a single geographic point.",
    ) {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("latitude") {
                put("type", "number")
                put("description", "Latitude of the point to focus on.")
            }
            putJsonObject("longitude") {
                put("type", "number")
                put("description", "Longitude of the point to focus on.")
            }
            putJsonObject("zoom") {
                put("type", "number")
                put("description", "Optional zoom level. When omitted the current zoom is kept.")
            }
        }
        putJsonArray("required") { add("latitude"); add("longitude") }
    }

    override suspend fun execute(arguments: JsonObject, api: AssistantApi): String {
        val latitude = arguments.requireDouble("latitude")
        val longitude = arguments.requireDouble("longitude")
        val zoom = arguments.optDouble("zoom")?.toFloat()

        api.focusOnPoint(PointFactory.create(latitude, longitude), zoom)

        return buildJsonObject {
            put("ok", true)
            put("latitude", latitude)
            put("longitude", longitude)
        }.toString()
    }
}
