package ru.netology.nework.models

import ru.netology.nework.dto.Post

data class EventsModel(
    val posts: List<Post> = emptyList(),
    val empty: Boolean = false,
)

sealed interface EventsModelState {
    object Loading : EventsModelState
    object Error : EventsModelState
    object Refresh : EventsModelState
    object Idle : EventsModelState
    object ShadowIdle : EventsModelState
}
