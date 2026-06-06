package ru.yandex.maps.workshop.common.agent.tools

import com.yandex.mapkit.kmp.geometry.PointFactory
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import ru.yandex.maps.workshop.common.agent.AssistantApi

object FocusOnPointsTool : AgentTool {

    override val spec = toolSpecs(
        name = "focus_on_points",
        description = "Move the map camera so that all the given points fit within the viewport. Useful after showing several places.",
    ) {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("points") {
                put("type", "array")
                put("description", "Points to bring into view.")
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("latitude") { put("type", "number") }
                        putJsonObject("longitude") { put("type", "number") }
                    }
                    putJsonArray("required") { add("latitude"); add("longitude") }
                }
            }
        }
        putJsonArray("required") { add("points") }
    }

    override suspend fun execute(arguments: JsonObject, api: AssistantApi): String {
        val pointsElement = arguments["points"]
            ?: throw IllegalArgumentException("missing required argument: points")
        val points = pointsElement.jsonArray.map { element ->
            val obj = element.jsonObject
            PointFactory.create(
                obj.requireDouble("latitude"),
                obj.requireDouble("longitude"),
            )
        }

        api.focusOnPoints(points)

        return buildJsonObject {
            put("ok", true)
            put("count", points.size)
        }.toString()
    }
}
