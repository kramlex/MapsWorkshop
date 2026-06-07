package ru.yandex.maps.workshop.common.agent.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import ru.yandex.maps.workshop.common.agent.AssistantApi

object CloseChatTool : AgentTool {

    override val spec = toolSpecs(
        name = TODO("Add unique tool name"),
        description = TODO("Add descriptions to explain when and why this tool should be called"),
    ) {
        // No arguments, don't change
        put("type", "object")
        putJsonObject("properties") {}
    }

    override suspend fun execute(arguments: JsonObject, api: AssistantApi): String {
        TODO("Parse arguments, call api.closeChat, return result as string")
    }
}
