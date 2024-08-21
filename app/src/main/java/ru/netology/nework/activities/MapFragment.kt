package ru.netology.nework.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentMapBinding
import ru.netology.nework.dto.Coords

class MapFragment : Fragment() {

    private val binding by lazy { FragmentMapBinding.inflate(layoutInflater) }
    private lateinit var nowTarget: Point

    private var locationPermission = false
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationPermission = true
                binding.mapView.mapWindow.map.move(POSITION)
            } else {
                showPermissionSnackbar()
            }
        }

    companion object {
        private val POINT = Point(55.751280, 37.629720)
        private val POSITION = CameraPosition(POINT, 17.0f, 150.0f, 30.0f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkMapPermission()
        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mapKit = MapKitFactory.getInstance()
        val userLocationLayer = mapKit.createUserLocationLayer(binding.mapView.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isHeadingEnabled = false

        binding.gettingPlaceContainer.visibility = View.VISIBLE
        binding.placemark.visibility = View.VISIBLE
        binding.apply {
            findMyLocationFab.setOnClickListener {
                if (locationPermission) {
                    if (userLocationLayer.cameraPosition() != null) {
                        binding.mapView.map.move(
                            CameraPosition(
                                userLocationLayer.cameraPosition()!!.target,
                                14.0f,
                                0.0f,
                                0.0f
                            )
                        )
                    } else {
                        Snackbar.make(
                            binding.mapView,
                            getString(R.string.no_user_location_error),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            mapView.map.apply {
                addCameraListener(cameraListener)
            }

            addPlaceFab.setOnClickListener {
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    "selectedCoords",
                    Coords(nowTarget.latitude.toString(), nowTarget.longitude.toString())
                )
                findNavController().navigateUp()
            }
        }

        return binding.root
    }

    private val cameraListener: CameraListener =
        CameraListener { _, cameraPosition, _, _ ->
            binding.coordinates.text = "${cameraPosition.target.latitude.toString().take(9)} | ${
                cameraPosition.target.longitude.toString().take(9)
            }"
            nowTarget = cameraPosition.target
        }

    private fun showPermissionSnackbar() {
        Snackbar.make(binding.mapView, getString(R.string.need_geolocation), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.permission)) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .show()
    }

    private fun checkMapPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                locationPermission = true
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showPermissionSnackbar()
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
    }
}