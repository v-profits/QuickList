package com.quicklist.app.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * In-app speech recognition (no system dialog). Partial results are streamed to [onText],
 * and the system beeps can be muted while listening.
 */
class VoiceInput(
    private val context: Context,
    private val shouldMute: () -> Boolean,
    private val onListening: (Boolean) -> Unit,
    private val onText: (String, Boolean) -> Unit,
    private val onError: (Int) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var recognizer: SpeechRecognizer? = null
    private val mutedStreams = mutableListOf<Int>()
    private val unmuteRunnable = Runnable { unmuteNow() }

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        handler.removeCallbacks(unmuteRunnable)
        recognizer?.destroy()
        recognizer = null
        mute()

        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onListening(true) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                finish()
                this@VoiceInput.onError(error)
            }

            override fun onResults(results: Bundle?) {
                val text = first(results)
                finish()
                if (text != null) onText(text, true)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                first(partialResults)?.let { onText(it, false) }
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, context.resources.configuration.locales[0].toLanguageTag())
        r.startListening(intent)
    }

    /** Stops listening; the final result still arrives through [onText]. */
    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        handler.removeCallbacks(unmuteRunnable)
        recognizer?.destroy()
        recognizer = null
        unmuteNow()
    }

    private fun first(b: Bundle?): String? =
        b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }

    private fun finish() {
        onListening(false)
        recognizer?.destroy()
        recognizer = null
        // Keep the streams muted a little longer so the "end of speech" beep is silenced too.
        handler.postDelayed(unmuteRunnable, 700)
    }

    private fun mute() {
        if (!shouldMute() || mutedStreams.isNotEmpty()) return
        for (s in listOf(AudioManager.STREAM_MUSIC, AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_SYSTEM)) {
            try {
                if (!audio.isStreamMute(s)) {
                    audio.adjustStreamVolume(s, AudioManager.ADJUST_MUTE, 0)
                    mutedStreams.add(s)
                }
            } catch (_: SecurityException) {
            }
        }
    }

    private fun unmuteNow() {
        for (s in mutedStreams) {
            try {
                audio.adjustStreamVolume(s, AudioManager.ADJUST_UNMUTE, 0)
            } catch (_: SecurityException) {
            }
        }
        mutedStreams.clear()
    }
}
