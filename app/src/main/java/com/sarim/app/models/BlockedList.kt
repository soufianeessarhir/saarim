package com.sarim.app.models

data class BlockedList(
    val id: String,
    val title: String,
    val sites: List<String>
)
