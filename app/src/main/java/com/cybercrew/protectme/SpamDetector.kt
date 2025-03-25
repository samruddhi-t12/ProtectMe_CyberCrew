package com.cybercrew.protectme

import android.content.Context
import android.util.Log
import java.nio.FloatBuffer

class SpamDetector(context: Context) {
    private val modelHelper = AIModelHelper(context)

    fun classifySMS(message: String): Boolean {
        // Convert text to feature vector (Assuming each letter = 1.0f for demo)
        val inputVector = FloatArray(message.length) { 1.0f }

        // Run prediction
        val prediction = modelHelper.predict(inputVector)

        // If output > 0.5, classify as spam
        val isSpam = prediction > 0.5f
        Log.d("AI_MODEL", if (isSpam) "⚠️ SPAM Detected: $message" else "✅ Safe SMS: $message")
        return isSpam
    }
}
