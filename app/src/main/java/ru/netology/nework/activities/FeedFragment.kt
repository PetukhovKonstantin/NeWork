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
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapters.OnInteractionListener
import ru.netology.nework.adapters.PostAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Post
import ru.netology.nework.models.FeedModelState
import ru.netology.nework.utils.Companion.Companion.linkArg
import ru.netology.nework.utils.Companion.Companion.mentionsCountArg
import ru.netology.nework.utils.Companion.Companion.textArg
import ru.netology.nework.utils.Companion.Companion.userId
import ru.netology.nework.viewmodels.AuthViewModel
import ru.netology.nework.viewmodels.PostViewModel
import ru.netology.nework.viewmodels.UsersViewModel
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class FeedFragment : Fragment() {

    companion object {
        private const val ARG_FROM_PAGER = "fromPager"
        private const val ARG_ID_USER = "userId"

        fun newInstance(fromPager: Boolean, userId: Long): FeedFragment {
            val fragment = FeedFragment()
            val args = Bundle()
            args.putBoolean(ARG_FROM_PAGER, fromPager)
            args.putLong(ARG_ID_USER, userId)
            fragment.arguments = args
            return fragment
        }
    }

    val viewModel: PostViewModel by activityViewModels()
    val authViewModel: AuthViewModel by viewModels()

    private val usersViewModel: UsersViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

    private val mediaPlayer = MediaPlayer()

    private var isProfileFragment = false

    private val interactionListener = object : OnInteractionListener {
        override fun onTapAvatar(post: Post) {
            if (!isProfileFragment) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_profileFragment,
                    Bundle().apply {
                        userId = post.authorId
                    }
                )
            }
        }

        override fun onLike(post: Post) {
            if (authViewModel.authenticated) {
                viewModel.likeById(post)
            } else {
                AlertDialog.Builder(context)
                    .setMessage(R.string.action_not_allowed)
                    .setPositiveButton(R.string.sign_up) { _, _ ->
                        findNavController().navigate(
                            if (isProfileFragment) R.id.action_profileFragment_to_authFragment else R.id.action_feedFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_up)
                            }
                        )
                    }
                    .setNeutralButton(R.string.sign_in) { _, _ ->
                        findNavController().navigate(
                            if (isProfileFragment) R.id.action_profileFragment_to_authFragment else R.id.action_feedFragment_to_authFragment,
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

        override fun onShare(post: Post) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, post.content)
            }

            val shareIntent =
                Intent.createChooser(intent, getString(R.string.chooser_share_post))
            startActivity(shareIntent)
        }

        override fun onRemove(post: Post) {
            viewModel.removeById(post.id)
        }

        override fun onEdit(post: Post) {
            viewModel.edit(post)
            findNavController().navigate(
                if (isProfileFragment) R.id.action_profileFragment_to_newPostFragment else R.id.action_feedFragment_to_newPostFragment,
                Bundle().apply {
                    textArg = post.content
                    linkArg = post.link
                    mentionsCountArg = post.mentionIds?.size?.toLong() ?: 0L
                })

        }

        override fun onPlayPost(post: Post, videoView: VideoView?) {
            if (post.attachment?.type == AttachmentType.VIDEO) {
                videoView?.isVisible = true
                val uri = Uri.parse(post.attachment.url)
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
            if (post.attachment?.type == AttachmentType.AUDIO) {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                } else {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(post.attachment.url)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
            }
        }

        override fun onLink(post: Post) {
            val intent =
                if (post.link?.contains("https://") == true || post.link?.contains("http://") == true) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(post.link))
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("http://${post.link}"))
                }
            startActivity(intent)
        }

        override fun onPreviewAttachment(post: Post) {
            findNavController().navigate(
                if (isProfileFragment) R.id.action_profileFragment_to_viewImageAttach else R.id.action_feedFragment_to_viewImageAttach,
                Bundle().apply {
                    textArg = post.attachment?.url
                })
        }

        override fun onOpenPost(post: Post) {
            findNavController().navigate(
                if (isProfileFragment) R.id.action_profileFragment_to_cardPostFragment else R.id.action_feedFragment_to_cardPostFragment,
                Bundle().apply { textArg = post.id.toString() })
        }
    }

    private lateinit var binding: FragmentFeedBinding
    private lateinit var adapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentFeedBinding.inflate(layoutInflater)
        adapter = PostAdapter(interactionListener)

        isProfileFragment = arguments?.getBoolean(ARG_FROM_PAGER) ?: false
        val userId = arguments?.getLong(ARG_ID_USER) ?: 0L

        binding.list.adapter = adapter

        usersViewModel.dataUsersList

        viewModel.data.observe(viewLifecycleOwner) {
            adapter.submitList(if (isProfileFragment) it.posts.filter { post -> post.authorId == userId } else it.posts)
            binding.emptyText.isVisible = it.empty
        }

        viewModel.dataState.observe(viewLifecycleOwner) {
            binding.progress.isVisible = it is FeedModelState.Loading
            binding.swipe.isRefreshing = it is FeedModelState.Refresh
            if (it is FeedModelState.Error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.loadPosts() }
                    .show()
            }
            if (it is FeedModelState.Idle) {
                Toast.makeText(context, R.string.on_success, Toast.LENGTH_SHORT).show()
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
                                    if (isProfileFragment) R.id.action_profileFragment_to_authFragment else R.id.action_feedFragment_to_authFragment,
                                    Bundle().apply {
                                        textArg = getString(R.string.sign_in)
                                    }
                                )
                                true
                            }

                            R.id.signup -> {
                                findNavController().navigate(
                                    if (isProfileFragment) R.id.action_profileFragment_to_authFragment else R.id.action_feedFragment_to_authFragment,
                                    Bundle().apply {
                                        textArg = getString(R.string.sign_up)
                                    }
                                )
                                true
                            }

                            R.id.profile -> {
                                findNavController().navigate(R.id.action_feedFragment_to_profileFragment)
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

        binding.mainNavView.selectedItemId = R.id.navigation_posts

        binding.mainNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_events -> {
                    findNavController().navigate(R.id.action_feedFragment_to_eventsFragment)
                    true
                }

                R.id.navigation_users -> {
                    findNavController().navigate(R.id.action_feedFragment_to_usersFragment)
                    true
                }

                else -> false
            }
        }

        binding.fab.setOnClickListener {
            if (binding.fab.visibility == View.VISIBLE) {
                if (authViewModel.authenticated) {
                    findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
                } else {
                    AlertDialog.Builder(context)
                        .setMessage(R.string.action_not_allowed)
                        .setPositiveButton(R.string.sign_up) { _, _ ->
                            findNavController().navigate(
                                R.id.action_feedFragment_to_authFragment,
                                Bundle().apply {
                                    textArg = getString(R.string.sign_up)
                                }
                            )
                        }
                        .setNeutralButton(R.string.sign_in) { _, _ ->
                            findNavController().navigate(
                                R.id.action_feedFragment_to_authFragment,
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
        }
        binding.retryButton.setOnClickListener {
            viewModel.loadPosts()
        }

        binding.swipe.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        binding.newerCount.setOnClickListener {
            binding.newerCount.isVisible = false
            CoroutineScope(EmptyCoroutineContext).launch {
                launch {
                    viewModel.viewNewPosts()
                    delay(25) // без delay прокручивает раньше, не смотря на join
                }.join()
                binding.list.smoothScrollToPosition(0)
            }
        }

        viewModel.newerCount.observe(viewLifecycleOwner) { state ->
            binding.newerCount.isVisible = state > 0
        }

        return binding.root
    }

    override fun onDestroyView() {
        mediaPlayer.release()
        super.onDestroyView()
    }
}