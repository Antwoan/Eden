package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack as AndroidAudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sin

data class AudioTrackItem(
    val id: String,
    val title: String,
    val uriString: String,
    val durationText: String = "0:00"
)

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentPlaylistName: String? = null,
    val currentPlaylistId: Long? = null,
    val currentTrackTitle: String? = null,
    val currentAmbientType: String? = null,
    val isSynthActive: Boolean = false,
    val volume: Float = 0.8f
)

class AmbientAudioManager(private val context: Context) {
    private val TAG = "AmbientAudioManager"
    private var mediaPlayer: MediaPlayer? = null
    private var synthAudioTrack: AndroidAudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var currentPlaylistTracks: List<AudioTrackItem> = emptyList()
    private var currentTrackIndex: Int = 0

    fun parseTracksJson(json: String): List<AudioTrackItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<AudioTrackItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AudioTrackItem(
                        id = obj.optString("id", i.toString()),
                        title = obj.optString("title", "Pista ${i + 1}"),
                        uriString = obj.optString("uriString", ""),
                        durationText = obj.optString("durationText", "3:00")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing tracks JSON", e)
        }
        return list
    }

    fun serializeTracks(tracks: List<AudioTrackItem>): String {
        val array = JSONArray()
        tracks.take(20).forEach { track ->
            val obj = JSONObject()
            obj.put("id", track.id)
            obj.put("title", track.title)
            obj.put("uriString", track.uriString)
            obj.put("durationText", track.durationText)
            array.put(obj)
        }
        return array.toString()
    }

    fun playPlaylist(
        playlistId: Long,
        playlistName: String,
        ambientType: String,
        tracksJson: String
    ) {
        stopAll()
        val tracks = parseTracksJson(tracksJson)
        currentPlaylistTracks = tracks
        currentTrackIndex = 0

        if (tracks.isEmpty()) {
            // Procedural synthesized soundscape for this occasion
            startSynthAmbient(ambientType, playlistName, playlistId)
            return
        }

        playTrackAtIndex(0, playlistName, playlistId, ambientType)
    }

    private fun playTrackAtIndex(
        index: Int,
        playlistName: String,
        playlistId: Long,
        ambientType: String
    ) {
        if (index !in currentPlaylistTracks.indices) return
        currentTrackIndex = index
        val track = currentPlaylistTracks[index]

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(track.uriString))
                isLooping = false
                setVolume(_playbackState.value.volume, _playbackState.value.volume)
                setOnCompletionListener {
                    val nextIndex = (currentTrackIndex + 1) % currentPlaylistTracks.size
                    playTrackAtIndex(nextIndex, playlistName, playlistId, ambientType)
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error $what, $extra. Falling back to ambient synth.")
                    startSynthAmbient(ambientType, playlistName, playlistId)
                    true
                }
                prepare()
                start()
            }

            _playbackState.value = AudioPlaybackState(
                isPlaying = true,
                currentPlaylistName = playlistName,
                currentPlaylistId = playlistId,
                currentTrackTitle = track.title,
                currentAmbientType = ambientType,
                isSynthActive = false,
                volume = _playbackState.value.volume
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play user track from uri: ${track.uriString}", e)
            startSynthAmbient(ambientType, playlistName, playlistId)
        }
    }

    fun startSynthAmbient(ambientType: String, playlistName: String = "Atmósfera", playlistId: Long? = null) {
        stopAll()

        val sampleRate = 22050
        val bufferSize = AndroidAudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            val audioTrack = AndroidAudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
                .setTransferMode(AndroidAudioTrack.MODE_STREAM)
                .build()

            synthAudioTrack = audioTrack
            audioTrack.play()

            synthJob = scope.launch {
                val buffer = ShortArray(1024)
                var phase1 = 0.0
                var phase2 = 0.0

                // Base frequencies and modulation for the 10 rich RPG occasions
                val (baseFreq, modFreq, noiseFactor) = when {
                    ambientType.contains("Combate", ignoreCase = true) -> Triple(55.0, 110.0, 0.08) // Deep battle pulse
                    ambientType.contains("Sigilo", ignoreCase = true) -> Triple(73.42, 146.83, 0.02) // Subtle stealth whisper
                    ambientType.contains("Persecución", ignoreCase = true) || ambientType.contains("Fuga", ignoreCase = true) -> Triple(82.41, 164.81, 0.06) // Rapid adrenaline chase
                    ambientType.contains("Misterio", ignoreCase = true) || ambientType.contains("Investigación", ignoreCase = true) -> Triple(98.0, 103.83, 0.03) // Noir tension
                    ambientType.contains("Terror", ignoreCase = true) -> Triple(43.65, 46.25, 0.09) // Cosmic sub-bass dread
                    ambientType.contains("Romance", ignoreCase = true) -> Triple(196.0, 293.66, 0.01) // Intimate warmth
                    ambientType.contains("Melancolía", ignoreCase = true) -> Triple(130.81, 155.56, 0.04) // Melancholy rain drone
                    ambientType.contains("Espacial", ignoreCase = true) -> Triple(146.83, 220.0, 0.03) // Cosmic synth void
                    ambientType.contains("Cyberpunk", ignoreCase = true) -> Triple(87.31, 174.61, 0.07) // Neon electrical pulse
                    else -> Triple(110.0, 164.81, 0.02) // Camp Rest serenity
                }

                while (isActive) {
                    val vol = _playbackState.value.volume * 0.32f
                    for (i in buffer.indices) {
                        phase1 += 2.0 * Math.PI * baseFreq / sampleRate
                        phase2 += 2.0 * Math.PI * modFreq / sampleRate

                        val sample1 = sin(phase1)
                        val sample2 = sin(phase2)
                        val noise = (Math.random() - 0.5) * noiseFactor
                        val mixed = ((sample1 * 0.6 + sample2 * 0.35 + noise) * vol * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                        buffer[i] = mixed.toShort()
                    }
                    audioTrack.write(buffer, 0, buffer.size)
                }
            }

            _playbackState.value = AudioPlaybackState(
                isPlaying = true,
                currentPlaylistName = playlistName,
                currentPlaylistId = playlistId,
                currentTrackTitle = "Paisaje Sonoro: $ambientType",
                currentAmbientType = ambientType,
                isSynthActive = true,
                volume = _playbackState.value.volume
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start synth ambient", e)
        }
    }

    fun pause() {
        if (_playbackState.value.isSynthActive) {
            synthJob?.cancel()
            synthAudioTrack?.pause()
        } else {
            mediaPlayer?.pause()
        }
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun resume() {
        val state = _playbackState.value
        if (state.isSynthActive) {
            state.currentAmbientType?.let {
                startSynthAmbient(it, state.currentPlaylistName ?: "Atmósfera", state.currentPlaylistId)
            }
        } else {
            mediaPlayer?.start()
            _playbackState.value = state.copy(isPlaying = true)
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(clamped, clamped)
        _playbackState.value = _playbackState.value.copy(volume = clamped)
    }

    fun stopAll() {
        try {
            synthJob?.cancel()
            synthJob = null
            synthAudioTrack?.stop()
            synthAudioTrack?.release()
            synthAudioTrack = null

            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }
}
