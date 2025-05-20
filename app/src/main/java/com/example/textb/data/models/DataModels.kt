package com.example.textb.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileData(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val university: String? = null,
    val major: String? = null,
    val profile_image_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class BookData(
    val id: String? = null,
    val title: String = "",
    val author: String = "",
    val subject: String = "",
    val price: Double = 0.0,
    val seller_id: String = "",
    val image_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class UserProfile(
    val name: String,
    val email: String,
    val phone: String,
    val university: String,
    val major: String,
    val profileImageUrl: String
)

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val subject: String,
    val price: Double,
    val sellerId: String,
    val imageUrl: String? = null
)
