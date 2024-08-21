package ru.netology.nework.repositories

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.IOException
import ru.netology.nework.api.*
import ru.netology.nework.dao.*
import ru.netology.nework.dto.*
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.AppError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import javax.inject.Inject


class RepositoryImpl @Inject constructor(
    private val dao: PostDaoRoom,
    private val apiService: ApiService,
) : Repository {

    private val newerPostsId = mutableListOf<Long>()

    override val data = dao.getAll()
        .map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override val dataUsers = dao.getUsers()
        .map(List<UserEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override val dataEvents: Flow<List<EventResponse>> = dao.getAllEvents()
        .map(List<EventsEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override val dataJobs: Flow<List<Job>> = dao.getJobs()
        .map(List<JobEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response = apiService.getNewer(id)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())
            dao.insert(body.toEntity(isNew = true))
            body.forEach {
                newerPostsId.add(it.id)
            }
            emit(body.size)
        }
    }

    override suspend fun showNewPosts() {
        dao.showNewPosts()
    }

    override suspend fun getAllAsync() {
        try {
            val response = apiService.getAll()

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeByIdAsync(id: Long) {
        try {
            val response = apiService.removeById(id)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            dao.removeById(id)

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveAsync(post: Post) {
        try {
            val postRequest = PostCreateRequest(
                post.id,
                post.content,
                post.coords,
                post.link,
                post.attachment,
                post.mentionIds
            )
            val response = apiService.save(postRequest)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val postResponse = response.body() ?: throw ApiError(response.message())
            val postDaoSaved = Post(
                id = postResponse.id,
                authorId = postResponse.authorId,
                author = postResponse.author,
                authorAvatar = postResponse.authorAvatar,
                content = postResponse.content,
                published = postResponse.published,
                likedByMe = postResponse.likedByMe,
                likeOwnerIds = postResponse.likeOwnerIds,
                attachment = postResponse.attachment,
                ownedByMe = postResponse.ownedByMe
            )
            dao.save(PostEntity.fromDto(postDaoSaved))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(post: Post, upload: MediaUpload, attachmentType: AttachmentType) {
        try {
            val media = upload(upload)
            val postWithAttachment = post.copy(attachment =
            Attachment(
                url = media.url,
                type = attachmentType,
            ))
            saveAsync(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun upload(upload: MediaUpload): MediaResponse {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )

            val response = apiService.upload(media)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            return response.body() ?: throw ApiError(response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getUsers() {
        try {
            val response = apiService.getUsers()
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val userResponseList = response.body() ?: throw ApiError(response.message())
            val usersDaoSaved = userResponseList.map { userResponse ->
                User(
                    id = userResponse.id,
                    login = userResponse.login,
                    name = userResponse.name,
                    avatar = userResponse.avatar
                )
            }
            dao.insertUsers(usersDaoSaved.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getUserBuId(id: Long) {
        try {
            val response = apiService.getUserById(id)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val userResponse = response.body() ?: throw ApiError(response.message())
            val userDaoSaved = User(
                id = userResponse.id,
                login = userResponse.login,
                name = userResponse.name,
                avatar = userResponse.avatar,
            )
            dao.insert(UserEntity.fromDto(userDaoSaved))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getAllEvents() {
        try {
            val response = apiService.getAllEvents()

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())
            dao.insertEvents(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveEvents(event: EventResponse) {
        try {
            val eventRequest = EventCreateRequest(
                event.id,
                event.authorId,
                event.author,
                event.authorJob,
                event.authorAvatar,
                event.content,
                event.datetime,
                event.published,
                event.coords,
                event.type,
                event.likeOwnerIds,
                event.likedByMe,
                event.speakerIds,
                event.participantsIds,
                event.participatedByMe,
                event.attachment,
                event.link,
                event.users
            )
            val response = apiService.saveEvents(eventRequest)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val eventResponse = response.body() ?: throw ApiError(response.message())
            dao.saveEvent(EventsEntity.fromDto(eventResponse))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveEventsWithAttachment(event: EventResponse, upload: MediaUpload, attachmentType: AttachmentType) {
        try {
            val media = upload(upload)
            val eventWithAttachment = event.copy(attachment =
            Attachment(
                url = media.url,
                type = attachmentType,
            ))
            saveEvents(eventWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeEventsById(id: Long) {
        try {
            val response = apiService.removeByIdEvent(id)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            dao.removeByIdEvent(id)

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeByIdEvents(event: EventResponse) {
        dao.likeByIdEvent(event.id)
        try {
            val response = if (event.likedByMe) {
                apiService.dislikeByIdEvent(event.id)
            } else {
                apiService.likeByIdEvent(event.id)
            }
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val body = response.body() ?: throw ApiError(response.message())
            dao.insertEvents(EventsEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun joinByIdEvents(event: EventResponse) {
        dao.joinByIdEvent(event.id)
        try {
            val response = if (event.participatedByMe) {
                apiService.unJoinByIdEvent(event.id)
            } else {
                apiService.joinByIdEvent(event.id)
            }
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val body = response.body() ?: throw ApiError(response.message())
            dao.insertEvents(EventsEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getJobs(id: Long) {
        try {
            val response = apiService.getUserJobs(id)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())
            dao.insertJobs(body.toEntity(id))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getMyJobs(id: Long) {
        try {
            val response = apiService.getMyJobs()

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())
            dao.insertJobs(body.toEntity(id))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveJob(job: Job) {
        try {
            val response = apiService.saveJob(job)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val jobResponse = response.body() ?: throw ApiError(response.message())
            dao.insertJob(JobEntity.fromDto(jobResponse))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeJobById(id: Long) {
        dao.removeByIdJob(id)
        try {
            val response = apiService.removeJobById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            dao.removeByIdJob(id)
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeByIdAsync(post: Post) {
        dao.likeById(post.id)
        try {
            val response = if (post.likedByMe) {
                apiService.dislikeById(post.id)
            } else {
                apiService.likeById(post.id)

            }
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val body = response.body() ?: throw ApiError(response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun edit(post: Post) {
        saveAsync(post)
    }
}
