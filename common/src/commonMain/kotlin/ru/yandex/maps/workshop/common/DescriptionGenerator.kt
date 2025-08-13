package ru.yandex.maps.workshop.common

import ru.yandex.maps.workshop.common.additional.llm.CompletionRequest
import ru.yandex.maps.workshop.common.additional.llm.Message
import ru.yandex.maps.workshop.common.additional.llm.YandexGPTClient
import ru.yandex.maps.workshop.common.additional.llm.completeText

fun interface DescriptionGenerator {

    suspend fun generateDescription(placemarkId: String): String?

    companion object {
        operator fun invoke(
            yandexGPTClient: YandexGPTClient,
            searchManager: SearchManager,
        ): DescriptionGenerator = DescriptionGenerator { placemarkId ->
            val request = """
            """.trimIndent()


            val completionRequest = CompletionRequest(
                messages = listOf(
                    Message(
                        role = "system",
                        text = "TODO",
                    ),

                    Message(
                        role = "user",
                        text = request
                    ),
                ),
            )

            yandexGPTClient.completeText(completionRequest) ?: error("Invalid response")
        }
    }
}
