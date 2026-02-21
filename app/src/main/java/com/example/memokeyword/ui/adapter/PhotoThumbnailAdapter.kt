package com.example.memokeyword.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.memokeyword.R

class PhotoThumbnailAdapter(
    private val onRemoveClick: ((Uri) -> Unit)? = null
) : RecyclerView.Adapter<PhotoThumbnailAdapter.PhotoViewHolder>() {

    private val items = mutableListOf<Uri>()

    fun setPhotos(uris: List<Uri>) {
        items.clear()
        items.addAll(uris)
        notifyDataSetChanged()
    }

    fun getPhotos(): List<Uri> = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_thumbnail, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.iv_photo_thumb)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btn_remove_photo)

        fun bind(uri: Uri) {
            ivPhoto.setImageURI(uri)
            if (onRemoveClick != null) {
                btnRemove.visibility = View.VISIBLE
                btnRemove.setOnClickListener { onRemoveClick.invoke(uri) }
            } else {
                btnRemove.visibility = View.GONE
            }
        }
    }
}
