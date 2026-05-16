package ui.chat

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import lab3.chat.R
import app.appContainer
import lab3.chat.databinding.FragmentMessagesBinding

class MessagesFragment : Fragment(R.layout.fragment_messages) {
    private var binding: FragmentMessagesBinding? = null

    private val viewModel: ChatViewModel by viewModels(ownerProducer = { requireActivity() }) {
        ChatViewModelFactory(appContainer().repository)
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.sendImage(uri, requireContext().contentResolver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = FragmentMessagesBinding.bind(view)
        binding = b
        val chatName = arguments?.getString("chatName") ?: return
        viewModel.selectChat(chatName)

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        b.backButton.visibility = if (isLandscape) View.GONE else View.VISIBLE
        b.backButton.setOnClickListener { findNavController().navigateUp() }

        val adapter = MessageAdapter { path ->
            findNavController().navigate(
                R.id.action_messagesFragment_to_imageFragment,
                Bundle().apply { putString("imagePath", path) }
            )
        }

        val layoutManager = LinearLayoutManager(requireContext())
        b.messagesRecycler.layoutManager = layoutManager
        b.messagesRecycler.adapter = adapter
        b.messagesRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy >= 0) return
                if (layoutManager.findFirstVisibleItemPosition() <= 2) {
                    viewModel.loadMoreMessages()
                }
            }
        })

        b.sendButton.setOnClickListener {
            viewModel.sendMessage(b.messageInput.text.toString())
            b.messageInput.setText("")
        }
        b.attachImageButton.setOnClickListener { pickImage.launch("image/*") }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submit(state.messages)
                if (state.unauthorized) {
                    viewModel.consumeUnauthorized()
                    findNavController().navigate(R.id.loginFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(chatName: String): MessagesFragment = MessagesFragment().apply {
            arguments = Bundle().apply { putString("chatName", chatName) }
        }
    }
}


