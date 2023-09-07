package com.journcorp.journcart.notifications.models

data class Notifications(
    val url:String,
    val notification_counter:String,
    val link:String,
    val date_added: String,
    val text: String,
    val title: String,
    val banner1: String
)