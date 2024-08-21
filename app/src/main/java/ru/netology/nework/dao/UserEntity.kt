package ru.netology.nework.dao

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.User

@Entity
data class UserEntity(
    @PrimaryKey
    val id: Long,
    val login: String,
    val name: String,
    val avatar: String?,
) {

    fun toDto() = User(
        id, login, name, avatar
    )

    companion object {
        fun fromDto(dto: User) =
            UserEntity(
                dto.id, dto.login, dto.name, dto.avatar
            )
    }
}

fun List<UserEntity>.toDto(): List<User> = map(UserEntity::toDto)
fun List<User>.toEntity(): List<UserEntity> = map(UserEntity::fromDto)