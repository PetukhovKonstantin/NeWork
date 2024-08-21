package ru.netology.nework.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.netology.nework.dao.*

@Database(entities = [PostEntity::class, UserEntity::class, EventsEntity::class, JobEntity::class], version = 2)
abstract class AppDb : RoomDatabase() {
    abstract fun postDaoRoom(): PostDaoRoom
}