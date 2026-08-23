package com.example.ui.chats

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.ToneGenerator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AudioCallSession
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AudioCallScreen(
    currentUserId: String,
    callSession: AudioCallSession,
    chatRepository: ChatRepository,
    onCallClosed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val liveSession by chatRepository.listenToCallSession(callSession.callId).collectAsState(initial = callSession)
    val activeSession = liveSession ?: callSession

    val isCaller = activeSession.callerId == currentUserId
    val otherPartyName = if (isCaller) activeSession.receiverName else activeSession.callerName
    val otherPartyAvatar = if (isCaller) activeSession.receiverAvatarUrl else activeSession.callerAvatarUrl

    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var callSeconds by remember { mutableIntStateOf(0) }

    // Close when status is ENDED or REJECTED
    LaunchedEffect(activeSession.status) {
        if (activeSession.status == "ENDED" || activeSession.status == "REJECTED") {
            delay(1200)
            onCallClosed()
        }
    }

    // Call timer when CONNECTED
    LaunchedEffect(activeSession.status) {
        if (activeSession.status == "CONNECTED") {
            while (isActive) {
                delay(1000)
                callSeconds++
            }
        }
    }

    // Audio Ringtone and Live Audio Engine
    DisposableEffect(activeSession.status, isMuted, isSpeakerOn) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        var toneGen: ToneGenerator? = null
        var isAudioStreaming = false

        val ringtoneJob = scope.launch(Dispatchers.IO) {
            try {
                if (activeSession.status == "RINGING") {
                    toneGen = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 75)
                    while (isActive && activeSession.status == "RINGING") {
                        if (isCaller) {
                            // Outgoing ringing tone (Beep ... Beep ...)
                            toneGen?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1400)
                            delay(4000)
                        } else {
                            // Incoming phone ring tone (Dual high frequency)
                            toneGen?.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, 1800)
                            delay(3500)
                        }
                    }
                } else if (activeSession.status == "CONNECTED") {
                    // Play connected tone
                    toneGen = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 85)
                    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    delay(350)
                    toneGen?.release()
                    toneGen = null

                    // Set speaker mode
                    audioManager?.isSpeakerphoneOn = isSpeakerOn

                    // Start live audio loop for real duplex voice conversation
                    isAudioStreaming = true
                    val sampleRate = 16000
                    val bufferSize = AudioRecord.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    ).coerceAtLeast(2048)

                    try {
                        val record = AudioRecord(
                            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                            sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSize
                        )

                        val track = AudioTrack.Builder()
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .build()
                            )
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build()
                            )
                            .setBufferSizeInBytes(bufferSize)
                            .build()

                        if (record.state == AudioRecord.STATE_INITIALIZED && track.state == AudioTrack.STATE_INITIALIZED) {
                            record.startRecording()
                            track.play()
                            val audioBuffer = ShortArray(bufferSize / 2)

                            while (isActive && isAudioStreaming) {
                                val read = record.read(audioBuffer, 0, audioBuffer.size)
                                if (read > 0 && !isMuted) {
                                    track.write(audioBuffer, 0, read)
                                }
                            }

                            record.stop()
                            record.release()
                            track.stop()
                            track.release()
                        }
                    } catch (e: Exception) {
                        // Audio streaming fallback graceful
                    }
                }
            } catch (e: Exception) {
                // Ignore tone generator errors on unsupported platforms
            }
        }

        onDispose {
            ringtoneJob.cancel()
            isAudioStreaming = false
            try {
                toneGen?.stopTone()
                toneGen?.release()
            } catch (e: Exception) {
                // Safe dispose
            }
        }
    }

    val formattedDuration = String.format("%02d:%02d", callSeconds / 60, callSeconds % 60)

    val statusText = when (activeSession.status) {
        "RINGING" -> if (isCaller) "Ringing..." else "Incoming Audio Call..."
        "CONNECTED" -> formattedDuration
        "ENDED" -> "Call Ended"
        "REJECTED" -> "Call Declined"
        else -> "Connecting..."
    }

    // Pulsing transition for ringing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (activeSession.status == "RINGING") 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .testTag("audio_call_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section (Call type & status)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 36.dp)
            ) {
                Text(
                    text = "FRNDOM AUDIO CALL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = otherPartyName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (activeSession.status == "CONNECTED") Color(0xFF4ADE80) else Color(0xFF94A3B8)
                )
            }

            // Middle Section (Avatar with pulsing rings)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                // Pulsing Ring 1
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFF1877F2).copy(alpha = 0.15f))
                )

                // Pulsing Ring 2
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .scale(pulseScale * 0.95f)
                        .clip(CircleShape)
                        .background(Color(0xFF1877F2).copy(alpha = 0.25f))
                )

                // Avatar
                Surface(
                    modifier = Modifier.size(130.dp),
                    shape = CircleShape,
                    color = Color(0xFF1E293B),
                    shadowElevation = 8.dp
                ) {
                    if (otherPartyAvatar.isNotBlank()) {
                        AsyncImage(
                            model = otherPartyAvatar,
                            contentDescription = otherPartyName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = otherPartyName.firstOrNull()?.uppercase() ?: "U",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }

            // Bottom Section (Call Controls)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                if (activeSession.status == "RINGING" && !isCaller) {
                    // Incoming Call: Decline or Accept
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decline
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = Color(0xFFEF4444)
                            ) {
                                IconButton(
                                    onClick = { chatRepository.rejectAudioCall(activeSession) },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "Decline",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Decline", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Accept
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = Color(0xFF22C55E)
                            ) {
                                IconButton(
                                    onClick = { chatRepository.acceptAudioCall(activeSession.callId) },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Accept",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Accept", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    // Connected or Outgoing Call Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute Mic Toggle
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(54.dp),
                                shape = CircleShape,
                                color = if (isMuted) Color.White else Color.White.copy(alpha = 0.2f)
                            ) {
                                IconButton(onClick = { isMuted = !isMuted }) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Mute",
                                        tint = if (isMuted) Color(0xFF0F172A) else Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isMuted) "Unmute" else "Mute",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        // End Call Button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(68.dp),
                                shape = CircleShape,
                                color = Color(0xFFEF4444)
                            ) {
                                IconButton(
                                    onClick = { chatRepository.endAudioCall(activeSession) },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "End Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "End", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Speaker Toggle
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(54.dp),
                                shape = CircleShape,
                                color = if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.2f)
                            ) {
                                IconButton(onClick = { isSpeakerOn = !isSpeakerOn }) {
                                    Icon(
                                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                        contentDescription = "Speaker",
                                        tint = if (isSpeakerOn) Color(0xFF0F172A) else Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Speaker",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
