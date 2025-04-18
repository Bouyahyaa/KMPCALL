package com.bouyahya.kmpcall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.audioTracks
import com.shepeliev.webrtckmp.videoTracks
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CallScreen(
    onDisconnect: () -> Unit,
    vm: CallViewModel = koinViewModel()
) {

    val state by vm.state.collectAsState()
    val (localStream, setLocalStream) = remember { mutableStateOf<MediaStream?>(null) }
    var isMicMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val stream = MediaDevices.getUserMedia(audio = true, video = true)
        setLocalStream(stream)
        vm.onEvent(CallEvent.Connect(stream))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main grid of remote connections
        if (state.connectionList.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(state.connectionList) { connection ->
                    val remoteVideoTrack = connection.remoteVideoTrack
                    val remoteAudioTrack = connection.remoteAudioTrack

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                    ) {
                        if (remoteVideoTrack != null && connection.videoEnabled == true) {
                            Video(
                                videoTrack = remoteVideoTrack,
                                audioTrack = remoteAudioTrack,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No video", color = Color.White)
                            }
                        }

                        // Microphone status indicator at bottom right
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(
                                    color = if (connection.audioEnabled == true)
                                        Color.Green.copy(alpha = 0.7f) else
                                        Color.Red.copy(alpha = 0.7f),
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector =
                                    if (connection.audioEnabled == true)
                                        Icons.Default.Mic else
                                        Icons.Default.MicOff,
                                contentDescription = if (connection.audioEnabled == true)
                                    "Mic on" else
                                    "Mic off",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Waiting for connections...")
            }
        }

        // Local video as PIP in bottom right
        val localVideoTrack = localStream?.videoTracks?.firstOrNull()
        if (!isCameraOff)
            localVideoTrack?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(width = 120.dp, height = 160.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Video(
                        videoTrack = it,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(width = 120.dp, height = 160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text("No video", color = Color.White)
            }

        // Control buttons at the bottom center
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic toggle button
            Button(
                onClick = {
                    isMicMuted = !isMicMuted
                    localStream?.audioTracks?.forEach { it.enabled = !isMicMuted }
                    vm.onEvent(CallEvent.ToggleAudio(!isMicMuted))
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isMicMuted)
                        Color.Red else
                        Color(0xFF2196F3)
                )
            ) {
                Icon(
                    imageVector = if (isMicMuted)
                        Icons.Default.MicOff else
                        Icons.Default.Mic,
                    contentDescription = if (isMicMuted)
                        "Unmute" else
                        "Mute",
                    tint = Color.White
                )
            }

            // Hang up button
            Button(
                onClick = {
                    scope.launch {
                        localStream?.release()
                        setLocalStream(null)
                        vm.onEvent(CallEvent.LeaveCall)
                        onDisconnect()
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End call",
                    tint = Color.White
                )
            }

            // Camera toggle button
            Button(
                onClick = {
                    isCameraOff = !isCameraOff
                    localStream?.videoTracks?.forEach { it.enabled = !isCameraOff }
                    vm.onEvent(CallEvent.ToggleVideo(!isCameraOff))
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isCameraOff)
                        Color.Red else
                        Color(0xFF2196F3)
                )
            ) {
                Icon(
                    imageVector = if (isCameraOff)
                        Icons.Default.VideocamOff else
                        Icons.Default.Videocam,
                    contentDescription = if (isCameraOff)
                        "Turn on camera" else
                        "Turn off camera",
                    tint = Color.White
                )
            }
        }
    }
}