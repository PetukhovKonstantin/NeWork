package ru.netology.nework.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Token
import ru.netology.nework.models.MediaModel
import ru.netology.nework.utils.ConstantValues.noPhoto
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val apiService: ApiService,
) : ViewModel() {
    val data: LiveData<AuthState> = appAuth
        .authStateFlow
        .asLiveData(Dispatchers.Default)
    val authenticated: Boolean
        get() = appAuth.authStateFlow.value.id != 0L

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<MediaModel>
        get() = _photo

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = MediaModel(uri, file)
    }
    private val _dataState = MutableLiveData(-1)
    val dataState: LiveData<Int>
        get() = _dataState

    init {
        _dataState.value = -1
    }

    suspend fun login(login: String, pass: String) {
        viewModelScope.launch {
            val token: Token
            try {
                val response = apiService.login(login, pass)

                if (!response.isSuccessful) {
                    _dataState.value = 1
                } else {
                    token = response.body() ?: Token(id = 0, token = "")
                    appAuth.setAuth(token.id, token.token, null)
                    _dataState.value = 0
                }
            } catch (e: IOException) {
                _dataState.value = 2
            } catch (e: Exception) {
                _dataState.value = 3
            }
        }
    }

    suspend fun registerWithPhoto(login: String, pass: String, name: String, upload: MediaUpload?) {
        viewModelScope.launch {
            val token: Token
            try {
                val response = if (upload != null) {
                    apiService.registerWithPhoto(
                        login.toRequestBody("text/plain".toMediaType()),
                        pass.toRequestBody("text/plain".toMediaType()),
                        name.toRequestBody("text/plain".toMediaType()),
                        MultipartBody.Part.createFormData(
                            "file", upload.file.name, upload.file.asRequestBody()
                        )
                    )
                } else {
                    apiService.register(login,pass,name)
                }

                if (!response.isSuccessful) {
                    _dataState.value = 1

                    //throw ApiError(response.code(), response.message())
                } else {
                    token = response.body() ?: Token(id = 0, token = "")
                    appAuth.setAuth(token.id, token.token, null)
                    _dataState.value = 0
                }
            } catch (e: IOException) {
                _dataState.value = 2
            } catch (e: Exception) {
                _dataState.value = 3
            }
        }
    }
}