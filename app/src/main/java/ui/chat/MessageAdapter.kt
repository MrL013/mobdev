package ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import data.MessageDto
import lab3.chat.databinding.ItemMessageBinding

class MessageAdapter(private val onImageClick: (String) -> Unit) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private var items: List<MessageDto> = emptyList()

    fun submit(items: List<MessageDto>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) = holder.bind(items[position])

    inner class MessageViewHolder(private val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: MessageDto) {
            binding.authorText.text = message.from
            binding.messageText.text = message.data.text?.text.orEmpty()

            val path = message.data.image?.link
            if (path.isNullOrBlank()) {
                binding.messageImage.visibility = View.GONE
            } else {
                binding.messageImage.visibility = View.VISIBLE
                val imageSource = if (
                    path.startsWith("content://") ||
                    path.startsWith("file://") ||
                    path.startsWith("http://") ||
                    path.startsWith("https://")
                ) {
                    path
                } else {
                    "https://faerytea.name/thumb/$path"
                }
                binding.messageImage.load(imageSource)
                binding.messageImage.setOnClickListener { onImageClick(path) }
            }
        }
    }
}


