package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDaoRoom {
    @Query("SELECT * FROM PostEntity WHERE isNew = 0 ORDER BY id DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("UPDATE PostEntity SET content = :content WHERE id = :id")
    suspend fun updateContentById(id: Long, content: String)

    suspend fun save(post: PostEntity) =
        if (post.id == 0L) insert(post) else updateContentById(post.id, post.content)

    @Query(
        """
        UPDATE PostEntity SET
        likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
        likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
        WHERE id = :id
        """
    )
    suspend fun likeById(id: Long)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query(
        """
        UPDATE PostEntity SET isNew = 0
        WHERE isNew = 1
        """
    )
    suspend fun showNewPosts()
    @Query("SELECT * FROM UserEntity ORDER BY id DESC")
    fun getUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM UserEntity ORDER BY id DESC")
    fun getUserById(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT * FROM EventsEntity ORDER BY id DESC")
    fun getAllEvents(): Flow<List<EventsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(event: EventsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(event: List<EventsEntity>)

    @Query("UPDATE EventsEntity SET content = :content WHERE id = :id")
    suspend fun updateContentEventsById(id: Long, content: String)

    suspend fun saveEvent(event: EventsEntity) =
        if (event.id == 0L) insertEvents(event) else updateContentById(event.id, event.content)

    @Query(
        """
        UPDATE EventsEntity SET
        likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
        WHERE id = :id
        """
    )
    suspend fun likeByIdEvent(id: Long)

    @Query(
        """
        UPDATE EventsEntity SET
        participatedByMe = CASE WHEN participatedByMe THEN 0 ELSE 1 END
        WHERE id = :id
        """
    )
    suspend fun joinByIdEvent(id: Long)

    @Query("DELETE FROM EventsEntity WHERE id = :id")
    suspend fun removeByIdEvent(id: Long)

    @Query("DELETE FROM JobEntity WHERE id = :id")
    suspend fun removeByIdJob(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobEntity>)

    @Query("SELECT * FROM JobEntity")
    fun getJobs(): Flow<List<JobEntity>>
}