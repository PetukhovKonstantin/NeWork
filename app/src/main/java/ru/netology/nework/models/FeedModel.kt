package ru.netology.nework.models

import ru.netology.nework.dto.Post

data class FeedModel(
    val posts: List<Post> = emptyList(),
    val empty: Boolean = false,
)

sealed interface FeedModelState {
    object Loading : FeedModelState
    object Error : FeedModelState
    object Refresh : FeedModelState
    object Idle : FeedModelState
    object ShadowIdle : FeedModelState
}
