package vibe.ccc.aichat.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vibe.ccc.aichat.data.auth.AuthStore
import vibe.ccc.aichat.data.auth.TestAccount
import vibe.ccc.aichat.data.network.APIClient

data class LoginUiState(
    val phoneNumber: String = "",
    val verifyCode: String = "",
    val errorMessage: String? = null,
    val isSendingCode: Boolean = false,
    val isLoggingIn: Boolean = false,
    val retryAfter: Int = 0
) {
    val canSendCode: Boolean
        get() = (TestAccount.isPhoneNumber(phoneNumber) || (phoneNumber.length == 11 && phoneNumber.all { it.isDigit() })) &&
            !isSendingCode &&
            retryAfter == 0

    val canLogin: Boolean
        get() = !isLoggingIn &&
            (
                TestAccount.matches(phoneNumber, verifyCode) ||
                    (phoneNumber.length == 11 && phoneNumber.all { it.isDigit() } && verifyCode.length >= 4)
                )
}

class LoginViewModel(private val apiClient: APIClient) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    private var countdownJob: Job? = null

    fun updatePhoneNumber(value: String) {
        _state.update { it.copy(phoneNumber = value.filter(Char::isDigit).take(11), errorMessage = null) }
    }

    fun updateVerifyCode(value: String) {
        _state.update { it.copy(verifyCode = value.filter(Char::isDigit).take(4), errorMessage = null) }
    }

    fun sendCode() {
        val current = _state.value
        if (!current.canSendCode && !TestAccount.isPhoneNumber(current.phoneNumber)) {
            _state.update { it.copy(errorMessage = "请输入 11 位中国大陆手机号") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSendingCode = true, errorMessage = null) }
            try {
                val result = apiClient.sendCode(phoneNumber = _state.value.phoneNumber)
                startCountdown(result.retryAfter)
            } catch (error: Exception) {
                _state.update { it.copy(errorMessage = error.localizedMessage ?: "发送验证码失败") }
            } finally {
                _state.update { it.copy(isSendingCode = false) }
            }
        }
    }

    fun login(authStore: AuthStore, onSuccess: () -> Unit) {
        if (!_state.value.canLogin) {
            _state.update { it.copy(errorMessage = "请输入手机号和验证码") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoggingIn = true, errorMessage = null) }
            try {
                val current = _state.value
                val session = apiClient.login(
                    phoneNumber = current.phoneNumber,
                    verifyCode = current.verifyCode
                )
                authStore.update(session)
                onSuccess()
            } catch (error: Exception) {
                _state.update { it.copy(errorMessage = error.localizedMessage ?: "登录失败") }
            } finally {
                _state.update { it.copy(isLoggingIn = false) }
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        _state.update { it.copy(retryAfter = seconds.coerceAtLeast(0)) }
        if (_state.value.retryAfter <= 0) return

        countdownJob = viewModelScope.launch {
            while (_state.value.retryAfter > 0) {
                delay(1000)
                _state.update { it.copy(retryAfter = (it.retryAfter - 1).coerceAtLeast(0)) }
            }
        }
    }
}
