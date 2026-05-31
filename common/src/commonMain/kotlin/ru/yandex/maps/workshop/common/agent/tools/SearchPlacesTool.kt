package ru.yandex.maps.workshop.common.agent.tools

import com.yandex.mapkit.kmp.geometry.PointFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import ru.yandex.maps.workshop.common.agent.AssistantApi
import ru.yandex.maps.workshop.common.agent.PlaceCandidate

object SearchPlacesTool : AgentTool {

    override val spec = toolSpecs(
        name = "search_places",
        description = "Find places by a text query. Returns a list of candidates with coordinates and addresses.",
    ) {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "What to look for, e.g. 'coffee shop', 'park', 'pharmacy'.")
            }
            putJsonObject("latitude") {
                put("type", "number")
                put("description", "Latitude of the search center. Defaults to the current camera position.")
            }
            putJsonObject("longitude") {
                put("type", "number")
                put("description", "Longitude of the search center. Defaults to the current camera position.")
            }
            putJsonObject("radius_meters") {
                put("type", "number")
                put("description", "Search radius in meters. Optional.")
            }
        }
        putJsonArray("required") { add("query") }
    }

    override fun renderLabel(arguments: JsonObject): String {
        val query = arguments.optString("query").orEmpty()
        return "search_places(\"$query\")"
    }

    override suspend fun execute(arguments: JsonObject, api: AssistantApi): String {
        val query = arguments.requireString("query")
        val latitude = arguments.optDouble("latitude")
        val longitude = arguments.optDouble("longitude")
        val near = if (latitude != null && longitude != null) {
            PointFactory.create(latitude, longitude)
        } else null
        val radius = arguments.optDouble("radius_meters")

        val results = api.searchPlaces(query, near, radius)
        return json.encodeToString(
            ListSerializer(PlaceCandidateDto.serializer()),
            results.map { it.toDto() },
        )
    }
}

@Serializable
private data class PlaceCandidateDto(
    val id: String,
    val title: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
)

private fun PlaceCandidate.toDto() = PlaceCandidateDto(
    id = id,
    title = title,
    address = address,
    latitude = latitude,
    longitude = longitude,
    category = category,
)
