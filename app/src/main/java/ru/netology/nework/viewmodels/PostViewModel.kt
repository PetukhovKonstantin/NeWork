package ru.netology.nework.viewmodels

import android.app.Application
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.utils.ConstantValues.emptyPost
import ru.netology.nework.utils.ConstantValues.noPhoto
import ru.netology.nework.dto.*
import ru.netology.nework.models.FeedModel
import ru.netology.nework.models.FeedModelState
import ru.netology.nework.models.MediaModel
import ru.netology.nework.repositories.*
import ru.netology.nework.utils.SingleLiveEvent
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    application: Application,
    private val repository: Repository,
    private val appAuth: AppAuth
) : AndroidViewModel(application) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val data: LiveData<FeedModel>
        get() = appAuth
            .authStateFlow
            .flatMapLatest { (myId, _) ->
                repository.data
                    .map { posts ->
                        FeedModel(
                            posts.map { it.copy(ownedByMe = it.authorId == myId) },
                            posts.isEmpty()
                        )
                    }
            }.asLiveData(Dispatchers.Default)

    private val _dataState = MutableLiveData<FeedModelState>(FeedModelState.Idle)
    val dataState: LiveData<FeedModelState>
        get() = _dataState
    private val edited = MutableLiveData(emptyPost)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _media = MutableLiveData(
        MediaModel(
            edited.value?.attachment?.url?.toUri(),
            edited.value?.attachment?.url?.toUri()?.toFile(),
            edited.value?.attachment?.type
        )
    )
    val media: LiveData<MediaModel>
        get() = _media

    val newerCount: LiveData<Int> = data.switchMap {
        repository.getNewerCount(it.posts.firstOrNull()?.id ?: 0L)
            .catch { e -> e.printStackTrace() }
            .asLiveData(Dispatchers.Default)
    }

    fun changeMedia(uri: Uri?, file: File?, attachmentType: AttachmentType?) {
        _media.value = MediaModel(uri, file, attachmentType)
    }


    init {
        loadPosts()
    }

    fun viewNewPosts() = viewModelScope.launch {
        try {
            repository.showNewPosts()
            _dataState.value = FeedModelState.ShadowIdle
        } catch (e: Exception) {
            _dataState.value = FeedModelState.Error
        }
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState.Loading
            repository.getAllAsync()
            _dataState.value = FeedModelState.ShadowIdle
        } catch (e: Exception) {
            _dataState.value = FeedModelState.Error
        }
    }

    fun refreshPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState.Refresh
            repository.getAllAsync()
            _dataState.value = FeedModelState.ShadowIdle
        } catch (e: Exception) {
            _dataState.value = FeedModelState.Error
        }
    }

    fun likeById(post: Post) {
        viewModelScope.launch {
            try {
                repository.likeByIdAsync(post)
            } catch (e: Exception) {
                _dataState.value = FeedModelState.Error
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeByIdAsync(id)
                _dataState.value = FeedModelState.Idle
            } catch (e: Exception) {
                _dataState.value = FeedModelState.Error
            }
        }
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun getEditedId(): Long {
        return edited.value?.id ?: 0
    }

    fun getEditedPostAttachment(): Attachment? {
        return edited.value?.attachment
    }

    fun changeContent(content: String, link: String?, mentionsIds:List<Long>) {
        val text = content.trim()
        if (edited.value?.content == text && edited.value?.link == link && edited.value?.mentionIds == mentionsIds) return
        edited.value = edited.value?.copy(content = text, link = link, mentionIds = mentionsIds)
    }

    fun deleteAttachment() {
        edited.value = edited.value?.copy(attachment = null)
    }

    fun save() {
        edited.value?.let { savingPost ->
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    when (_media.value) {
                        noPhoto -> repository.saveAsync(savingPost)
                        else -> _media.value?.file?.let { file ->
                            repository.saveWithAttachment(savingPost, MediaUpload(file), _media.value!!.attachmentType!!)
                        }
                    }
                    _dataState.value = FeedModelState.Idle
                } catch (e: Exception) {
                    _dataState.value = FeedModelState.Error
                }
            }
        }
        edited.value = emptyPost
        _media.value = noPhoto
    }

    fun getEditedPostCoords(): Coords? {
        return edited.value?.coords
    }

    fun changeCoords(coords: Coords?) {
        edited.value = edited.value?.copy(coords = coords)
    }
}