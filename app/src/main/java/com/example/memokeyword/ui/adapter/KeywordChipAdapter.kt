package com.example.memokeyword.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memokeyword.databinding.ItemKeywordChipBinding

class KeywordChipAdapter(
    private val onKeywordClick: ((String) -> Unit)? = null,
    private val onRemoveClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<KeywordChipAdapter.ChipViewHolder>() {

    private val keywords = mutableListOf<String>()
    private var showRemoveButton = false

    fun setKeywords(list: List<String>, showRemove: Boolean = false) {
        keywords.clear()
        keywords.addAll(list)
        showRemoveButton = showRemove
        notifyDataSetChanged()
    }

    fun getKeywords(): List<String> = keywords.toList()

    fun addKeyword(keyword: String) {
        if (keyword.isNotBlank() && !keywords.contains(keyword)) {
            keywords.add(keyword)
            notifyItemInserted(keywords.size - 1)
        }
    }

    fun removeKeyword(keyword: String) {
        val idx = keywords.indexOf(keyword)
        if (idx >= 0) {
            keywords.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemKeywordChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        holder.bind(keywords[position])
    }

    override fun getItemCount() = keywords.size

    inner class ChipViewHolder(private val binding: ItemKeywordChipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(keyword: String) {
            binding.tvKeyword.text = "#$keyword"
            binding.btnRemove.visibility = if (showRemoveButton)
                android.view.View.VISIBLE else android.view.View.GONE

            binding.root.setOnClickListener { onKeywordClick?.invoke(keyword) }
            binding.btnRemove.setOnClickListener { onRemoveClick?.invoke(keyword) }
        }
    }
}
