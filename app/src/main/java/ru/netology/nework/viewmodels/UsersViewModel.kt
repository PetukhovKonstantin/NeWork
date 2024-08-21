package ru.netology.nework.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.netology.nework.dto.EventResponse
import ru.netology.nework.dto.User
import ru.netology.nework.repositories.Repository
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    application: Application,
    private val repository: Repository,
) : AndroidViewModel(application) {
//    val dataUsersList
//        get() = flow {
//            while (true) {
//                getData()
//                emit(_dataUsersList)
//                delay(1_000)
//            }
//        }

    val dataUsersList
        get() = repository.dataUsers.map { users ->
            _dataUsersList = users
            _dataUsersList
        }.distinctUntilChanged()

    private var _dataUsersList: List<User> = listOf()

    private var userJob: kotlinx.coroutines.Job? = null

    fun startLoadingUsers() {
        userJob = viewModelScope.launch {
            while (isActive) {
                getData()
                delay(1_000)
            }
        }
    }

    fun stopLoadingUsers() {
        userJob?.cancel()
    }

    private fun getData() = viewModelScope.launch  {
        try {
            repository.getUsers()
        } catch (_: Exception) { }

        repository.dataUsers.collectLatest {
            _dataUsersList = it
        }
    }
}