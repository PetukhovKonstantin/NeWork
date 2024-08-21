package ru.netology.nework.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.utils.Companion.Companion.textArg
import ru.netology.nework.databinding.FragmentAttachmentImageViewBinding

@AndroidEntryPoint
class ViewImageAttach : Fragment() {

    private lateinit var binding: FragmentAttachmentImageViewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAttachmentImageViewBinding.inflate(layoutInflater)

        val url = arguments?.textArg ?: ""

        Glide.with(binding.image)
            .load(url)
            .placeholder(R.drawable.ic_not_image_500)
            .timeout(10_000)
            .into(binding.image)



        binding.fabCancel.setOnClickListener {
            findNavController().navigateUp()
        }


        return binding.root
    }
}