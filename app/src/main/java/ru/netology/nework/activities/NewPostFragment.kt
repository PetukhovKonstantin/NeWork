package ru.netology.nework.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
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
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapters.OnInteractionListenerUsers
import ru.netology.nework.adapters.UsersAdapter
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Coords
import ru.netology.nework.dto.User
import ru.netology.nework.utils.AndroidUtils.hideKeyboard
import ru.netology.nework.utils.Companion.Companion.linkArg
import ru.netology.nework.utils.Companion.Companion.mentionsCountArg
import ru.netology.nework.utils.Companion.Companion.textArg
import ru.netology.nework.utils.FloatingValue.currentFragment
import ru.netology.nework.utils.FloatingValue.getExtensionFromUri
import ru.netology.nework.utils.FloatingValue.textNewPost
import ru.netology.nework.viewmodels.PostViewModel
import ru.netology.nework.viewmodels.UsersViewModel
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class NewPostFragment : Fragment() {

    private val binding by lazy { FragmentNewPostBinding.inflate(layoutInflater) }
    private val viewModel: PostViewModel by activityViewModels()
    private val userViewModel: UsersViewModel by viewModels()

    private var type: AttachmentType? = null
    private var attachRes: Attachment? = null
    private var mentionsIds:MutableList<Long> = mutableListOf()
    private var adapter = UsersAdapter(object : OnInteractionListenerUsers {
        override fun onTap(user: User) {
            mentionsIds.add(user.id)
            binding.countMentions.text = mentionsIds.size.toString()
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        with(binding) {

            arguments?.textArg?.let {
                edit.setText(it)
            }

            arguments?.linkArg?.let {
                inputLink.setText(it)
            }

            arguments?.mentionsCountArg?.let {
                countMentions.text = it.toString()
            }

            if (edit.text.isNullOrBlank()) {
                edit.setText(textNewPost)
            }

            edit.requestFocus()
            attachRes = viewModel.getEditedPostAttachment()
            Glide.with(photo)
                .load(attachRes?.url)
                .placeholder(
                    when (attachRes?.type) {
                        AttachmentType.AUDIO -> {
                            R.drawable.ic_baseline_audio_file_500
                        }
                        AttachmentType.VIDEO -> {
                            R.drawable.ic_baseline_video_library_500
                        }
                        else -> {
                            R.drawable.ic_not_image_500
                        }
                    }
                )
                .timeout(10_000)
                .into(photo)

            if (!attachRes?.url.isNullOrBlank() && arguments?.textArg != null) {
                binding.photoContainer.visibility = View.VISIBLE
            }

            val coords = viewModel.getEditedPostCoords()
            if (coords != null && coords.toString().isNotEmpty()) {
                binding.selectedCoords.text = coords.toString()
                binding.removeCoords.visibility = View.VISIBLE
            }

            viewModel.media.observe(viewLifecycleOwner) {
                if (it.uri == null && attachRes?.url.isNullOrBlank()) {
                    binding.photoContainer.visibility = View.GONE
                    return@observe
                } else {
                    binding.photoContainer.visibility = View.VISIBLE
                    if (it.attachmentType == AttachmentType.IMAGE) {
                        binding.photo.setImageURI(it.uri)
                    }
                }
            }

            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_new_post, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.save -> {
                            binding.let {
                                viewModel.changeContent(
                                    it.edit.text.toString(),
                                    it.inputLink.text.toString().ifEmpty { null },
                                    mentionsIds
                                )
                                viewModel.save()
                                hideKeyboard(requireView())
                            }
                            true
                        }
                        else -> false
                    }

            }, viewLifecycleOwner)

            binding.listUsers.adapter = adapter
            clickListeners()

            return root
        }
    }

    override fun onStart() {
        currentFragment = javaClass.simpleName
        super.onStart()
    }

    @SuppressLint("IntentReset")
    private fun clickListeners() {

        binding.countMentions.setOnLongClickListener {
            mentionsIds = mutableListOf()
            binding.countMentions.text = mentionsIds.size.toString()
            true
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Coords>("selectedCoords")
            ?.observe(viewLifecycleOwner) { coords ->
                viewModel.changeCoords(coords)
                binding.selectedCoords.text = coords.toString()
                binding.removeCoords.visibility = View.VISIBLE
                val point = Point(coords.lat?.toDouble() ?: 0.0, coords.long?.toDouble() ?: 0.0)
                binding.mapViewPost.mapView.map.move(CameraPosition(point, 14.0f, 0.0f, 0.0f))
                binding.mapViewPost.mapView.map.mapObjects.addPlacemark(point, com.yandex.runtime.image.ImageProvider.fromResource(requireContext(), R.drawable.ic_placemark_16))
            }


        val pickPhotoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    ImagePicker.RESULT_ERROR -> {
                        Snackbar.make(
                            binding.root,
                            ImagePicker.getError(it.data),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    Activity.RESULT_OK -> {
                        val uri: Uri? = it.data?.data
                        viewModel.changeMedia(uri, uri?.toFile(), AttachmentType.IMAGE)
                    }
                }
            }

        val pickMediaLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    Activity.RESULT_OK -> {
                        val uri: Uri? = it.data?.data
                        val contentResolver = context?.contentResolver
                        val inputStream = uri?.let { it1 -> contentResolver?.openInputStream(it1) }
                        val audioBytes = inputStream?.readBytes()
                        if (uri != null && contentResolver != null) {
                            val extension = getExtensionFromUri(uri, contentResolver)
                            val file = File(context?.getExternalFilesDir(null), "input.$extension")
                            FileOutputStream(file).use { outputStream ->
                                outputStream.write(audioBytes)
                                outputStream.flush()
                            }
                            viewModel.changeMedia(uri, file, type)
                        }
                    }
                    else -> {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.error_upload),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }

        binding.bottomMenuAttachment.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.upload_photo -> {
                    ImagePicker.with(this)
                        .crop()
                        .compress(2048)
                        .provider(ImageProvider.CAMERA)
                        .createIntent(pickPhotoLauncher::launch)
                    true
                }
                R.id.select_attachment -> {
                    binding.bottomSubmenuAttachment.visibility = if (binding.bottomSubmenuAttachment.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    true
                }
                R.id.select_mentions-> {
                    binding.listUsers.isVisible = !binding.listUsers.isVisible
                    lifecycleScope.launch {
                        repeatOnLifecycle(Lifecycle.State.CREATED) {
                            userViewModel.dataUsersList.collectLatest {
                                adapter.submitList(it)
                            }
                        }
                    }
                    true
                }
                R.id.select_location -> {
                    findNavController().navigate(R.id.action_newPostFragment_to_mapFragment)
                    true
                }
                else -> false
            }
        }

        binding.bottomSubmenuAttachment.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.upload_image -> {
                    ImagePicker.with(this)
                        .crop()
                        .compress(2048)
                        .provider(ImageProvider.GALLERY)
                        .galleryMimeTypes(
                            arrayOf(
                                "image/png",
                                "image/jpeg",
                            )
                        )
                        .createIntent(pickPhotoLauncher::launch)
                    binding.bottomSubmenuAttachment.visibility = View.GONE
                    true
                }
                R.id.upload_audio -> {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                    intent.type = "audio/*"
                    type = AttachmentType.AUDIO
                    attachRes = attachRes?.copy(type = type!!)
                    pickMediaLauncher.launch(intent)
                    binding.bottomSubmenuAttachment.visibility = View.GONE
                    true
                }
                R.id.upload_video -> {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    intent.type = "video/*"
                    type = AttachmentType.VIDEO
                    attachRes = attachRes?.copy(type = type!!)
                    pickMediaLauncher.launch(intent)
                    binding.bottomSubmenuAttachment.visibility = View.GONE
                    true
                }
                else -> false
            }
        }

        binding.removeCoords.setOnClickListener {
            viewModel.changeCoords(null)
            binding.selectedCoords.text = getString(R.string.not_selected_coordinates)
            binding.removeCoords.visibility = View.INVISIBLE
        }

        binding.removePhotoPost.setOnClickListener {
            viewModel.deleteAttachment()
            attachRes = null
            viewModel.changeMedia(null, null, null)
        }


        with(binding) {

            viewModel.postCreated.observe(viewLifecycleOwner) {
                viewModel.loadPosts()
                findNavController().navigateUp()
            }

            fabCancel.setOnClickListener {
                if (viewModel.getEditedId() == 0L) {
                    textNewPost = edit.text.toString()
                } else {
                    edit.text?.clear()
                    viewModel.save()
                }
                hideKeyboard(root)
                findNavController().navigateUp()
            }

        }
    }
}