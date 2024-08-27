package ru.netology.nework.activities

import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
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
import ru.netology.nework.adapters.JobAdapter
import ru.netology.nework.adapters.OnInteractionListenerJob
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentJobsBinding
import ru.netology.nework.dto.Job
import ru.netology.nework.utils.Companion.Companion.textArg
import ru.netology.nework.viewmodels.AuthViewModel
import ru.netology.nework.viewmodels.JobViewModel
import javax.inject.Inject

@AndroidEntryPoint
class JobsFragment : Fragment() {
    companion object {
        private const val ARG_FROM_PAGER = "fromPager"
        private const val ARG_ID_USER = "userId"

        fun newInstance(fromPager: Boolean, userId: Long): JobsFragment {
            val fragment = JobsFragment()
            val args = Bundle()
            args.putBoolean(ARG_FROM_PAGER, fromPager)
            args.putLong(ARG_ID_USER, userId)
            fragment.arguments = args
            return fragment
        }
    }

    val viewModel: JobViewModel by activityViewModels()
    val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var appAuth: AppAuth

    private val mediaPlayer = MediaPlayer()

    private var isProfileFragment = false

    private val interactionListener = object : OnInteractionListenerJob {
        override fun onRemove(job: Job) {
            viewModel.removeById(job.id)
        }

        override fun onEdit(job: Job) {
            viewModel.edit(job)
            findNavController().navigate(if (isProfileFragment) R.id.action_profileFragment_to_newJobFragment else R.id.action_jobsFragment_to_newJobFragment)
        }

        override fun onLink(job: Job) {
            val intent =
                if (job.link?.contains("https://") == true || job.link?.contains("http://") == true) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(job.link))
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("http://${job.link}"))
                }
            startActivity(intent)
        }

        override fun myOrNo(job: Job): Boolean {
            return job.ownerId == appAuth.authStateFlow.value.id
        }
    }

    private lateinit var binding: FragmentJobsBinding
    private lateinit var adapter: JobAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        isProfileFragment = arguments?.getBoolean(ARG_FROM_PAGER) ?: false

        binding = FragmentJobsBinding.inflate(layoutInflater)
        adapter = JobAdapter(interactionListener)


        val userId = arguments?.getLong(ARG_ID_USER) ?: 0L

        binding.list.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.data.collectLatest {
                    adapter.submitList(if (isProfileFragment) it.filter { job -> job.ownerId == userId } else it)
                }
            }
        }

        if (isProfileFragment) {
            binding.mainNavView.visibility = View.INVISIBLE
            if (userId != appAuth.authStateFlow.value.id) {
                binding.fab.visibility = View.GONE
            }
        }

        var menuProvider: MenuProvider? = null

        if (!isProfileFragment) {
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
                                    R.id.action_jobsFragment_to_authFragment,
                                    Bundle().apply {
                                        textArg = getString(R.string.sign_in)
                                    }
                                )
                                true
                            }

                            R.id.signup -> {
                                findNavController().navigate(
                                    R.id.action_jobsFragment_to_authFragment,
                                    Bundle().apply {
                                        textArg = getString(R.string.sign_up)
                                    }
                                )
                                true
                            }

                            R.id.profile -> {
                                findNavController().navigate(R.id.action_jobsFragment_to_profileFragment)
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
        }

        binding.mainNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_posts -> {
                    findNavController().navigate(R.id.action_jobsFragment_to_feedFragment)
                    true
                }

                R.id.navigation_events -> {
                    findNavController().navigate(R.id.action_jobsFragment_to_eventsFragment)
                    true
                }

                R.id.navigation_users -> {
                    findNavController().navigate(R.id.action_jobsFragment_to_usersFragment)
                    true
                }
                else -> false
            }
        }

        binding.fab.setOnClickListener {
            if (authViewModel.authenticated) {
                findNavController().navigate(if (isProfileFragment) R.id.action_profileFragment_to_newJobFragment else R.id.action_jobsFragment_to_newJobFragment)
            } else {
                AlertDialog.Builder(context)
                    .setMessage(R.string.action_not_allowed)
                    .setPositiveButton(R.string.sign_up) { _, _ ->
                        findNavController().navigate(
                            if (isProfileFragment) R.id.action_profileFragment_to_authFragment else R.id.action_jobsFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_up)
                            }
                        )
                    }
                    .setNeutralButton(R.string.sign_in) { _, _ ->
                        findNavController().navigate(
                            if (isProfileFragment) R.id.action_profileFragment_to_authFragment else R.id.action_jobsFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_in)
                            }
                        )
                    }
                    .setNegativeButton(R.string.no, null)
                    .setCancelable(true)
                    .create()
                    .show()
            }
        }

        binding.swipe.setOnRefreshListener {
            viewModel.loadMyJobs()
            binding.swipe.isRefreshing = false
        }

        return binding.root
    }

    override fun onDestroyView() {
        mediaPlayer.release()
        super.onDestroyView()
    }
}