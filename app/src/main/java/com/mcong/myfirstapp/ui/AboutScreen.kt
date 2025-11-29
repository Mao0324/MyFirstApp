package com.mcong.myfirstapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mcong.myfirstapp.BuildConfig
import com.mcong.myfirstapp.data.UpdateManager
import com.mcong.myfirstapp.data.VersionInfo
import kotlinx.coroutines.launch

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }
    
    var showDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<VersionInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "About App",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Open Source Licenses",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "This app uses the following open source libraries:\n" +
                    "- Jetpack Compose\n" +
                    "- Retrofit\n" +
                    "- Material Components",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
            scope.launch {
                val info = updateManager.getLatestVersion()
                if (info != null && info.versionCode > BuildConfig.VERSION_CODE) {
                    updateInfo = info
                    showDialog = true
                } else {
                    Toast.makeText(context, "App is up to date", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Check for Updates")
        }
    }

    if (showDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { if (!isDownloading) showDialog = false },
            title = { Text("Update Available") },
            text = {
                Column {
                    Text("New version: ${updateInfo!!.versionName}")
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("${(downloadProgress * 100).toInt()}%")
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    TextButton(onClick = {
                        isDownloading = true
                        scope.launch {
                            updateManager.downloadAndInstall(
                                versionInfo = updateInfo!!,
                                onProgress = { progress ->
                                    downloadProgress = progress
                                },
                                onComplete = {
                                    isDownloading = false
                                    showDialog = false
                                    downloadProgress = 0f
                                },
                                onError = { error ->
                                    isDownloading = false
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }) {
                        Text("Update")
                    }
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
