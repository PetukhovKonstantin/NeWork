package ru.netology.nework.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.VideoView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentCardPostBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Post
import ru.netology.nework.utils.FloatingValue.convertDatePublished
import ru.netology.nework.utils.NumberTranslator
import ru.netology.nework.utils.setAllOnClickListener
import kotlin.coroutines.EmptyCoroutineContext

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onShare(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onPlayPost(post: Post, videoView: VideoView? = null) {}
    fun onLink(post: Post) {}
    fun onPreviewAttachment(post: Post) {}
    fun onTapAvatar(post: Post) {}
    fun onOpenPost(post: Post) {}
}

class PostAdapter(
    private val onInteractionListener: OnInteractionListener
) : ListAdapter<Post,PostViewHolder>(PostDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding =
            FragmentCardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.renderingPostStructure(post)
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.videoAttachment.stopPlayback()
        holder.binding.videoAttachment.setVideoURI(null)
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }
}

class PostViewHolder(
    val binding: FragmentCardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun renderingPostStructure(post: Post) {
        with(binding) {
            title.text = post.author
            datePublished.text = convertDatePublished(post.published)
            content.text = post.content
            like.text = NumberTranslator.translateNumber(post.likeOwnerIds?.size ?: 0)
            like.isChecked = post.likedByMe
            share.isChecked = post.sharedByMe
            mentions.text = NumberTranslator.translateNumber(post.mentionIds?.size ?: 0)
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
            moreVert.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE
            if (post.attachment != null) {
                attachmentContent.isVisible = true
                if(post.attachment.type == AttachmentType.IMAGE) {
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
                videoAttachment.isVisible = (post.attachment.type == AttachmentType.VIDEO)
                playButtonPost.isVisible = (post.attachment.type == AttachmentType.VIDEO)
                playButtonPostAudio.isVisible = (post.attachment.type == AttachmentType.AUDIO)
                imageAttachment.isVisible = (post.attachment.type == AttachmentType.IMAGE)
                descriptionAttachment.isVisible = (post.attachment.type == AttachmentType.AUDIO)
            } else {
                attachmentContent.visibility = View.GONE
            }
            postListeners(post)
        }
    }

    private fun postListeners(post: Post) {
        with(binding) {
            like.setOnClickListener {
                like.isChecked = !like.isChecked
                onInteractionListener.onLike(post)
            }
            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }
            playButtonPostAudio.setOnClickListener {
                CoroutineScope(EmptyCoroutineContext).launch {
                    onInteractionListener.onPlayPost(post)
                }
            }
            playButtonPost.setOnClickListener {
                onInteractionListener.onPlayPost(post, binding.videoAttachment)
            }
            imageAttachment.setOnClickListener {
                onInteractionListener.onPreviewAttachment(post)
            }
            links.setOnClickListener {
                onInteractionListener.onLink(post)
            }
            avatar.setOnClickListener {
                onInteractionListener.onTapAvatar(post)
            }
            moreVert.setOnClickListener {
                val popupMenu = PopupMenu(it.context, it)
                popupMenu.apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                moreVert.isChecked = false
                                onInteractionListener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                moreVert.isChecked = false
                                onInteractionListener.onEdit(post)
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
            groupOpenPost.setAllOnClickListener {
                onInteractionListener.onOpenPost(post)
            }
        }
    }
}