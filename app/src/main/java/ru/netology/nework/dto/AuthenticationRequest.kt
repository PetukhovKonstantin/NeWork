package ru.netology.nework.dto

data class AuthenticationRequest(
    val login: String,
    val password: String
)
