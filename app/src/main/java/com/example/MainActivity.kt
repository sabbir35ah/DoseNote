package com.example

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DoseNoteApp
import com.example.ui.MainViewModel
import com.example.ui.theme.DoseNoteTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val currentUser by mainViewModel.currentUser.collectAsState()

            // Handle language override dynamically
            val currentLanguage = currentUser?.language ?: Locale.getDefault().language
            val targetLocale = if (currentLanguage == "bn") Locale("bn", "BD") else Locale.ENGLISH

            val config = Configuration(LocalConfiguration.current).apply {
                setLocale(targetLocale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setLocales(LocaleList(targetLocale))
                }
            }

            CompositionLocalProvider(LocalConfiguration provides config) {
                DoseNoteTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        DoseNoteApp(viewModel = mainViewModel)
                    }
                }
            }
        }
    }
}
