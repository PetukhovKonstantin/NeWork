package ru.netology.nework.dao

import ru.netology.nework.dto.Coords

data class CoordEmbeddable(
    val latitude : String?,
    val longitude : String?,
) {
    fun toDto() =
        Coords(latitude, longitude)

    companion object {
        fun fromDto(dto: Coords?) = dto?.let {
            CoordEmbeddable(it.lat, it.long)
        }
    }
}