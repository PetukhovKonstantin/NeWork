package ru.netology.nework.utils

import android.os.Bundle
import com.yandex.mapkit.geometry.Point
import ru.netology.nework.dto.Coords
import ru.netology.nework.utils.ConstantValues.EVENT_ID
import ru.netology.nework.utils.ConstantValues.EVENT_REQUEST_TYPE
import ru.netology.nework.utils.ConstantValues.POST_CONTENT
import ru.netology.nework.utils.ConstantValues.POST_LINK
import ru.netology.nework.utils.ConstantValues.POST_MENTIONS_COUNT
import ru.netology.nework.utils.ConstantValues.USER_ID


class Companion {

    companion object {
        var Bundle.textArg: String?
            set(value) = putString(POST_CONTENT, value)
            get() = getString(POST_CONTENT)
        var Bundle.linkArg: String?
            set(value) = putString(POST_LINK, value)
            get() = getString(POST_LINK)
        var Bundle.mentionsCountArg: Long
            set(value) = putLong(POST_MENTIONS_COUNT, value)
            get() = getLong(POST_MENTIONS_COUNT)
        var Bundle.eventId: Long
            set(value) = putLong(EVENT_ID, value)
            get() = getLong(EVENT_ID)
        var Bundle.eventRequestType: String?
            set(value) = putString(EVENT_REQUEST_TYPE, value)
            get() = getString(EVENT_REQUEST_TYPE)
        var Bundle.userId: Long
            set(value) = putLong(USER_ID, value)
            get() = getLong(USER_ID)
    }

}