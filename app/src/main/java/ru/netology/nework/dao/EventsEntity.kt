package ru.netology.nework.dao

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.utils.Converters
import ru.netology.nework.dto.*

@Entity
class EventsEntity(
    @PrimaryKey
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val authorJob: String? = null,
    val content: String,
    val datetime: String,
    val published: String,
    @Embedded
    val coords: CoordEmbeddable?,
    val typeEvent: EventType,
    val likeOwnerIds: String?,
    val likedByMe: Boolean = false,
    val speakerIds: String?,
    val participantsIds: String?,
    val participatedByMe: Boolean = false,
    @Embedded
    val attachment: AttachmentEmbeddable? = null,
    val link: String? = null,
    val ownedByMe: Boolean = false,
) {

    fun toDto() = EventResponse(
        id,
        authorId,
        author,
        authorAvatar,
        authorJob,
        content,
        datetime,
        published,
        coords?.toDto(),
        typeEvent,
        Converters.toListDto(likeOwnerIds),
        likedByMe,
        Converters.toListDto(speakerIds),
        Converters.toListDto(participantsIds),
        participatedByMe,
        attachment?.toDto(),
        link,
        ownedByMe,
        null
    )

    companion object {
        fun fromDto(dto: EventResponse) =
            EventsEntity(
                dto.id,
                dto.authorId,
                dto.author,
                dto.authorAvatar,
                dto.authorJob,
                dto.content,
                dto.datetime,
                dto.published,
                CoordEmbeddable.fromDto(dto.coords),
                dto.type,
                Converters.fromListDto(dto.likeOwnerIds),
                dto.likedByMe,
                Converters.fromListDto(dto.speakerIds),
                Converters.fromListDto(dto.participantsIds),
                dto.participatedByMe,
                AttachmentEmbeddable.fromDto(dto.attachment),
                dto.link,
                dto.ownedByMe
            )
    }
}


fun List<EventsEntity>.toDto(): List<EventResponse> = map(EventsEntity::toDto)
fun List<EventResponse>.toEntity(): List<EventsEntity> = map(EventsEntity::fromDto)