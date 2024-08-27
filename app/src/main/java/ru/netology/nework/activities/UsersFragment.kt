package ru.netology.nework.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapters.OnInteractionListenerUsers
import ru.netology.nework.adapters.UsersAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentUsersBinding
import ru.netology.nework.dto.User
import ru.netology.nework.utils.Companion.Companion.textArg
import ru.netology.nework.utils.Companion.Companion.userId
import ru.netology.nework.utils.FloatingValue.currentFragment
import ru.netology.nework.viewmodels.AuthViewModel
import ru.netology.nework.viewmodels.EventViewModel
import ru.netology.nework.viewmodels.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UsersFragment : Fragment() {
    val viewModel: UsersViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentUsersBinding.inflate(layoutInflater)

        val authViewModel: AuthViewModel by viewModels()

        var menuProvider: MenuProvider? = null

        val adapter = UsersAdapter(
            object : OnInteractionListenerUsers {
                override fun onTap(user: User) {
                    findNavController().navigate(
                        R.id.action_usersFragment_to_profileFragment,
                        Bundle().apply {
                            userId = user.id
                        }
                    )
                }
            }
        )

        authViewModel.data.observe(viewLifecycleOwner) {
            menuProvider?.let(requireActivity()::removeMenuProvider)
            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_main, menu)

                    menu.setGroupVisible(R.id.unauthenticated, !authViewModel.authenticated)
                    menu.setGroupVisible(R.id.authenticated, authViewModel.authenticated)

                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.signin -> {
                            findNavController().navigate(
                                R.id.action_usersFragment_to_authFragment,
                                Bundle().apply {
                                    textArg = getString(R.string.sign_in)
                                }
                            )
                            true
                        }

                        R.id.signup -> {
                            findNavController().navigate(
                                R.id.action_usersFragment_to_authFragment,
                                Bundle().apply {
                                    textArg = getString(R.string.sign_up)
                                }
                            )
                            true
                        }

                        R.id.profile -> {
                            findNavController().navigate(R.id.action_usersFragment_to_profileFragment)
                            true
                        }

                        R.id.signout -> {
                            AlertDialog.Builder(requireActivity())
                                .setTitle(R.string.are_you_suare)
                                .setPositiveButton(R.string.yes) { _, _ ->
                                    appAuth.removeAuth()
                                }
                                .setCancelable(true)
                                .setNegativeButton(R.string.no, null)
                                .create()
                                .show()
                            true
                        }

                        else -> false
                    }
                }
            }.apply {
                menuProvider = this
            }, viewLifecycleOwner)
        }

        binding.mainNavView.selectedItemId = R.id.navigation_users

        binding.mainNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_posts -> {
                    findNavController().navigate(R.id.action_usersFragment_to_feedFragment)
                    true
                }

                R.id.navigation_events -> {
                    findNavController().navigate(R.id.action_usersFragment_to_eventsFragment)
                    true
                }

                else -> false
            }
        }

        binding.listUsers.adapter = adapter
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.dataUsersList.collectLatest {
                    adapter.submitList(it)
                }
            }
        }
        return binding.root
    }

    override fun onStart() {
        currentFragment = javaClass.simpleName
        viewModel.startLoadingUsers()
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopLoadingUsers()
    }
}