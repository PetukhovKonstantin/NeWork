package ru.netology.nework.dto

data class PostResponse(
    val id:	Long,
    val authorId:Long,
    val author:String,
    val authorAvatar:String? = null,
    val authorJob:String? = null,
    val content:String,
    val published:String,
    val coords: Coords? = null,
    val link:String? = null,
    val likeOwnerIds: List<Long> = emptyList(),
    val mentionIds: List<Long> = emptyList(),
    val mentionedMe:Boolean = false,
    val likedByMe:Boolean = false,
    val attachment:	Attachment? = null,
    val ownedByMe:Boolean = false,
    val users: UserPreview? = null
)
