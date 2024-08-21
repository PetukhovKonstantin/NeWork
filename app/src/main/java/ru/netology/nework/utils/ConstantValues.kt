package ru.netology.nework.utils

import ru.netology.nework.dto.*
import ru.netology.nework.models.MediaModel

object ConstantValues {
    const val POST_CONTENT = "content"
    const val POST_LINK = "link"
    const val POST_MENTIONS_COUNT = "count mentions in post"
    const val EVENT_ID = "event id"
    const val EVENT_REQUEST_TYPE = "audience or speakers"
    const val USER_ID = "user id"
    val emptyUser = User(
        id = -1,
        login = "",
        name = "",
        avatar = null
    )
    val emptyJob = Job(
        id = 0,
        name = "",
        position = "",
        start = "",
        finish = "",
        link = "",
        ownerId = -1L,
    )
    val emptyPost = Post(
        id = 0,
        authorId = 0,
        content = "",
        author = "",
        likeOwnerIds = emptyList(),
        countShared = 0,
        mentionIds = emptyList(),
        published = ""
    )
    val emptyEvent = EventResponse(
        id = 0,
        authorId = 0,
        content = "",
        author = "",
        likeOwnerIds = emptyList(),
        datetime = "",
        type = EventType.OFFLINE,
        published = "",
    )
    val noPhoto = MediaModel()
}