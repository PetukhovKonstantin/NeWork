package ru.netology.nework.utils

object Converters {
    fun fromListDto(list: List<Long>?): String {
        if (list == null) return ""
        return list.toString()
    }

    fun toListDto(data: String?): List<Long> {
        return if (data == "[]") emptyList()
        else {
            val substr = data?.substring(1, data.length - 1)
            substr?.split(", ")?.map { it.toLong() } ?: emptyList()
        }
    }
}