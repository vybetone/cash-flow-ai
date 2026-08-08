package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CashFlowApp
import com.example.ui.CashFlowViewModel
import com.example.ui.theme.CashFlowTheme

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>

    companion object {
        // Keys used to pass projection data to the service
        const val EXTRA_RESULT_CODE = "com.example.extra.RESULT_CODE"
        const val EXTRA_RESULT_INTENT = "com.example.extra.RESULT_INTENT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register the ActivityResultLauncher here
        mediaProjectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            try {
                val resultCode = result.resultCode
                val data = result.data
                Log.d(TAG, "MediaProjection result: resultCode=$resultCode, data=${'$'}{data != null}")
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // Start the service now that we have a valid projection Intent
                    Log.i(TAG, "Permission granted. Starting ScreenCaptureService with projection data.")
                    val svcIntent = Intent(this, com.example.data.ScreenCaptureService::class.java).apply {
                        putExtra(EXTRA_RESULT_CODE, resultCode)
                        putExtra(EXTRA_RESULT_INTENT, data)
                    }
                    try {
                        ContextCompat.startForegroundService(this, svcIntent)
                        Log.d(TAG, "Requested startForegroundService for ScreenCaptureService")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start ScreenCaptureService", e)
                        Toast.makeText(this, "Failed to start screen capture service: ${'$'}{e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "MediaProjection permission denied or no data")
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Exception processing MediaProjection result", t)
                Toast.makeText(this, "Error while handling screen permission: ${'$'}{t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }

        setContent {
            CashFlowTheme {
                val viewModel: CashFlowViewModel = viewModel()
                CashFlowApp(viewModel = viewModel)
            }
        }
    }

    /**
     * Call this to start the MediaProjection permission flow.
     * This method will prompt the user; if granted, it starts the ScreenCaptureService carrying
     * the projection result extras.
     */
    fun requestScreenCapturePermission() {
        try {
            val pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (pm == null) {
                Log.e(TAG, "MediaProjectionManager unavailable")
                Toast.makeText(this, "Screen capture not supported on this device", Toast.LENGTH_SHORT).show()
                return
            }
            val captureIntent = pm.createScreenCaptureIntent()
            Log.d(TAG, "Launching MediaProjection permission intent")
            mediaProjectionLauncher.launch(captureIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch MediaProjection permission intent", e)
            Toast.makeText(this, "Failed to request screen capture permission: ${'$'}{e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
