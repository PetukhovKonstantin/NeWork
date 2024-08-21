package ru.netology.nework.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Resources
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.PopupMenu
import android.widget.VideoView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapters.OnInteractionListener
import ru.netology.nework.databinding.FragmentCardPostBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Post
import ru.netology.nework.utils.Companion.Companion.linkArg
import ru.netology.nework.utils.Companion.Companion.mentionsCountArg
import ru.netology.nework.utils.Companion.Companion.textArg
import ru.netology.nework.utils.Companion.Companion.userId
import ru.netology.nework.utils.FloatingValue.convertDatePublished
import ru.netology.nework.utils.NumberTranslator
import ru.netology.nework.viewmodels.AuthViewModel
import ru.netology.nework.viewmodels.PostViewModel
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class CardPostFragment : Fragment() {
    private val viewModel: PostViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val mediaPlayer = MediaPlayer()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCardPostBinding.inflate(layoutInflater)

        val postId = if (!arguments?.textArg.isNullOrEmpty()) {
            arguments?.textArg?.toLongOrNull() ?: 0L
        } else {
            0L
        }

        viewModel.data.observe(viewLifecycleOwner) { model ->
            val post = model.posts.find { it.id == postId } ?: return@observe
            with(binding) {
                title.text = post.author
                datePublished.text =
                    convertDatePublished(post.published)
                content.text = post.content
                like.text = NumberTranslator.translateNumber(
                    post.likeOwnerIds?.size ?: 0
                )
                like.isChecked = post.likedByMe
                share.isChecked = post.sharedByMe
                mentions.text = NumberTranslator.translateNumber(
                    post.mentionIds?.size ?: 0
                )
                mentions.isCheckable = true
                mentions.isClickable = false
                mentions.isChecked = post.mentionedMe
                links.isVisible = (post.link != null)
                if (post.link != null) {
                    links.text = post.link
                }
                Glide.with(avatar)
                    .load(post.authorAvatar)
                    .placeholder(R.drawable.ic_image_not_supported_24)
                    .error(R.drawable.ic_not_avatars_24)
                    .circleCrop()
                    .timeout(10_000)
                    .into(avatar)
                moreVert.visibility =
                    if (post.ownedByMe) View.VISIBLE else View.INVISIBLE
                if (post.attachment != null) {
                    attachmentContent.isVisible = true
                    if (post.attachment.type == AttachmentType.IMAGE) {
                        Glide.with(imageAttachment)
                            .load(post.attachment.url)
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
                        (post.attachment.type == AttachmentType.VIDEO)
                    playButtonPost.isVisible =
                        (post.attachment.type == AttachmentType.VIDEO)
                    playButtonPostAudio.isVisible =
                        (post.attachment.type == AttachmentType.AUDIO)
                    imageAttachment.isVisible =
                        (post.attachment.type == AttachmentType.IMAGE)
                    descriptionAttachment.isVisible =
                        (post.attachment.type == AttachmentType.AUDIO)
                } else {
                    attachmentContent.visibility = View.GONE
                }

                if (post.coords != null && post.coords.toString().isNotEmpty()) {
                    binding.mapContainer.visibility = View.VISIBLE
                    binding.selectedCoords.text = post.coords.toString()
                    val point = Point(post.coords.lat?.toDouble() ?: 0.0, post.coords.long?.toDouble() ?: 0.0)
                    binding.mapViewPost.mapView.map.move(CameraPosition(point, 14.0f, 0.0f, 0.0f))
                    binding.mapViewPost.mapView.map.mapObjects.addPlacemark(point, ImageProvider.fromResource(requireContext(), R.drawable.ic_placemark_16))
                }

                like.setOnClickListener {
                    like.isChecked = !like.isChecked
                    interactionListener.onLike(post)
                }
                share.setOnClickListener {
                    interactionListener.onShare(post)
                }
                playButtonPostAudio.setOnClickListener {
                    CoroutineScope(EmptyCoroutineContext).launch {
                        interactionListener.onPlayPost(post)
                    }
                }
                playButtonPost.setOnClickListener {
                    interactionListener.onPlayPost(post, binding.videoAttachment)
                }
                imageAttachment.setOnClickListener {
                    interactionListener.onPreviewAttachment(post)
                }
                links.setOnClickListener {
                    interactionListener.onLink(post)
                }
                avatar.setOnClickListener {
                    interactionListener.onTapAvatar(post)
                }
                moreVert.setOnClickListener {
                    val popupMenu = PopupMenu(it.context, it)
                    popupMenu.apply {
                        inflate(R.menu.options_post)
                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.remove -> {
                                    moreVert.isChecked = false
                                    interactionListener.onRemove(post)
                                    true
                                }

                                R.id.edit -> {
                                    moreVert.isChecked = false
                                    interactionListener.onEdit(post)
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
        return binding.root
    }

    private val interactionListener = object : OnInteractionListener {

        override fun onTapAvatar(post: Post) {
            findNavController().navigate(
                R.id.action_cardPostFragment_to_profileFragment,
                Bundle().apply {
                    userId = post.authorId
                }
            )
        }

        override fun onLike(post: Post) {
            if (authViewModel.authenticated) {
                viewModel.likeById(post)
            } else {
                AlertDialog.Builder(context)
                    .setMessage(R.string.action_not_allowed)
                    .setPositiveButton(R.string.sign_up) { _, _ ->
                        findNavController().navigate(
                            R.id.action_cardPostFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_up)
                            }
                        )
                    }
                    .setNeutralButton(R.string.sign_in) { _, _ ->
                        findNavController().navigate(
                            R.id.action_cardPostFragment_to_authFragment,
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
                R.id.action_cardPostFragment_to_newPostFragment,
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
                R.id.action_cardPostFragment_to_viewImageAttach,
                Bundle().apply {
                    textArg = post.attachment?.url
                })
        }

        override fun onOpenPost(post: Post) {

        }
    }
}