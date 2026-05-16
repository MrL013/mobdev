package ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import data.ChatRepository

data class LoginUiState(
    val success: Boolean = false,
    val loading: Boolean = false,
    val errorRes: Int? = null
)

class LoginViewModel(
    private val repository: ChatRepository,
    private val hasSavedCredentials: Boolean
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        if (hasSavedCredentials) {
            _uiState.value = LoginUiState(loading = true)
            viewModelScope.launch {
                repository.loginWithSavedCredentials()
                    .onSuccess { _uiState.value = LoginUiState(success = true) }
                    .onFailure { _uiState.value = LoginUiState(errorRes = null) }
            }
        }
    }

    fun login(login: String, password: String) {
        _uiState.value = LoginUiState(loading = true)
        viewModelScope.launch {
            repository.login(login, password)
                .onSuccess { _uiState.value = LoginUiState(success = true) }
                .onFailure { _uiState.value = LoginUiState(success = false, errorRes = lab3.chat.R.string.invalid_credentials) }
        }
    }
}

class LoginViewModelFactory(
    private val repository: ChatRepository,
    private val hasSavedCredentials: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LoginViewModel(repository, hasSavedCredentials) as T
    }
}


