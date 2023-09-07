package com.journcorp.journcart.productDetails.adapters

import android.app.Activity
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.core.utils.GlideLoader
import com.journcorp.journcart.databinding.ItemProductDetailsSliderBinding
import com.journcorp.journcart.databinding.ItemProductDetailsSliderVideoBinding
import com.journcorp.journcart.productDetails.dataClass.ProductTopCarouselModel

//we didn't add the "context" property into the constructor coz we don't really use it but u can add it if the tutorial guy does it so u follow
class ProductViewPagerAdapter(
    private val activity: Activity,
    private val ItemList: List<ProductTopCarouselModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_VIDEO = 0
        private const val VIEW_IMAGE = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        //we return the inflated xml file that we have passed into the ViewHolder class constructor
        /*val view = ItemProductDetailsSliderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)*/

        return when (viewType) {
            VIEW_VIDEO -> VideoViewHolder.from(
                parent
            )
            VIEW_IMAGE -> ImageViewHolder.from(
                parent
            )
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (ItemList[position].vid !== null) {//video exists
            VIEW_VIDEO
        } else {
            VIEW_IMAGE
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val video = ItemList[position].vid
        val image = ItemList[position].img

        if (video !== null) {//video exists
            (holder as VideoViewHolder).bind(activity, video, image)
        } else {
            (holder as ImageViewHolder).bind(image)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is VideoViewHolder) {
            holder.binding.ProductSliderVideoView.stopPlayback()
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int = ItemList.size

    class ImageViewHolder private constructor(val binding: ItemProductDetailsSliderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val imageView = binding.ProductSliderImageView
        private val bgImageView = binding.ProductSliderImageViewBlur

        fun bind(img: Any) {
            val image = Constants.storage_url + "/" + img

            GlideLoader(itemView.context).loadUserPictureFitCenterBlur(
                image,
                imageView,
                bgImageView
            )
        }

        companion object {
            fun from(parent: ViewGroup): ImageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemProductDetailsSliderBinding.inflate(layoutInflater, parent, false)
                return ImageViewHolder(binding)
            }
        }
    }

    class VideoViewHolder private constructor(val binding: ItemProductDetailsSliderVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var isPlaying = true
        private lateinit var mp: MediaPlayer

        private val bgImageView = binding.ProductSliderImageViewBlur
        private val videoView = binding.ProductSliderVideoView
        private val progressBar = binding.PBVideoView

        private val playButton = binding.playButton
        private val pauseButton = binding.pauseButton
        private val unMuteButton = binding.unMuteButton
        private val muteButton = binding.muteButton

        fun bind(activity_: Activity, vid: Any, img: Any?) {
            val video = Constants.storage_url + "/" + vid
            val image = Constants.storage_url + "/" + img

            /*val mediaController = MediaController(itemView.context)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)*/

            videoView.setVideoPath(video)

            progressBar.visibility = View.VISIBLE
            if(!videoView.isPlaying && isPlaying){
                progressBar.visibility = View.VISIBLE
                progressBar.progress = videoView.bufferPercentage
            }else{
                progressBar.visibility = View.GONE
            }

            videoView.setOnPreparedListener {
                mp = it
                // Start the video playback
                progressBar.visibility = View.GONE
                //videoView.seekTo(500)
                mute()
                play()

                videoView.setOnClickListener {
                    if (isPlaying) {//pause the video
                        pause()
                    } else {//play the video
                        play()
                    }
                }

                playButton.setOnClickListener {
                    play()
                }

                pauseButton.setOnClickListener {
                    pause()
                }

                muteButton.setOnClickListener {
                    mute()
                }

                unMuteButton.setOnClickListener {
                    unMute()
                }

                mp.setOnCompletionListener {
                    pause()
                }
            }



            GlideLoader(itemView.context).loadBlurPicture(image, bgImageView)
        }

        private fun pause(){
            videoView.pause()
            isPlaying = false

            playButton.visibility = View.VISIBLE
            pauseButton.visibility = View.GONE
        }

        private fun play(){
            videoView.start()
            isPlaying = true

            playButton.visibility = View.GONE
            pauseButton.visibility = View.VISIBLE
        }

        private fun mute(){
            mp.setVolume(0f, 0f)
            unMuteButton.visibility = View.VISIBLE
            muteButton.visibility = View.GONE
        }

        private fun unMute(){
            mp.setVolume(1f, 1f)
            unMuteButton.visibility = View.GONE
            muteButton.visibility = View.VISIBLE
        }

        companion object {
            fun from(parent: ViewGroup): VideoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding =
                    ItemProductDetailsSliderVideoBinding.inflate(layoutInflater, parent, false)
                return VideoViewHolder(binding)
            }
        }
    }
}


