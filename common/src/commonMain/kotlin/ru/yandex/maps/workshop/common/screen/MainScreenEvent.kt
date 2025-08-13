package ru.yandex.maps.workshop.common.screen

import ru.yandex.maps.workshop.common.additional.udf.Event

// ========= Public

interface MainScreenEvent : Event

// ========= Internal

internal sealed interface BaseMainScreenEvent : MainScreenEvent

internal fun BaseMainScreenEvent.reduce(
    state: MainScreenViewModel.State
): MainScreenViewModel.State = when (this) {
    else -> state
}
