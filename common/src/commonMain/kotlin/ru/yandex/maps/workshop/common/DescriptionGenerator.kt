package ru.yandex.maps.workshop.common

import ru.yandex.maps.workshop.common.additional.llm.YandexGPTClient

fun interface DescriptionGenerator {

    suspend fun generateDescription(placemarkId: String): String?

    companion object {
        operator fun invoke(
            yandexGPTClient: YandexGPTClient,
            // todo
        ): DescriptionGenerator = DescriptionGenerator { placemarkId ->
            TODO()
        }
    }
}
