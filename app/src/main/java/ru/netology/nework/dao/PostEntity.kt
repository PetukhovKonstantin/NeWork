package ru.netology.nework.dao

import androidx.room.*
import ru.netology.nework.utils.Converters
import ru.netology.nework.dto.Post

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val published: String,
    val likedByMe: Boolean = false,
    val likeOwnerIds:  String?,
    @Embedded
    val coords: CoordEmbeddable?,
    val link:String? = null,
    val sharedByMe: Boolean = false,
    val countShared: Int = 999,
    val mentionIds: String?,
    val mentionedMe:Boolean = false,
    @Embedded
    var attachment: AttachmentEmbeddable?,
    val isNew: Boolean = false,
    var likes: Int = 0,
) {
    init {
        likes = Converters.toListDto(likeOwnerIds).size
    }

    fun toDto() = Post(
        id, authorId, author, authorAvatar, content, published, likedByMe, Converters.toListDto(likeOwnerIds), coords?.toDto(),
        link, sharedByMe, countShared, Converters.toListDto(mentionIds), mentionedMe, attachment?.toDto()
    )

    companion object {
        fun fromDto(dto: Post) =
            PostEntity(
                dto.id, dto.authorId, dto.author, dto.authorAvatar, dto.content, dto.published, dto.likedByMe, Converters.fromListDto(dto.likeOwnerIds),
                CoordEmbeddable.fromDto(dto.coords), dto.link, dto.sharedByMe, dto.countShared, Converters.fromListDto(dto.mentionIds), dto.mentionedMe, AttachmentEmbeddable.fromDto(dto.attachment)
            )
    }
}



fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
fun List<Post>.toEntity(isNew: Boolean = false): List<PostEntity> = map(PostEntity::fromDto)
    .map { it.copy(isNew = isNew) }