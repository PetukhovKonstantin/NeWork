package ru.netology.nework.dao

import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.AttachmentType

data class AttachmentEmbeddable(
    var url: String,
    var type: AttachmentType,
) {
    fun toDto() = Attachment(url, type)

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url, it.type)
        }
    }
}