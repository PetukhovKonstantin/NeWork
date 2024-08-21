package ru.netology.nework.dto

sealed interface FeedItem {
    val id: Long
}

data class Post(
    override val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val published: String,
    val likedByMe: Boolean = false,
    val likeOwnerIds : List<Long>? = emptyList(),
    val coords: Coords? = null,
    val link:String? = null,
    val sharedByMe: Boolean = false,
    val countShared: Int = 999,
    val mentionIds: List<Long>? = emptyList(),
    val mentionedMe:Boolean = false,
    val attachment: Attachment? = null,
    val ownedByMe: Boolean = false,
) : FeedItem