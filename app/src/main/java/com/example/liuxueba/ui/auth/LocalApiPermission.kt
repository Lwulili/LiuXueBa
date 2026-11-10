package com.example.liuxueba.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.liuxueba.BuildConfig

/** Android 17 protects the local API used by debug builds with a runtime permission. */
@Composable
internal fun rememberLocalApiSubmission(
    onGranted: (AuthPage) -> Unit,
    onDenied: () -> Unit
): (AuthPage) -> Unit {
    val context = LocalContext.current
    val currentOnGranted by rememberUpdatedState(onGranted)
    val currentOnDenied by rememberUpdatedState(onDenied)
    var pendingPage by rememberSaveable { mutableStateOf<AuthPage?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val page = pendingPage
        pendingPage = null
        if (page != null) {
            if (granted) currentOnGranted(page) else currentOnDenied()
        }
    }
    return { page ->
        if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_LOCAL_NETWORK
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted(page)
        } else if (pendingPage == null) {
            pendingPage = page
            launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }
}
