package ru.netology.nework.activities

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.utils.AndroidUtils.hideKeyboard
import ru.netology.nework.utils.ConstantValues.emptyJob
import ru.netology.nework.utils.FloatingValue.currentFragment
import ru.netology.nework.databinding.FragmentNewJobBinding
import ru.netology.nework.viewmodels.JobViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class NewJobFragment : Fragment() {

    private val binding by lazy { FragmentNewJobBinding.inflate(layoutInflater) }
    private val viewModel: JobViewModel by activityViewModels()
    private var job = emptyJob
    private var fragmentBinding: FragmentNewJobBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        fragmentBinding = binding

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.data.collectLatest { list ->
                    job = list.find { job ->
                        job.id == viewModel.getEditedId()
                    } ?: emptyJob
                }
            }
        }

        with(binding) {
            if (job != emptyJob) {
                jobOrganizationInput.setText(job.name)
                jobPositionInput.setText(job.position)
                dateStartWorkingInput.setText(job.start)
                dateEndWorkingInput.setText(job.finish)
                inputLink.setText(job.link)
            }

            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_new_post, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.save -> {
                            fragmentBinding?.let {
                                viewModel.changeContent(
                                    it.jobOrganizationInput.text.toString(),
                                    it.jobPositionInput.text.toString(),
                                    it.dateStartWorkingInput.text.toString(),
                                    it.dateEndWorkingInput.text.toString().ifEmpty { null },
                                    it.inputLink.text.toString().ifEmpty { null },
                                )
                                viewModel.save()
                                hideKeyboard(requireView())
                                findNavController().navigate(R.id.action_newJobFragment_to_profileFragment)
                            }
                            true
                        }
                        else -> false
                    }

            }, viewLifecycleOwner)

            binding.dateStartWorkingInput.setOnClickListener {
                showDatePickerDialog(binding.dateStartWorkingInput)
            }

            binding.dateEndWorkingInput.setOnClickListener {
                showDatePickerDialog(binding.dateEndWorkingInput)
            }
            return root
        }
    }

    private fun showDatePickerDialog(textInputEditText: TextInputEditText) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.choose_date))
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormatter.format(Date(selection))
            textInputEditText.setText(date)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    override fun onStart() {
        currentFragment = javaClass.simpleName
        super.onStart()
    }

    override fun onDestroyView() {
        fragmentBinding = null
        super.onDestroyView()
    }
}