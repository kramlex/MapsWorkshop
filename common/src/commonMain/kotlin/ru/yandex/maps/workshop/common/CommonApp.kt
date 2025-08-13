package ru.yandex.maps.workshop.common

import ru.yandex.maps.workshop.common.additional.context.PlatformContext
import ru.yandex.maps.workshop.common.additional.context.observableSettingsFactory
import ru.yandex.maps.workshop.common.additional.llm.YandexGPTClient
import ru.yandex.maps.workshop.common.additional.settings.ObservableSettingsFactory
import ru.yandex.maps.workshop.common.screen.MainScreenViewModel

class CommonApp(
    folderId: String,
    iamToken: String,
    context: PlatformContext
) {

    private val settingsFactory: ObservableSettingsFactory by lazy {
        context.observableSettingsFactory()
    }

    private val gptClient: YandexGPTClient by lazy {
        YandexGPTClient(
            folderId = folderId,
            iamToken = iamToken,
        )
    }

    private val descriptionGenerator: DescriptionGenerator by lazy {
        DescriptionGenerator(
            yandexGPTClient = gptClient
        )
    }

    fun createMainViewModel() = MainScreenViewModel(
        descriptionGenerator = descriptionGenerator
    )
}
