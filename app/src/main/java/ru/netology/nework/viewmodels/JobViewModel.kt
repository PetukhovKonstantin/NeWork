package ru.netology.nework.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.utils.ConstantValues
import ru.netology.nework.dto.*
import ru.netology.nework.repositories.Repository
import javax.inject.Inject

@HiltViewModel
class JobViewModel @Inject constructor(
    application: Application,
    private val repository: Repository,
    private val appAuth: AppAuth
) : AndroidViewModel(application) {
    val data: Flow<List<Job>>
        get() = flow {
            while (true) {
                loadAllJobs()
                emit(_data)
                delay(1_000)
            }
        }

    private var _data: List<Job> = listOf()

    private val edited = MutableLiveData(ConstantValues.emptyJob)


    init {
        loadMyJobs()
    }

    private fun loadAllJobs() = viewModelScope.launch {
        repository.dataJobs.collectLatest {
            _data = it
        }
    }

    fun loadMyJobs() = viewModelScope.launch {
        try {
            repository.getMyJobs(appAuth.authStateFlow.value.id)
        } catch (_: Exception) {
        }

        repository.dataJobs.collectLatest { listAllJob ->
            _data = listAllJob.filter { job ->
                job.ownerId == appAuth.authStateFlow.value.id
            }
        }
    }

    fun loadUserJobs(id: Long) = viewModelScope.launch {
        try {
            repository.getJobs(id)
        } catch (_: Exception) {
        }

        repository.dataJobs.collectLatest { listAllJob ->
            _data = listAllJob.filter { job ->
                job.ownerId == id
            }
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            repository.removeJobById(id)
        } catch (_: Exception) {
        }
    }

    fun edit(job: Job) {
        edited.value = job
    }

    fun getEditedId(): Long {
        return edited.value?.id ?: 0
    }

    fun changeContent(
        name: String,
        position: String,
        start: String,
        finish: String?,
        link: String?
    ) {
        if (edited.value?.name == name
            && edited.value?.position == position
            && edited.value?.start == start
            && edited.value?.finish == finish
            && edited.value?.link == link
        ) return
        edited.value = edited.value?.copy(
            name = name,
            position = position,
            start = start,
            finish = finish,
            link = link
        )
    }

    fun save() {
        viewModelScope.launch {
            try {
                edited.value?.let {
                    repository.saveJob(it)
                }
            } catch (msg: Exception) { }
        }
        edited.value = ConstantValues.emptyJob
    }
}