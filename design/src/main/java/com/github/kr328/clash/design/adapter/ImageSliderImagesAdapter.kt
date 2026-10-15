package com.github.kr328.clash.design.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.kr328.clash.design.R

data class BannerItem(
    val imageUrl: String,
    val title: String? = null,
    val subtitle: String? = null,
    val link: String? = null
)

class ImageSliderImagesAdapter(
    private val items: List<BannerItem>,
    private val onItemClick: (BannerItem) -> Unit
) : RecyclerView.Adapter<ImageSliderImagesAdapter.ImageSliderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageSliderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_slider, parent, false)
        return ImageSliderViewHolder(view)
    }

    private fun isVideo(url: String): Boolean {
        val u = url.lowercase().substringBefore('?')
        return u.endsWith(".mp4") || u.endsWith(".mov") || u.endsWith(".m3u8") || u.endsWith(".webm")
    }

    override fun onBindViewHolder(holder: ImageSliderViewHolder, position: Int) {
        val item = items[position]

        if (isVideo(item.imageUrl)) {
            // 视频
            holder.imageView.visibility = View.GONE
            holder.videoView.visibility = View.VISIBLE
            try {
                val mc = MediaController(holder.videoView.context)
                mc.setVisibility(View.GONE)
                holder.videoView.setMediaController(mc)
                holder.videoView.setVideoURI(Uri.parse(item.imageUrl))
                holder.videoView.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f)
                    holder.videoView.start()
                }
                holder.videoView.setOnErrorListener { _, _, _ -> true }
            } catch (_: Throwable) {}
        } else {
            // 图片
            holder.videoView.visibility = View.GONE
            holder.imageView.visibility = View.VISIBLE
            Glide.with(holder.imageView.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(holder.imageView)
        }

        if (item.title.isNullOrBlank()) {
            holder.titleView.visibility = View.GONE
        } else {
            holder.titleView.visibility = View.VISIBLE
            holder.titleView.text = item.title
        }

        if (item.subtitle.isNullOrBlank()) {
            holder.subtitleView.visibility = View.GONE
        } else {
            holder.subtitleView.visibility = View.VISIBLE
            holder.subtitleView.text = item.subtitle
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun onViewRecycled(holder: ImageSliderViewHolder) {
        super.onViewRecycled(holder)
        try {
            holder.videoView.stopPlayback()
        } catch (_: Throwable) {}
    }

    override fun getItemCount(): Int = items.size

    class ImageSliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val videoView: VideoView = view.findViewById(R.id.videoView)
        val titleView: TextView = view.findViewById(R.id.titleView)
        val subtitleView: TextView = view.findViewById(R.id.subtitleView)
    }
}
