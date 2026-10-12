package com.github.kr328.clash.design.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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

    override fun onBindViewHolder(holder: ImageSliderViewHolder, position: Int) {
        val item = items[position]

        Glide.with(holder.imageView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(holder.imageView)

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

        holder.imageView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class ImageSliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val titleView: TextView = view.findViewById(R.id.titleView)
        val subtitleView: TextView = view.findViewById(R.id.subtitleView)
    }
}
