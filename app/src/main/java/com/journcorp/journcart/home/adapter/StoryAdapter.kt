package com.journcorp.journcart.home.adapter

import android.graphics.Color
import android.view.View
import androidx.core.view.isVisible
import com.journcorp.journcart.R
import com.redmadrobot.stories.databinding.ItemPreviewStoryBinding
import com.redmadrobot.stories.models.StoriesInputParams
import com.redmadrobot.stories.models.Story
import com.redmadrobot.stories.stories.adapter.StoriesBasePreviewAdapter
import com.redmadrobot.stories.utils.setImageWithGlide


class StoryAdapter(
    private val listener: StoriesAdapterListener,
    private val inputParams: StoriesInputParams
) : StoriesBasePreviewAdapter(R.layout.item_story, inputParams) {

    override fun createViewHolder(view: View): StoriesBasePreviewViewHolder =
        StoriesPreviewViewHolder(view, listener, inputParams)

    class StoriesPreviewViewHolder(
        view: View,
        listener: StoriesAdapterListener,
        inputParams: StoriesInputParams
    ) : StoriesBasePreviewViewHolder(listener, view, inputParams) {
        private val binding = ItemPreviewStoryBinding.bind(view)

        override fun bind(data: Story) = with(binding) {
            textTitle.text = data.title
            imgPreview.setImageWithGlide(
                imageUrl = data.previewUrl,
                onReady = { drawable ->
                    drawable ?: return@setImageWithGlide

                    textTitle.setTextColor(Color.WHITE)
                    /*Palette.from(drawable.toBitmap()).generate {
                        val swatch = it?.dominantSwatch ?: return@generate
                        textTitle.setTextColor(
                            if (StoriesColorUtils.isDark(swatch.rgb)) Color.BLACK else Color.WHITE
                            Color.WHITE
                        )
                    }*/
                },
                onFailed = {
                    textTitle.setTextColor(Color.BLACK)
                }
            )
            viewSeen.isVisible = data.isSeen
        }
    }
}
