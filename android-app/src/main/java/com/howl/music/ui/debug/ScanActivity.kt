package com.howl.music.ui.debug

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.scan.LibraryScanRepository
import com.howl.music.data.scan.MediaScanner
import com.howl.music.data.scan.ScanProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanActivity : ComponentActivity() {
    private val message = mutableStateOf("Scan music on this device.")
    private val scanning = mutableStateOf(false)
    private val askingPermission = mutableStateOf(false)
    private var scanJob: Job? = null
    private val permission: String
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        askingPermission.value = false
        if (granted) startScan()
        else message.value = "Music access denied. Allow music access in Settings to scan."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Text("Music scan")
                Text(message.value)
                if (scanning.value) {
                    LinearProgressIndicator()
                    Button(onClick = { scanJob?.cancel() }) { Text("Cancel") }
                } else {
                    Button(onClick = ::requestScan, enabled = !askingPermission.value) {
                        Text("Scan")
                    }
                }
            }
        }
    }

    private fun requestScan() {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            startScan()
        } else {
            askingPermission.value = true
            permissionRequest.launch(permission)
        }
    }

    private fun startScan() {
        if (scanning.value) return
        scanning.value = true
        message.value = "Reading MediaStore…"
        scanJob = lifecycleScope.launch {
            val db = HowlDatabase.open(applicationContext)
            try {
                LibraryScanRepository(db).scan(MediaScanner(applicationContext).tracks()).collect { progress ->
                    message.value = when (progress) {
                        is ScanProgress.Indexing -> "Indexed ${progress.processed} tracks…"
                        is ScanProgress.Complete ->
                            "Scan complete. Found ${progress.processed} tracks. Library total ${progress.librarySize}."
                    }
                }
            } catch (cancelled: CancellationException) {
                message.value = "Scan cancelled. Completed batches were kept."
                throw cancelled
            } catch (_: SecurityException) {
                message.value = "Music access is required to scan."
            } catch (failure: Exception) {
                message.value = "Scan failed. ${failure.message.orEmpty()}"
            } finally {
                withContext(NonCancellable + Dispatchers.IO) { db.close() }
                scanning.value = false
            }
        }
    }
}
