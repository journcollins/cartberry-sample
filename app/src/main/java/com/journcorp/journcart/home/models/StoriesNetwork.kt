package com.journcorp.journcart.home.models

import com.google.gson.annotations.SerializedName
import com.redmadrobot.stories.models.*

data class StoriesNetwork(
    @SerializedName("get_stories_list")
    val getStoriesList: Int = 0,
    val stories: List<StoriesData>
){
    data class StoriesData(
        val id: String,//this is the url
        val title: String,
        val image: String,
        val story_items: List<StoryItems>,
    ){
        data class StoryItems(
            var item_id: String,
            val media_type: String,
            val media_preview: String,
            val media: String,
            val btn_link: String,
            val btn_text: String,
            val time_added: String,
        )
    }
}