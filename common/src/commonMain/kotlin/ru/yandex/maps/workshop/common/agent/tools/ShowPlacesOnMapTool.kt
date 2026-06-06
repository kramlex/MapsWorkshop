package ru.yandex.maps.workshop.common.agent.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import ru.yandex.maps.workshop.common.agent.AssistantApi
import ru.yandex.maps.workshop.common.agent.PlaceCandidate

object ShowPlacesOnMapTool : AgentTool {

    override val spec = toolSpecs(
        name = "show_places_on_map",
        description = "Show the given places on the map as placemarks. Pass places returned by search_places. Returns the created placemark ids.",
    ) {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("places") {
                put("type", "array")
                put("description", "Places to display, typically taken from search_places results.")
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("id") {
                            put("type", "string")
                            put("description", "Stable place id, e.g. the one returned by search_places.")
                        }
                        putJsonObject("title") {
                            put("type", "string")
                            put("description", "Display name of the place.")
                        }
                        putJsonObject("address") {
                            put("type", "string")
                            put("description", "Human-readable address. Optional.")
                        }
                        putJsonObject("latitude") { put("type", "number") }
                        putJsonObject("longitude") { put("type", "number") }
                    }
                    putJsonArray("required") {
                        add("id"); add("title"); add("latitude"); add("longitude")
                    }
                }
            }
        }
        putJsonArray("required") { add("places") }
    }

    override suspend fun execute(arguments: JsonObject, api: AssistantApi): String {
        val placesElement = arguments["places"]
            ?: throw IllegalArgumentException("missing required argument: places")
        val places = json.decodeFromJsonElement<List<PlaceCandidate>>(placesElement)

        val placemarkIds = api.showOnMap(places)
        return json.encodeToString(placemarkIds)
    }
}
