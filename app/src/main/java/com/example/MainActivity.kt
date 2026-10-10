package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.LocaleHelper

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()

  override fun attachBaseContext(newBase: android.content.Context) {
    val currentLang = LocaleHelper.getAppLanguage(newBase)
    val context = LocaleHelper.setAppLanguage(newBase, currentLang)
    super.attachBaseContext(context)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
          viewModel.refreshServiceStatuses()
        }

        LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
              permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          }
          viewModel.refreshServiceStatuses()
        }

        Surface(modifier = Modifier.fillMaxSize()) {
          MainScreen(viewModel = viewModel)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshServiceStatuses()
  }
}
