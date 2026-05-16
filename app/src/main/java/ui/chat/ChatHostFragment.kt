package ui.chat

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import lab3.chat.R
import app.appContainer
import lab3.chat.databinding.FragmentChatHostBinding

class ChatHostFragment : Fragment(R.layout.fragment_chat_host) {
    private var binding: FragmentChatHostBinding? = null

    private val viewModel: ChatViewModel by viewModels(ownerProducer = { requireActivity() }) {
        ChatViewModelFactory(appContainer().repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = FragmentChatHostBinding.bind(view)
        binding = b

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val adapter = ChatAdapter { chat ->
            viewModel.selectChat(chat)
            if (isLandscape) {
                childFragmentManager.commit { replace(R.id.messagesContainer, MessagesFragment.newInstance(chat)) }
            } else {
                findNavController().navigate(
                    R.id.action_chatHostFragment_to_messagesFragment,
                    Bundle().apply { putString("chatName", chat) }
                )
            }
        }

        b.chatsRecycler.layoutManager = LinearLayoutManager(requireContext())
        b.chatsRecycler.adapter = adapter
        b.logoutButton.setOnClickListener { viewModel.logout() }

        if (isLandscape && childFragmentManager.fragments.isEmpty()) {
            val selected = viewModel.uiState.value.selectedChat
            childFragmentManager.commit {
                replace(
                    R.id.messagesContainer,
                    if (selected == null) ChooseChatFragment() else MessagesFragment.newInstance(selected)
                )
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (isLandscape && viewModel.uiState.value.selectedChat != null) {
                viewModel.clearSelection()
                childFragmentManager.commit { replace(R.id.messagesContainer, ChooseChatFragment()) }
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        viewModel.loadChannelsIfNeeded()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submit(state.channels, state.selectedChat)
                if (state.unauthorized) {
                    viewModel.consumeUnauthorized()
                    findNavController().navigate(R.id.action_chatHostFragment_to_loginFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}


