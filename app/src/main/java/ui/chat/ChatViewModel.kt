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
import data.ImageData
import data.MessageData
import data.MessageDto
import data.TextData
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
    private var isLoadingMessages = false
    private var canLoadMoreMessages = true
    private val messagesByChat = mutableMapOf<String, List<MessageDto>>()
    private val oldestMessageIdByChat = mutableMapOf<String, String?>()
    private val canLoadMoreByChat = mutableMapOf<String, Boolean>()

    fun loadChannelsIfNeeded() {
        if (_uiState.value.channels.isNotEmpty()) return
        viewModelScope.launch {
            repository.channels().onSuccess { channels ->
                _uiState.value = _uiState.value.copy(channels = channels)
            }.onFailure(::handleFailure)
        }
    }

    fun selectChat(chat: String) {
        if (chat == _uiState.value.selectedChat) return
        oldestLoadedMessageId = oldestMessageIdByChat[chat]
        canLoadMoreMessages = canLoadMoreByChat[chat] ?: true
        val cachedMessages = messagesByChat[chat].orEmpty()
        _uiState.value = _uiState.value.copy(selectedChat = chat, messages = cachedMessages)
        ensureSocketObserver()
        if (cachedMessages.isEmpty()) {
            loadMoreMessages()
        }
    }

    fun clearSelection() {
        oldestLoadedMessageId = null
        canLoadMoreMessages = true
        _uiState.value = _uiState.value.copy(selectedChat = null, messages = emptyList())
    }

    fun loadMoreMessages() {
        val chat = _uiState.value.selectedChat ?: return
        if (isLoadingMessages || !canLoadMoreMessages) return
        isLoadingMessages = true
        viewModelScope.launch {
            try {
                repository.messages(chat, oldestLoadedMessageId).onSuccess { newMessages ->
                    newMessages.oldestMessageId()?.let { oldestLoadedMessageId = it }
                    canLoadMoreMessages = newMessages.size >= ChatRepository.MESSAGES_PAGE_SIZE
                    oldestMessageIdByChat[chat] = oldestLoadedMessageId
                    canLoadMoreByChat[chat] = canLoadMoreMessages
                    updateChatMessages(chat, messagesByChat[chat].orEmpty() + newMessages)
                }.onFailure(::handleFailure)
            } finally {
                isLoadingMessages = false
            }
        }
    }

    fun sendMessage(text: String) {
        val chat = _uiState.value.selectedChat ?: return
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return
        val localMessage = MessageDto(
            id = "local-${System.currentTimeMillis()}",
            from = repository.currentLogin,
            to = chat,
            data = MessageData(text = TextData(trimmedText))
        )
        updateChatMessages(chat, messagesByChat[chat].orEmpty() + localMessage)
        viewModelScope.launch {
            repository.sendMessage(chat, trimmedText).onFailure { error ->
                updateChatMessages(chat, messagesByChat[chat].orEmpty().filterNot { it.id == localMessage.id })
                handleFailure(error)
            }
        }
    }

    fun sendImage(uri: Uri, contentResolver: ContentResolver) {
        val chat = _uiState.value.selectedChat ?: return
        val localMessage = MessageDto(
            id = "local-image-${System.currentTimeMillis()}",
            from = repository.currentLogin,
            to = chat,
            data = MessageData(image = ImageData(link = uri.toString()))
        )
        updateChatMessages(chat, messagesByChat[chat].orEmpty() + localMessage)
        viewModelScope.launch {
            repository.sendImageMessage(chat, uri, contentResolver).onFailure { error ->
                updateChatMessages(chat, messagesByChat[chat].orEmpty().filterNot { it.id == localMessage.id })
                handleFailure(error)
            }
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

    fun consumeError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun refreshChat() {
        oldestLoadedMessageId = null
        canLoadMoreMessages = true
        _uiState.value.selectedChat?.let { chat ->
            oldestMessageIdByChat.remove(chat)
            canLoadMoreByChat[chat] = true
            messagesByChat[chat] = emptyList()
        }
        _uiState.value = _uiState.value.copy(messages = emptyList())
        loadMoreMessages()
    }

    private fun ensureSocketObserver() {
        if (observingSocket) return
        observingSocket = true
        viewModelScope.launch {
            repository.observeIncomingMessages().collect { incoming ->
                val chat = incoming.chatKey()
                if (chat != null) {
                    updateChatMessages(chat, messagesByChat[chat].orEmpty() + incoming)
                }
            }
        }
    }

    private fun updateChatMessages(chat: String, messages: List<MessageDto>) {
        val mergedMessages = mergeMessages(messages)
        messagesByChat[chat] = mergedMessages
        if (_uiState.value.selectedChat == chat) {
            _uiState.value = _uiState.value.copy(messages = mergedMessages)
        }
    }

    private fun MessageDto.chatKey(): String? {
        return when {
            to?.endsWith("@channel") == true -> to
            from == repository.currentLogin -> to
            else -> from
        }
    }

    private fun mergeMessages(messages: List<MessageDto>): List<MessageDto> {
        val serverMessageKeys = messages
            .filterNot { it.id?.startsWith("local-") == true }
            .map { it.contentKey() }
            .toSet()
        return messages
            .filterNot { it.id?.startsWith("local-") == true && it.contentKey() in serverMessageKeys }
            .distinctBy { message ->
                message.id ?: message.contentKey()
            }
            .sortedWith(compareBy<MessageDto> { it.sortTime() }.thenBy { it.sortId() })
    }

    private fun MessageDto.contentKey(): List<String?> {
        return listOf(
            from,
            to,
            time,
            data.text?.text,
            data.image?.link
        )
    }

    private fun List<MessageDto>.oldestMessageId(): String? {
        return minByOrNull { it.sortId() }?.id
    }

    private fun MessageDto.sortTime(): Long {
        return time?.toLongOrNull() ?: localTimestamp() ?: Long.MAX_VALUE
    }

    private fun MessageDto.sortId(): Long {
        return id?.toLongOrNull() ?: localTimestamp() ?: Long.MAX_VALUE
    }

    private fun MessageDto.localTimestamp(): Long? {
        return id
            ?.removePrefix("local-")
            ?.removePrefix("image-")
            ?.toLongOrNull()
    }

    private fun handleFailure(error: Throwable) {
        if (error is UnauthorizedException) {
            _uiState.value = _uiState.value.copy(unauthorized = true)
        } else {
            _uiState.value = _uiState.value.copy(error = error.message)
        }
    }
}


