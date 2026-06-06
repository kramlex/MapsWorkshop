package ru.yandex.maps.workshop.common.agent.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import ru.yandex.maps.workshop.common.agent.AssistantApi

object CloseChatTool : AgentTool {

    override val spec = toolSpecs(
        name = "close_chat",
        description = "Close the chat panel once you are done interacting with the map. " +
            "Call this after you have shown places and/or moved the camera so the user can see " +
            "the result on the full map without the chat covering it",
    ) {
        // No arguments, don't change
        put("type", "object")
        putJsonObject("properties") {}
    }

    override suspend fun execute(arguments: JsonObject, api: AssistantApi): String {
        api.closeChat()
        return buildJsonObject {
            put("ok", true)
            put("closed", true)
        }.toString()
    }
}
