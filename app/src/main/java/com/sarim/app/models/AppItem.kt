package com.sarim.app.models

import android.graphics.drawable.Drawable

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isSystem: Boolean,
    val isSuspended: Boolean = false
)
