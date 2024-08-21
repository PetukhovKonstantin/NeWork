package ru.netology.nework.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import android.view.View
import android.widget.PopupMenu
import android.widget.VideoView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.utils.FloatingValue.convertDatePublished
import ru.netology.nework.utils.NumberTranslator
import ru.netology.nework.databinding.FragmentCardEventBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.EventResponse
import ru.netology.nework.utils.setAllOnClickListener
import kotlin.coroutines.EmptyCoroutineContext

interface OnInteractionListenerEvent {
    fun onLike(event: EventResponse) {}
    fun onShare(event: EventResponse) {}
    fun onEdit(event: EventResponse) {}
    fun onRemove(event: EventResponse) {}
    fun onPlayPost(event: EventResponse, videoView: VideoView? = null) {}
    fun onLink(event: EventResponse) {}
    fun onPreviewAttachment(event: EventResponse) {}
    fun onSpeakersAction(event: EventResponse) {}
    fun onPartyAction(event: EventResponse) {}
    fun onJoinAction(event: EventResponse) {}
    fun onTapAvatar(event: EventResponse) {}
    fun onEventOpen(event: EventResponse) {}
}

class EventAdapter(
    private val onInteractionListener: OnInteractionListenerEvent
) : ListAdapter<EventResponse, EventViewHolder>(EventDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding =
            FragmentCardEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val post = getItem(position)
        holder.renderingPostStructure(post)
    }

    override fun onViewRecycled(holder: EventViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.videoAttachment.stopPlayback()
        holder.binding.videoAttachment.setVideoURI(null)
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<EventResponse>() {
    override fun areItemsTheSame(oldItem: EventResponse, newItem: EventResponse): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EventResponse, newItem: EventResponse): Boolean {
        return oldItem == newItem
    }
}

class EventViewHolder(
    val binding: FragmentCardEventBinding,
    private val onInteractionListener: OnInteractionListenerEvent,
) : RecyclerView.ViewHolder(binding.root) {

    fun renderingPostStructure(event: EventResponse) {
        with(binding) {
            title.text = event.author
            datePublished.text = convertDatePublished(event.published)
            content.text = event.content
            like.text = NumberTranslator.translateNumber(event.likeOwnerIds.size)
            like.isChecked = event.likedByMe
            eventDateValue.text = convertDatePublished(event.datetime).dropLast(3)
            eventFormatValue.text = event.type.getDisplayName(binding.root.context)
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
                videoAttachment.isVisible = (event.attachment.type == AttachmentType.VIDEO)
                playButtonPost.isVisible = (event.attachment.type == AttachmentType.VIDEO)
                playButtonPostAudio.isVisible = (event.attachment.type == AttachmentType.AUDIO)
                imageAttachment.isVisible = (event.attachment.type == AttachmentType.IMAGE)
                descriptionAttachment.isVisible = (event.attachment.type == AttachmentType.AUDIO)
            } else {
                attachmentContent.visibility = View.GONE
            }
            postListeners(event)
        }
    }

    private fun postListeners(event: EventResponse) {
        with(binding) {
            like.setOnClickListener {
                like.isChecked = !like.isChecked
                onInteractionListener.onLike(event)
            }
            share.setOnClickListener {
                onInteractionListener.onShare(event)
            }
            playButtonPostAudio.setOnClickListener {
                CoroutineScope(EmptyCoroutineContext).launch {
                    onInteractionListener.onPlayPost(event)
                }
            }
            playButtonPost.setOnClickListener {
                onInteractionListener.onPlayPost(event, binding.videoAttachment)
            }
            imageAttachment.setOnClickListener {
                onInteractionListener.onPreviewAttachment(event)
            }
            links.setOnClickListener {
                onInteractionListener.onLink(event)
            }
            partyButton.setOnClickListener {
                onInteractionListener.onPartyAction(event)
            }
            joinButton.setOnClickListener {
                onInteractionListener.onJoinAction(event)
            }
            speakersButton.setOnClickListener {
                onInteractionListener.onSpeakersAction(event)
            }
            avatar.setOnClickListener {
                onInteractionListener.onTapAvatar(event)
            }
            moreVert.setOnClickListener {
                val popupMenu = PopupMenu(it.context, it)
                popupMenu.apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                moreVert.isChecked = false
                                onInteractionListener.onRemove(event)
                                true
                            }

                            R.id.edit -> {
                                moreVert.isChecked = false
                                onInteractionListener.onEdit(event)
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

            groupOpenEvent.setAllOnClickListener {
                onInteractionListener.onEventOpen(event)
            }
        }
    }
}