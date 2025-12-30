package com.example.moodtune

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            DarkMoodTuneTheme {
                MoodTuneApp()
            }
        }
    }
}

/* ---------------- ROOT ---------------- */

@Composable
fun MoodTuneApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var nowPlaying by remember { mutableStateOf<Song?>(null) }
    var showPlayer by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    // ⬅ Intercept BACK when player is open
    BackHandler(enabled = showPlayer) {
        showPlayer = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // 👈 FIX TOP SPACING
    ) {

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Black
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Songs", color = Color.White) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Mood Test", color = Color.White) }
            )
        }

        when (selectedTab) {
            0 -> SongList { song ->
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(context, song.resId)
                mediaPlayer?.start()
                isPlaying = true
                nowPlaying = song
                showPlayer = true
            }

            1 -> MoodDetection { song ->
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(context, song.resId)
                mediaPlayer?.start()
                isPlaying = true
                nowPlaying = song
                showPlayer = true
            }
        }
    }

    // 🎧 FULL PLAYER OVERLAY
    if (showPlayer && nowPlaying != null) {
        FullPlayer(
            song = nowPlaying!!,
            isPlaying = isPlaying,
            onBack = { showPlayer = false },
            onPlayPause = {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                        isPlaying = false
                    } else {
                        it.start()
                        isPlaying = true
                    }
                }
            }
        )
    }
}

/* ---------------- SONG LIST ---------------- */

@Composable
fun SongList(onSongClick: (Song) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        items(allSongs) { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(song) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Green)
                Spacer(Modifier.width(16.dp))
                Text(song.name, color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

/* ---------------- FULL PLAYER ---------------- */

@Composable
fun FullPlayer(
    song: Song,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {

            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }

            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(song.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(32.dp))

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Green)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

/* ---------------- MOOD DETECTION ---------------- */

@OptIn(ExperimentalGetImage::class)
@Composable
fun MoodDetection(onSongSelected: (Song) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var detecting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val smileValues = remember { mutableStateListOf<Float>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (detecting) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)

                    providerFuture.addListener({
                        val provider = providerFuture.get()

                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val analyzer = ImageAnalysis.Builder().build()
                        val detector = FaceDetection.getClient(
                            FaceDetectorOptions.Builder()
                                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                                .build()
                        )

                        analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            val img = imageProxy.image
                            if (img != null) {
                                val image = InputImage.fromMediaImage(
                                    img, imageProxy.imageInfo.rotationDegrees
                                )
                                detector.process(image)
                                    .addOnSuccessListener { faces ->
                                        if (faces.isNotEmpty()) {
                                            smileValues.add(
                                                faces[0].smilingProbability ?: 0f
                                            )
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else imageProxy.close()
                        }

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            context as ComponentActivity,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            analyzer
                        )
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
        }

        if (status.isNotEmpty()) {
            Text(status, color = Color.White, modifier = Modifier.padding(12.dp))
        }

        Button(
            onClick = {
                detecting = true
                status = "Detecting mood..."
                smileValues.clear()

                scope.launch {
                    delay(3000)
                    detecting = false

                    val avg = if (smileValues.isNotEmpty()) smileValues.average().toFloat() else 0f
                    val mood = inferMood(avg)

                    status = "Detected: $mood"
                    delay(2000) // 👈 SHOW MOOD FIRST

                    onSongSelected(songsByMood(mood).random())
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
        ) {
            Text("Start Mood Detection", color = Color.Black)
        }
    }
}

/* ---------------- DATA ---------------- */

data class Song(val name: String, val resId: Int)

val allSongs = listOf(
    Song("Chaleya", R.raw.happy1),
    Song("Tere Vaaste", R.raw.happy2),
    Song("What Jhumka", R.raw.happy3),
    Song("Heeriye", R.raw.happy4),
    Song("Akhiyan Gulaab", R.raw.neutral1),
    Song("Husn", R.raw.neutral2),
    Song("Phir Aur Kya Chahiye", R.raw.neutral3),
    Song("Neutral Track", R.raw.neutral4),
    Song("Satranga", R.raw.sad1),
    Song("Aararaari Raaro", R.raw.sad2),
    Song("O Maahi", R.raw.sad3),
    Song("Tum Se", R.raw.sad4)
)

fun songsByMood(mood: String): List<Song> =
    when (mood) {
        "Happy" -> allSongs.subList(0, 4)
        "Sad" -> allSongs.subList(8, 12)
        else -> allSongs.subList(4, 8)
    }

fun inferMood(smile: Float): String =
    when {
        smile >= 0.6f -> "Happy"
        smile <= 0.25f -> "Sad"
        else -> "Neutral"
    }

/* ---------------- DARK THEME ---------------- */

@Composable
fun DarkMoodTuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF1DB954),
            background = Color.Black,
            surface = Color.Black
        ),
        content = content
    )
}
