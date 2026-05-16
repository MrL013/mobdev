package ui.chat

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import data.ChatRepository
import data.MessageDto
import data.UnauthorizedException

data class ChatUiState(
    val channels: List<String> = emptyList(),
    val selectedChat: String? = null,
    val messages: List<MessageDto> = emptyList(),
    val unauthorized: Boolean = false,
    val error: String? = null
)

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var oldestLoadedMessageId: String? = null
    private var observingSocket = false

    fun loadChannelsIfNeeded() {
        if (_uiState.value.channels.isNotEmpty()) return
        viewModelScope.launch {
            repository.channels().onSuccess { channels ->
                _uiState.value = _uiState.value.copy(channels = channels)
            }.onFailure(::handleFailure)
        }
    }

    fun selectChat(chat: String) {
        if (chat == _uiState.value.selectedChat && _uiState.value.messages.isNotEmpty()) return
        oldestLoadedMessageId = null
        _uiState.value = _uiState.value.copy(selectedChat = chat, messages = emptyList())
        ensureSocketObserver()
        loadMoreMessages()
    }

    fun clearSelection() {
        oldestLoadedMessageId = null
        _uiState.value = _uiState.value.copy(selectedChat = null, messages = emptyList())
    }

    fun loadMoreMessages() {
        val chat = _uiState.value.selectedChat ?: return
        viewModelScope.launch {
            repository.messages(chat, oldestLoadedMessageId).onSuccess { newMessages ->
                if (newMessages.isNotEmpty()) oldestLoadedMessageId = newMessages.last().id
                _uiState.value = _uiState.value.copy(messages = (_uiState.value.messages + newMessages).distinctBy { it.id })
            }.onFailure(::handleFailure)
        }
    }

    fun sendMessage(text: String) {
        val chat = _uiState.value.selectedChat ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(chat, text).onSuccess { refreshChat() }.onFailure(::handleFailure)
        }
    }

    fun sendImage(uri: Uri, contentResolver: ContentResolver) {
        val chat = _uiState.value.selectedChat ?: return
        viewModelScope.launch {
            repository.sendImageMessage(chat, uri, contentResolver).onSuccess { refreshChat() }.onFailure(::handleFailure)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = _uiState.value.copy(unauthorized = true)
        }
    }

    fun consumeUnauthorized() {
        _uiState.value = _uiState.value.copy(unauthorized = false)
    }

    private fun refreshChat() {
        oldestLoadedMessageId = null
        _uiState.value = _uiState.value.copy(messages = emptyList())
        loadMoreMessages()
    }

    private fun ensureSocketObserver() {
        if (observingSocket) return
        observingSocket = true
        viewModelScope.launch {
            repository.observeIncomingMessages().collect { incoming ->
                val currentChat = _uiState.value.selectedChat
                if (incoming.to == currentChat || incoming.from == currentChat) {
                    _uiState.value = _uiState.value.copy(messages = (_uiState.value.messages + incoming).distinctBy { it.id })
                }
            }
        }
    }

    private fun handleFailure(error: Throwable) {
        if (error is UnauthorizedException) {
            _uiState.value = _uiState.value.copy(unauthorized = true)
        } else {
            _uiState.value = _uiState.value.copy(error = error.message)
        }
    }
}


