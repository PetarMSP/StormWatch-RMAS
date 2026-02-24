package com.example.stormwatch.data.model.domain

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val photoUrl: String = "",

    val weeklyScore: Int = 0,
    val goldTrophies: Int = 0,
    val silverTrophies: Int = 0,
    val bronzeTrophies: Int = 0
)