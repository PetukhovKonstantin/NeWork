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
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
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
import ru.netology.nework.adapters.EventAdapter
import ru.netology.nework.adapters.OnInteractionListenerEvent
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.EventResponse
import ru.netology.nework.dto.Post
import ru.netology.nework.utils.Companion.Companion.eventId
import ru.netology.nework.utils.Companion.Companion.eventRequestType
import ru.netology.nework.utils.Companion.Companion.textArg
import ru.netology.nework.utils.Companion.Companion.userId
import ru.netology.nework.utils.FloatingValue.currentFragment
import ru.netology.nework.viewmodels.AuthViewModel
import ru.netology.nework.viewmodels.EventViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventsFragment : Fragment() {
    companion object {
        private const val ARG_FROM_PAGER = "fromPager"
        private const val ARG_ID_USER = "userId"

        fun newInstance(fromPager: Boolean, userId: Long): EventsFragment {
            val fragment = EventsFragment()
            val args = Bundle()
            args.putBoolean(ARG_FROM_PAGER, fromPager)
            args.putLong(ARG_ID_USER, userId)
            fragment.arguments = args
            return fragment
        }
    }

    val viewModel: EventViewModel by activityViewModels()
    val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var appAuth: AppAuth

    val mediaPlayer = MediaPlayer()

    private var isProfileFragment = false

    private val interactionListener = object : OnInteractionListenerEvent {
        override fun onTapAvatar(event: EventResponse) {
            if (!isProfileFragment) {
                findNavController().navigate(
                    R.id.action_eventsFragment_to_profileFragment,
                    Bundle().apply {
                        userId = event.authorId
                    }
                )
            }
        }

        override fun onLike(event: EventResponse) {
            if (authViewModel.authenticated) {
                viewModel.likeById(event)
            } else {
                AlertDialog.Builder(context)
                    .setMessage(R.string.action_not_allowed)
                    .setPositiveButton(R.string.sign_up) { _, _ ->
                        findNavController().navigate(
                            if (isProfileFragment) R.id.action_profileFragment_to_authFragment else R.id.action_eventsFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_up)
                            }
                        )
                    }
                    .setNeutralButton(R.string.sign_in) { _, _ ->
                        findNavController().navigate(
                            if (isProfileFragment) R.id.action_profileFragment_to_authFragment else R.id.action_eventsFragment_to_authFragment,
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

        override fun onShare(event: EventResponse) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, event.content)
            }

            val shareIntent =
                Intent.createChooser(intent, getString(R.string.chooser_share_post))
            startActivity(shareIntent)
        }

        override fun onRemove(event: EventResponse) {
            viewModel.removeById(event.id)
        }

        override fun onEdit(event: EventResponse) {
            viewModel.edit(event)
            findNavController().navigate(if (isProfileFragment) R.id.action_profileFragment_to_newEventFragment else R.id.action_eventsFragment_to_newEventFragment)
        }

        override fun onPlayPost(event: EventResponse, videoView: VideoView?) {
            if (event.attachment?.type == AttachmentType.VIDEO) {
                videoView?.isVisible = true
                val uri = Uri.parse(event.attachment.url)
                videoView?.apply {
                    setMediaController(MediaController(requireContext()))
                    setVideoURI(uri)
                    setOnPreparedListener {
                        videoView.layoutParams?.height =
                            (resources.displayMetrics.widthPixels * (it.videoHeight.toDouble() / it.videoWidth)).toInt()
                        start()
                    }
                    setOnCompletionListener {

                        if (videoView.layoutParams?.width != null) {
                            videoView.layoutParams?.width = resources.displayMetrics.widthPixels
                            videoView.layoutParams?.height =
                                (videoView.layoutParams?.width!! * 0.5625).toInt()
                        }
                        stopPlayback()

                    }

                }
            }
            if (event.attachment?.type == AttachmentType.AUDIO) {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                } else {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(event.attachment.url)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
            }
        }

        override fun onLink(event: EventResponse) {
            val intent =
                if (event.link?.contains("https://") == true || event.link?.contains("http://") == true) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(event.link))
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("http://${event.link}"))
                }
            startActivity(intent)
        }

        override fun onPreviewAttachment(event: EventResponse) {
            findNavController().navigate(
                if (isProfileFragment) R.id.action_profileFragment_to_viewImageAttach else R.id.action_eventsFragment_to_viewImageAttach,
                Bundle().apply {
                    textArg = event.attachment?.url
                })
        }

        override fun onSpeakersAction(event: EventResponse) {
            if (event.speakerIds.isNotEmpty()) {
                findNavController().navigate(
                    if (isProfileFragment) R.id.action_profileFragment_to_bottomSheetFragment else R.id.action_eventsFragment_to_bottomSheetFragment,
                    Bundle().apply {
                        eventId = event.id
                        eventRequestType = "speakers"
                    }
                )
            } else {
                Toast.makeText(requireContext(), R.string.not_value_event, Toast.LENGTH_SHORT)
                    .show()
            }

        }

        override fun onPartyAction(event: EventResponse) {
            if (event.participantsIds.isNotEmpty()) {
                findNavController().navigate(
                    if (isProfileFragment) R.id.action_profileFragment_to_bottomSheetFragment else R.id.action_eventsFragment_to_bottomSheetFragment,
                    Bundle().apply {
                        eventId = event.id
                        eventRequestType = "party"
                    }
                )
            } else {
                Toast.makeText(requireContext(), R.string.not_value_event, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        override fun onJoinAction(event: EventResponse) {
            viewModel.joinById(event)
        }

        override fun onEventOpen(event: EventResponse) {
            findNavController().navigate(
                if (isProfileFragment) R.id.action_profileFragment_to_cardEventFragment else R.id.action_eventsFragment_to_cardEventFragment,
                Bundle().apply { textArg = event.id.toString() })
        }
    }

    private lateinit var binding: FragmentEventsBinding
    private lateinit var adapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentEventsBinding.inflate(layoutInflater)
        adapter = EventAdapter(interactionListener)

        isProfileFragment = arguments?.getBoolean(ARG_FROM_PAGER) ?: false
        val userId = arguments?.getLong(ARG_ID_USER) ?: 0L

        binding.list.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.data.collectLatest {
                    adapter.submitList(if (isProfileFragment) it.filter { event -> event.authorId == userId } else it)
                }
            }
        }

        if (isProfileFragment) {
            binding.mainNavView.visibility = View.GONE
            binding.fab.visibility = View.GONE
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
                                    R.id.action_feedFragment_to_authFragment,
                                    Bundle().apply {
                                        textArg = getString(R.string.sign_in)
                                    }
                                )
                                true
                            }

                            R.id.signup -> {
                                findNavController().navigate(
                                    R.id.action_feedFragment_to_authFragment,
                                    Bundle().apply {
                                        textArg = getString(R.string.sign_up)
                                    }
                                )
                                true
                            }

                            R.id.profile -> {
                                findNavController().navigate(R.id.action_eventsFragment_to_profileFragment)
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

        binding.mainNavView.selectedItemId = R.id.navigation_events

        binding.mainNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_posts -> {
                    findNavController().navigate(R.id.action_eventsFragment_to_feedFragment)
                    true
                }

                R.id.navigation_users -> {
                    findNavController().navigate(R.id.action_eventsFragment_to_usersFragment)
                    true
                }

                else -> false
            }
        }

        binding.fab.setOnClickListener {
            if (authViewModel.authenticated) {
                findNavController().navigate(R.id.action_eventsFragment_to_newEventFragment)
            } else {
                AlertDialog.Builder(context)
                    .setMessage(R.string.action_not_allowed)
                    .setPositiveButton(R.string.sign_up) { _, _ ->
                        findNavController().navigate(
                            R.id.action_eventsFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_up)
                            }
                        )
                    }
                    .setNeutralButton(R.string.sign_in) { _, _ ->
                        findNavController().navigate(
                            R.id.action_eventsFragment_to_authFragment,
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
            viewModel.loadEvents()
            binding.swipe.isRefreshing = false
        }

        return binding.root
    }

    override fun onDestroyView() {
        mediaPlayer.release()
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startLoadingEvents()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopLoadingEvents()
    }
}