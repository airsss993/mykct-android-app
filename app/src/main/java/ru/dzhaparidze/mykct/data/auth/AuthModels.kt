package ru.dzhaparidze.mykct.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Студент из auth-сервиса. Группа и подгруппы отсюда подставляются в расписание. */
@Serializable
data class User(
    val id: String = "",
    val username: String = "",
    val role: String? = null,
    @SerialName("academic_group") val academicGroup: String? = null,
    val profile: String? = null,
    val subgroup: String? = null,
    @SerialName("english_group") val englishGroup: String? = null,
)

@Serializable
data class SignInResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("access_expires_in") val accessExpiresIn: Long,
    @SerialName("refresh_expires_in") val refreshExpiresIn: Long,
    val user: User,
)

@Serializable
data class AccessTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    val user: User,
)

@Serializable
data class RefreshTokenResponse(
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
data class ValidateResponse(val valid: Boolean, val user: User? = null)
