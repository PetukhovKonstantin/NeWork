package ru.netology.nework.activities


import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.utils.FloatingValue.currentFragment
import ru.netology.nework.utils.FloatingValue.textNewPost
import ru.netology.nework.viewmodels.AuthViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_app) {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)

        viewModel.data.observe(this) {
            invalidateOptionsMenu()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentFragment == "NewPostFragment") {
                    val fabCancel = findViewById<FloatingActionButton>(R.id.fab_cancel)
                    if (fabCancel != null) {
                        fabCancel.callOnClick()
                        return
                    }
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

        checkGoogleApiAvailability()
    }

    private fun checkGoogleApiAvailability() {
        with(GoogleApiAvailability.getInstance()) {
            val code = isGooglePlayServicesAvailable(this@MainActivity)
            if (code == ConnectionResult.SUCCESS) {
                return@with
            }
            if (isUserResolvableError(code)) {
                getErrorDialog(this@MainActivity, code, 9000)?.show()
                return
            }
            Toast.makeText(this@MainActivity, "Google Api Unavailable", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() {
        currentFragment = ""
        textNewPost = ""
        super.onStop()
    }
}