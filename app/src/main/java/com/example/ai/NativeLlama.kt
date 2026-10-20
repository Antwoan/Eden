package com.example.ai

import android.util.Log

object NativeLlama {
    private const val TAG = "NativeLlama"
    var isLibraryLoaded: Boolean = false
        private set

    init {
        try {
            System.loadLibrary("llama-android")
            isLibraryLoaded = true
            Log.i(TAG, "llama-android native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library llama-android not found in environment, fallback engine active")
            isLibraryLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading native library", e)
            isLibraryLoaded = false
        }
    }

    interface TokenCallback {
        fun onToken(token: String)
        fun onComplete()
        fun onError(error: String)
    }

    // JNI Native methods
    external fun loadModel(modelPath: String, nThreads: Int, contextSize: Int): Long
    external fun generateCompletion(
        modelHandle: Long,
        prompt: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        callback: TokenCallback
    )
    external fun freeModel(modelHandle: Long)
}
