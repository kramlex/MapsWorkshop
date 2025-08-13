package ru.yandex.maps.workshop.common

interface SearchManager {

    suspend fun search(
        // todo
    ): Unit

    companion object {
        operator fun invoke(
            // todo
        ) : SearchManager = object : SearchManager {
            override suspend fun search(
                // todo
            ) {
                TODO()
            }
        }
    }
}
