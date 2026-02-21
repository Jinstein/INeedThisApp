package com.example.memokeyword.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.memokeyword.data.MemoWithKeywords
import com.example.memokeyword.databinding.ItemMemoBinding
import java.text.SimpleDateFormat
import java.util.*

class MemoAdapter(
    private val onMemoClick: (MemoWithKeywords) -> Unit,
    private val onMemoLongClick: (MemoWithKeywords) -> Boolean
) : ListAdapter<MemoWithKeywords, MemoAdapter.MemoViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val binding = ItemMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemoViewHolder(private val binding: ItemMemoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val photoAdapter = PhotoThumbnailAdapter(onRemoveClick = null)

        init {
            binding.rvPhotoPreview.apply {
                layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
                adapter = photoAdapter
            }
        }

        fun bind(item: MemoWithKeywords) {
            binding.tvTitle.text = item.memo.title.ifBlank { "(제목 없음)" }
            binding.tvContent.text = item.memo.content
            binding.tvDate.text = formatDate(item.memo.updatedAt)

            val keywordText = item.keywords
                .take(5)
                .joinToString("  ") { "#${it.word}" }
            binding.tvKeywords.text = keywordText

            val sortedPhotos = item.photos.sortedBy { it.orderIndex }
            if (sortedPhotos.isNotEmpty()) {
                val uris = sortedPhotos.map { Uri.parse("file://${it.filePath}") }
                photoAdapter.setPhotos(uris)
                binding.rvPhotoPreview.visibility = View.VISIBLE
            } else {
                photoAdapter.setPhotos(emptyList())
                binding.rvPhotoPreview.visibility = View.GONE
            }

            binding.root.setOnClickListener { onMemoClick(item) }
            binding.root.setOnLongClickListener { onMemoLongClick(item) }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MemoWithKeywords>() {
        override fun areItemsTheSame(old: MemoWithKeywords, new: MemoWithKeywords) =
            old.memo.id == new.memo.id

        override fun areContentsTheSame(old: MemoWithKeywords, new: MemoWithKeywords) =
            old.memo == new.memo && old.keywords == new.keywords && old.photos == new.photos
    }
}
