package ru.yandex.maps.workshop.common.agent.tools

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.yandex.maps.workshop.common.additional.llm.FunctionSpec
import ru.yandex.maps.workshop.common.additional.llm.ToolSpecs
import ru.yandex.maps.workshop.common.additional.llm.ToolCall
import ru.yandex.maps.workshop.common.agent.AssistantApi

interface AgentTool {
    val spec: ToolSpecs
    fun renderLabel(arguments: JsonObject): String
    suspend fun execute(arguments: JsonObject, api: AssistantApi): String
}

class AgentToolset(
    private val assistantApi: AssistantApi,
) {

    private val tools: List<AgentTool> = listOf(
        SearchPlacesTool,
    )

    private val byName: Map<String, AgentTool> = tools.associateBy { it.spec.function.name }

    val specs: List<ToolSpecs> = tools.map { it.spec }

    fun renderLabel(call: ToolCall): String {
        val tool = byName[call.function.name] ?: return "${call.function.name}: unknown tool"
        val args = parseArgs(call.function.arguments) ?: return "${call.function.name}: bad arguments"
        return tool.renderLabel(args)
    }

    suspend fun dispatch(call: ToolCall): String {
        val tool = byName[call.function.name] ?: return jsonError("unknown tool: ${call.function.name}")
        return try {
            val args = parseArgs(call.function.arguments) ?: return jsonError("bad arguments")
            tool.execute(args, assistantApi)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            jsonError("internal tool error: ${t.message}")
        }
    }

    private fun parseArgs(arguments: String): JsonObject? = try {
        json.parseToJsonElement(arguments).jsonObject
    } catch (_: Throwable) {
        null
    }
}

internal val json: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = false
}

fun toolSpecs(
    name: String,
    description: String,
    parameters: JsonObjectBuilder.() -> Unit,
): ToolSpecs = ToolSpecs(
    function = FunctionSpec(
        name = name,
        description = description,
        parameters = buildJsonObject(parameters),
    ),
)

fun jsonError(message: String): String =
    buildJsonObject { put("error", message) }.toString()

fun JsonObject.requireString(key: String): String =
    this[key]?.jsonPrimitive?.content
        ?: throw IllegalArgumentException("missing required argument: $key")

fun JsonObject.optString(key: String): String? =
    this[key]?.jsonPrimitive?.content

fun JsonObject.optDouble(key: String): Double? =
    this[key]?.jsonPrimitive?.doubleOrNull
