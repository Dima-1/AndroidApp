package com.strikelines.app.domain.models

data class Chart(
    val description: String,
    val downloadurl: String,
    val imageurl: String,
    val latitude: String,
    val longitude: String,
    val name: String,
    val price: String,
    val region: String,
    val weburl: String
)