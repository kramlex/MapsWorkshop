package ru.yandex.maps.workshop.common

import com.yandex.mapkit.search.kmp.SearchFactory
import com.yandex.mapkit.search.kmp.SearchManagerType
import ru.yandex.maps.workshop.common.additional.context.PlatformContext
import ru.yandex.maps.workshop.common.additional.context.observableSettingsFactory
import ru.yandex.maps.workshop.common.additional.llm.OpenAIClient
import ru.yandex.maps.workshop.common.agent.AssistantApi
import ru.yandex.maps.workshop.common.agent.MapCameraController
import ru.yandex.maps.workshop.common.chat.ChatRepository
import ru.yandex.maps.workshop.common.chat.ChatViewModel
import ru.yandex.maps.workshop.common.internal.PlacemarkRepository
import ru.yandex.maps.workshop.common.screen.MainScreenViewModel

class CommonApp(
    openAiApiKey: String,
    openAiModel: String,
    openAiBaseUrl: String,
    context: PlatformContext
) {

    fun createMainViewModel() = MainScreenViewModel(
        descriptionGenerator = descriptionGenerator,
        placemarkRepository = placemarkRepository
    )

    fun createChatViewModel() = ChatViewModel(
        repository = chatRepository,
    )


    val mapCameraController: MapCameraController = MapCameraController()

    // ======= Private

    private val assistantApi: AssistantApi by lazy {
        AssistantApi(
            searchManager = searchManager,
            placemarkRepository = placemarkRepository,
            mapCameraController = mapCameraController,
        )
    }

    private val openAIClient: OpenAIClient by lazy {
        OpenAIClient(
            apiKey = openAiApiKey,
            model = openAiModel,
            baseUrl = openAiBaseUrl,
        )
    }

    private val chatRepository: ChatRepository by lazy { ChatRepository() }

    private val searchManager: SearchManager by lazy {
        SearchManager(
            searchManager = SearchFactory.instance.createSearchManager(SearchManagerType.ONLINE),
        )
    }

    private val placemarkRepository: PlacemarkRepository by lazy {
        PlacemarkRepository(
            factory = context.observableSettingsFactory()
        )
    }

    private val descriptionGenerator by lazy {
        DescriptionGenerator(
            openAIClient = openAIClient,
            searchManager = searchManager,
            placemarkRepository = placemarkRepository
        )
    }

}
