package ru.dzhaparidze.mykct.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Пользователь из auth mykct-api. `id` - логин (i24s0291), `username` - ФИО из LDAP.
 * Групповые поля есть только у студента, у преподавателя их нет вовсе.
 */
@Serializable
data class User(
    val id: String = "",
    val username: String = "",
    val role: String? = null,
    @SerialName("academic_group") val academicGroup: String? = null,
    val profile: String? = null,
    val subgroup: String? = null,
    @SerialName("english_group") val englishGroup: String? = null,
) {
    val isStudent: Boolean get() = role == "student"
}

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

