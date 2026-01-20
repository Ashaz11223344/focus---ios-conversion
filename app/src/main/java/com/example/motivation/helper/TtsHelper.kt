package com.example.motivation.helper

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsHelper(context: Context, private val onInit: (Boolean) -> Unit) : TextToSpeech.OnInitListener {

    private val tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts.language = Locale.US
            onInit(true)
        } else {
            isInitialized = false
            Log.e("TtsHelper", "TTS initialization failed.")
            onInit(false)
        }
    }

    fun speak(text: String, gender: String) {
        if (!isInitialized) {
            Log.w("TtsHelper", "TTS not initialized, cannot speak.")
            return
        }

        // On API 33+, try to use reflection to get the 'gender' property.
        // This bypasses the strange compile-time errors.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val desiredGenderInt = if (gender == "Male") 1 else 2 // Male is 1, Female is 2

                val desiredVoice = tts.voices.find { voice ->
                    val voiceGender = voice::class.java.getMethod("getGender").invoke(voice) as Int
                    voice.locale == Locale.US && voiceGender == desiredGenderInt
                }

                if (desiredVoice != null) {
                    tts.voice = desiredVoice
                    Log.d("TtsHelper", "Using voice (API 33+): ${desiredVoice.name}")
                } else {
                    Log.w("TtsHelper", "No matching API 33+ voice found for gender: $gender. Falling back.")
                    findVoiceByName(gender)
                }
            } catch (e: Exception) {
                Log.e("TtsHelper", "Failed to use reflection for gender. Falling back.", e)
                findVoiceByName(gender)
            }
        } else {
            findVoiceByName(gender)
        }
        
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun findVoiceByName(gender: String) {
        val desiredVoice = if (gender == "Male") {
            tts.voices.find {
                it.locale == Locale.US && it.name.contains("male", ignoreCase = true)
            } ?: tts.voices.find {
                it.locale == Locale.US && !it.name.contains("female", ignoreCase = true)
            }
        } else { // For "Female"
            tts.voices.find {
                it.locale == Locale.US && it.name.contains("female", ignoreCase = true)
            }
        }

        if (desiredVoice != null) {
            tts.voice = desiredVoice
            Log.d("TtsHelper", "Using fallback voice: ${desiredVoice.name}")
        } else {
            tts.voice = tts.defaultVoice
            Log.w("TtsHelper", "No matching fallback voice found. Using default.")
        }
    }

    fun shutdown() {
        if (isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}
