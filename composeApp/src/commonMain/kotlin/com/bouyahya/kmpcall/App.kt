package com.bouyahya.kmpcall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bouyahya.kmpcall.ui.CallViewModel
import com.bouyahya.kmpcall.ui.Video
import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.videoTracks
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.bouyahya.kmpcall.ui.CallEvent

@Composable
@Preview
fun App(vm: CallViewModel = koinViewModel()) {
    MaterialTheme {
        val state by vm.state.collectAsState()
        val (localStream, setLocalStream) = remember { mutableStateOf<MediaStream?>(null) }
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
                            if (remoteVideoTrack != null) {
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
            }
        }
    }
}

@Composable
private fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text)
    }
}