package ru.yandex.maps.workshop.common.screen

import ru.yandex.maps.workshop.common.DescriptionGenerator
import ru.yandex.maps.workshop.common.additional.udf.Reducer
import ru.yandex.maps.workshop.common.additional.udf.ViewModel

private val ScreenReducer: Reducer<MainScreenViewModel.State> get() = { state, event ->
    when (event) {
        is BaseMainScreenEvent -> event.reduce(state)

        // todo

        else -> state
    }
}

class MainScreenViewModel internal constructor(
    descriptionGenerator: DescriptionGenerator
): ViewModel<MainScreenViewModel.State>(
    initialState = State.Initial,
    reducer = ScreenReducer
) {

    data class State(
        val selectedPlacemarkId: String?,
    ) {
        companion object {
            val Initial = State(
                selectedPlacemarkId = null,
            )
        }
    }
}
