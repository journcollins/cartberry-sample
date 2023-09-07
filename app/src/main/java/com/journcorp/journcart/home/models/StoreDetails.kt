package com.journcorp.journcart.home.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StoreDetails(
    val url: String,
    val banner: String,
    val title: String,
    val stories_count: Int
) : Parcelable
