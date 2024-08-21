package ru.netology.nework.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapters.OnInteractionListenerUsers
import ru.netology.nework.adapters.UsersAdapter
import ru.netology.nework.utils.Companion.Companion.eventId
import ru.netology.nework.utils.Companion.Companion.eventRequestType
import ru.netology.nework.utils.Companion.Companion.userId
import ru.netology.nework.databinding.FragmentBottomSheetBinding
import ru.netology.nework.dto.User
import ru.netology.nework.viewmodels.EventViewModel
import ru.netology.nework.viewmodels.UsersViewModel

@AndroidEntryPoint
class BottomSheetFragment : BottomSheetDialogFragment() {

    private val usersViewModel: UsersViewModel by activityViewModels()
    val viewModel: EventViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentBottomSheetBinding.inflate(inflater, container, false)
        val adapter = UsersAdapter(object : OnInteractionListenerUsers {
            override fun onTap(user: User) {
                findNavController().navigate(
                    R.id.action_bottomSheetFragment_to_profileFragment,
                    Bundle().apply { userId = user.id }
                )
            }
        })
        var filteredList: List<Long> = emptyList()
        binding.list.adapter = adapter
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.data.collectLatest {
                    when (arguments?.eventRequestType) {
                        "speakers" -> filteredList = it.find { event ->
                            event.id == arguments?.eventId
                        }?.speakerIds ?: emptyList()

                        "party" -> filteredList = it.find { event ->
                            event.id == arguments?.eventId
                        }?.participantsIds ?: emptyList()

                        else -> emptyList<Long>()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                usersViewModel.dataUsersList.collectLatest {
                    adapter.submitList(it.filter { user ->
                        filteredList.contains(user.id)
                    })
                }
            }
        }
        return binding.root
    }
}