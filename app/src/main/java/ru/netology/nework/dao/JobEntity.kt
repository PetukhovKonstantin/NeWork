package ru.netology.nework.dao

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Job

@Entity
data class JobEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val position: String,
    val start: String,
    val finish: String?,
    val link: String?,
    val ownerId:Long = -1
) {

    fun toDto() = Job(
        id, name, position, start, finish, link, ownerId
    )

    companion object {
        fun fromDto(dto: Job) =
            JobEntity(
                dto.id, dto.name, dto.position, dto.start, dto.finish, dto.link, dto.ownerId
            )
    }
}

fun List<JobEntity>.toDto(): List<Job> = map(JobEntity::toDto)
fun List<Job>.toEntity(id: Long): List<JobEntity> = map(JobEntity::fromDto).map { it.copy(ownerId = id) }