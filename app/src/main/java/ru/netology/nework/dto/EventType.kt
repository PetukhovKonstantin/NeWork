package ru.netology.nework.dto

import android.content.Context
import ru.netology.nework.R

enum class EventType {
    OFFLINE, ONLINE;

    fun getDisplayName(context: Context): String {
        return when (this) {
            OFFLINE -> context.getString(R.string.offline)
            ONLINE -> context.getString(R.string.online)
        }
    }
}