package ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import lab3.chat.databinding.ItemChatBinding

class ChatAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    private var items: List<String> = emptyList()
    private var selected: String? = null

    fun submit(items: List<String>, selected: String?) {
        this.items = items
        this.selected = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, item == selected)
    }

    inner class ChatViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(name: String, isSelected: Boolean) {
            binding.chatNameText.text = name
            binding.root.isSelected = isSelected
            binding.root.setOnClickListener { onClick(name) }
        }
    }
}


