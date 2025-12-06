package com.example.policemobiledirectory.model

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long?,
    val targetType: NotificationTarget,
    val targetKgid: String? = null,
    val targetDistrict: String? = null,
    val targetStation: String? = null
)






