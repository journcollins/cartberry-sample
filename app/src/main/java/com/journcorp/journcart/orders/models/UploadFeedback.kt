package com.journcorp.journcart.orders.models

import com.google.gson.annotations.SerializedName

data class UploadFeedback(
    @SerializedName("error")
    val error: Boolean = false,

    @SerializedName("message")
    val message: String = ""
){
    // No-args constructor is optional if you're using @JvmOverloads on the primary constructor.
    // You can include it explicitly if Gson requires it.
    constructor() : this(false, "")
}