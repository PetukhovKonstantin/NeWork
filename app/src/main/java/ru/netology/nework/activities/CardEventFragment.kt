package ru.netology.nework.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Resources
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
import android.widget.PopupMenu
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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapters.OnInteractionListenerEvent
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentCardEventBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.EventResponse
import ru.netology.nework.utils.Companion.Companion.eventId
import ru.netology.nework.utils.Companion.Companion.eventRequestType
import ru.netology.nework.utils.Companion.Companion.textArg
import ru.netology.nework.utils.Companion.Companion.userId
import ru.netology.nework.utils.FloatingValue.convertDatePublished
import ru.netology.nework.utils.NumberTranslator
import ru.netology.nework.viewmodels.AuthViewModel
import ru.netology.nework.viewmodels.EventViewModel
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class CardEventFragment : Fragment() {
    private val viewModel: EventViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var appAuth: AppAuth

    val mediaPlayer = MediaPlayer()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCardEventBinding.inflate(layoutInflater)

        val eventId = if (!arguments?.textArg.isNullOrEmpty()) {
            arguments?.textArg?.toLongOrNull() ?: 0L
        } else {
            0L
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.data.collectLatest { events ->
                    val event = events.find { it.id == eventId } ?: return@collectLatest
                    with(binding) {
                        title.text = event.author
                        datePublished.text = convertDatePublished(event.published)
                        content.text = event.content
                        like.text = NumberTranslator.translateNumber(event.likeOwnerIds.size)
                        like.isChecked = event.likedByMe
                        eventDateValue.text = convertDatePublished(event.datetime).dropLast(3)
                        eventFormatValue.text = event.type.getDisplayName(requireContext())
                        joinButton.isChecked = event.participatedByMe
                        joinButton.text = if (joinButton.isChecked) {
                            binding.root.context.getString(R.string.un_join)
                        } else {
                            binding.root.context.getString(R.string.join)
                        }

                        links.isVisible = (event.link != null)
                        if (event.link != null) {
                            links.text = event.link
                        }

                        if (event.coords != null && event.coords.toString().isNotEmpty()) {
                            binding.mapContainer.visibility = View.VISIBLE
                            binding.selectedCoords.text = event.coords.toString()
                            val point = Point(event.coords.lat?.toDouble() ?: 0.0, event.coords.long?.toDouble() ?: 0.0)
                            binding.mapViewPost.mapView.map.move(CameraPosition(point, 14.0f, 0.0f, 0.0f))
                            binding.mapViewPost.mapView.map.mapObjects.addPlacemark(point, ImageProvider.fromResource(requireContext(), R.drawable.ic_placemark_16))
                        }

                        Glide.with(avatar)
                            .load(event.authorAvatar)
                            .placeholder(R.drawable.ic_image_not_supported_24)
                            .error(R.drawable.ic_not_avatars_24)
                            .circleCrop()
                            .timeout(10_000)
                            .into(avatar)
                        moreVert.visibility = if (event.ownedByMe) View.VISIBLE else View.INVISIBLE
                        if (event.attachment != null) {
                            attachmentContent.isVisible = true
                            if (event.attachment.type == AttachmentType.IMAGE) {
                                Glide.with(imageAttachment)
                                    .load(event.attachment.url)
                                    .placeholder(R.drawable.ic_image_not_supported_24)
                                    .apply(
                                        RequestOptions.overrideOf(
                                            Resources.getSystem().displayMetrics.widthPixels
                                        )
                                    )
                                    .timeout(10_000)
                                    .into(imageAttachment)
                            }
                            videoAttachment.isVisible =
                                (event.attachment.type == AttachmentType.VIDEO)
                            playButtonPost.isVisible =
                                (event.attachment.type == AttachmentType.VIDEO)
                            playButtonPostAudio.isVisible =
                                (event.attachment.type == AttachmentType.AUDIO)
                            imageAttachment.isVisible =
                                (event.attachment.type == AttachmentType.IMAGE)
                            descriptionAttachment.isVisible =
                                (event.attachment.type == AttachmentType.AUDIO)
                        } else {
                            attachmentContent.visibility = View.GONE
                        }
                        like.setOnClickListener {
                            like.isChecked = !like.isChecked
                            interactionListener.onLike(event)
                        }
                        share.setOnClickListener {
                            interactionListener.onShare(event)
                        }
                        playButtonPostAudio.setOnClickListener {
                            CoroutineScope(EmptyCoroutineContext).launch {
                                interactionListener.onPlayPost(event)
                            }
                        }
                        playButtonPost.setOnClickListener {
                            interactionListener.onPlayPost(event, binding.videoAttachment)
                        }
                        imageAttachment.setOnClickListener {
                            interactionListener.onPreviewAttachment(event)
                        }
                        links.setOnClickListener {
                            interactionListener.onLink(event)
                        }
                        partyButton.setOnClickListener {
                            interactionListener.onPartyAction(event)
                        }
                        joinButton.setOnClickListener {
                            interactionListener.onJoinAction(event)
                        }
                        speakersButton.setOnClickListener {
                            interactionListener.onSpeakersAction(event)
                        }
                        avatar.setOnClickListener {
                            interactionListener.onTapAvatar(event)
                        }
                        moreVert.setOnClickListener {
                            val popupMenu = PopupMenu(it.context, it)
                            popupMenu.apply {
                                inflate(R.menu.options_post)
                                setOnMenuItemClickListener { item ->
                                    when (item.itemId) {
                                        R.id.remove -> {
                                            moreVert.isChecked = false
                                            interactionListener.onRemove(event)
                                            true
                                        }

                                        R.id.edit -> {
                                            moreVert.isChecked = false
                                            interactionListener.onEdit(event)
                                            true
                                        }

                                        else -> false
                                    }
                                }
                            }.show()
                            popupMenu.setOnDismissListener {
                                moreVert.isChecked = false
                            }
                        }
                    }
                }
            }
        }

        var menuProvider: MenuProvider? = null

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
                                R.id.action_cardEventFragment_to_authFragment,
                                Bundle().apply {
                                    textArg = getString(R.string.sign_in)
                                }
                            )
                            true
                        }

                        R.id.signup -> {
                            findNavController().navigate(
                                R.id.action_cardEventFragment_to_authFragment,
                                Bundle().apply {
                                    textArg = getString(R.string.sign_up)
                                }
                            )
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

        return binding.root
    }

    private val interactionListener = object : OnInteractionListenerEvent {

        override fun onTapAvatar(event: EventResponse) {
            findNavController().navigate(
                R.id.action_cardEventFragment_to_profileFragment,
                Bundle().apply {
                    userId = event.authorId
                }
            )
        }

        override fun onLike(event: EventResponse) {
            if (authViewModel.authenticated) {
                viewModel.likeById(event)
            } else {
                AlertDialog.Builder(context)
                    .setMessage(R.string.action_not_allowed)
                    .setPositiveButton(R.string.sign_up) { _, _ ->
                        findNavController().navigate(
                            R.id.action_cardEventFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_up)
                            }
                        )
                    }
                    .setNeutralButton(R.string.sign_in) { _, _ ->
                        findNavController().navigate(
                            R.id.action_cardEventFragment_to_authFragment,
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
            findNavController().navigate(R.id.action_eventsFragment_to_cardEventFragment)
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
                R.id.action_cardEventFragment_to_viewImageAttach,
                Bundle().apply {
                    textArg = event.attachment?.url
                })
        }

        override fun onSpeakersAction(event: EventResponse) {
            if (event.speakerIds.isNotEmpty()) {
                findNavController().navigate(
                    R.id.action_cardEventFragment_to_bottomSheetFragment,
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
                    R.id.action_cardEventFragment_to_bottomSheetFragment,
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

        }
    }
}