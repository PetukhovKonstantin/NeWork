package ru.netology.nework.viewmodels

import android.app.Application
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.netology.nework.utils.ConstantValues.emptyEvent
import ru.netology.nework.utils.ConstantValues.noPhoto
import ru.netology.nework.dto.*
import ru.netology.nework.models.MediaModel
import ru.netology.nework.repositories.*
import ru.netology.nework.utils.SingleLiveEvent
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    application: Application,
    private val repository: Repository,
) : AndroidViewModel(application) {

    val data: Flow<List<EventResponse>>
        get() = repository.dataEvents.map { events ->
            _data = events
            _data
        }.distinctUntilChanged()

    private var _data: List<EventResponse> = listOf()

    private val edited = MutableLiveData(emptyEvent)
    private val _eventCreated = SingleLiveEvent<Unit>()
    val eventCreated: LiveData<Unit>
        get() = _eventCreated

    private val _media = MutableLiveData(
        MediaModel(
            edited.value?.attachment?.url?.toUri(),
            edited.value?.attachment?.url?.toUri()?.toFile(),
            edited.value?.attachment?.type
        )
    )
    val media: LiveData<MediaModel>
        get() = _media

    fun changeMedia(uri: Uri?, file: File?, attachmentType: AttachmentType?) {
        _media.value = MediaModel(uri, file, attachmentType)
    }

    private var eventJob: kotlinx.coroutines.Job? = null

    fun startLoadingEvents() {
        eventJob = viewModelScope.launch {
            while (isActive) {
                loadEvents()
                delay(1_000)
            }
        }
    }

    fun stopLoadingEvents() {
        eventJob?.cancel()
    }

    init {
        loadEvents()
    }

    fun loadEvents() = viewModelScope.launch {
        try {
            repository.getAllEvents()
        } catch (_: Exception) {
        }

        repository.dataEvents.collectLatest {
            _data = it
        }
    }

    fun likeById(eventResponse: EventResponse) = viewModelScope.launch {
        try {
            repository.likeByIdEvents(eventResponse)
        } catch (_: Exception) {
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            repository.removeEventsById(id)
        } catch (_: Exception) {
        }
    }

    fun edit(eventResponse: EventResponse) {
        edited.value = eventResponse
    }

    fun getEditedId(): Long {
        return edited.value?.id ?: 0
    }

    fun getEditedEventAttachment(): Attachment? {
        return edited.value?.attachment
    }

    fun changeContent(content: String, link: String?, datetime:String, type: EventType, speakerIds: List<Long>) {
        val text = content.trim()
        if (edited.value?.content == text && edited.value?.link == link && edited.value?.datetime == datetime && edited.value?.type == type && edited.value?.speakerIds == speakerIds) return
        edited.value = edited.value?.copy(content = text, link = link, datetime = datetime, type=type, speakerIds = speakerIds)
    }

    fun deleteAttachment() {
        edited.value = edited.value?.copy(attachment = null)
    }

    fun joinById(eventResponse: EventResponse) = viewModelScope.launch {
        try {
            repository.joinByIdEvents(eventResponse)
        } catch (_: Exception) {
        }
    }

    fun save() {
        edited.value?.let { savingEvents ->
            _eventCreated.value = Unit
            viewModelScope.launch {
                try {
                    when (_media.value) {
                        noPhoto -> repository.saveEvents(savingEvents)
                        else -> _media.value?.file?.let { file ->
                            repository.saveEventsWithAttachment(
                                savingEvents,
                                MediaUpload(file),
                                _media.value!!.attachmentType!!
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
        edited.value = emptyEvent
        _media.value = noPhoto
    }

    fun changeCoords(coords: Coords?) {
        edited.value = edited.value?.copy(coords = coords)
    }
}