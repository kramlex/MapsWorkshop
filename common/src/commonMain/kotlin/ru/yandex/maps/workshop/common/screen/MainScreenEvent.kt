package ru.yandex.maps.workshop.common.screen

import ru.yandex.maps.workshop.common.additional.udf.Event

interface MainScreenEvent : Event
data class GeneratePlacemarkDescriptionEvent(val id: String) : MainScreenEvent, BaseMainScreenEvent

data class SelectPlacemarkEvent(val id: String) : MainScreenEvent
