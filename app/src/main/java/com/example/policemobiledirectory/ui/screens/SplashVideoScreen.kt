package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@Composable
fun SplashVideoScreen(
    navController: NavController,
    viewModel: EmployeeViewModel
) {
    val context = LocalContext.current
    val videoView = remember { VideoView(context) }

    fun navigateNext() {
        val target = if (viewModel.isLoggedIn.value) Routes.EMPLOYEE_LIST else Routes.LOGIN
        navController.navigate(target) {
            popUpTo(Routes.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoView.stopPlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A3C)) // very dark navy blue background
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                videoView.apply {
                    // Play locally bundled splash video for instant start
                    // Place the video file as res/raw/splash_intro.mp4 (download from the provided Drive link)
                    setVideoURI(
                        Uri.parse("android.resource://${context.packageName}/${R.raw.splash_intro}")
                    )
                    setOnCompletionListener { navigateNext() }
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = false
                        start()
                    }
                    // Allow tapping the video itself to skip
                    setOnTouchListener { _, _ ->
                        navigateNext()
                        true
                    }
                }
            }
        )

        // Skip overlay (tap anywhere on overlay to skip)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { navigateNext() }
                .padding(0.dp)
                .align(Alignment.Center)
                .background(Color.Transparent)
        )

        // Skip label (top-right)
        Surface(
            color = Color.Black.copy(alpha = 0.4f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clickable { navigateNext() }
        ) {
            Text(
                text = "Skip",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

