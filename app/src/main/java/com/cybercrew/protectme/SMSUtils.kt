package com.cybercrew.protectme

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log

object SMSUtils {
    private lateinit var spamDetector: SpamDetector

    fun initialize(context: Context) {
        if (!::spamDetector.isInitialized) {
            spamDetector = SpamDetector(context)
            Log.d("SMS_DEBUG", "✅ SpamDetector Initialized")
        }

    }

    fun fetchLatestSMS(context: Context) {
        val detector = spamDetector // ✅ Use existing instance
        val uri = Uri.parse("content://sms/inbox")
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000) // 1 hour ago
        val prefs = context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE)
        val lastProcessedTimestamp = prefs.getLong("last_processed_sms", 0L)
        var latestTimestamp = lastProcessedTimestamp // ✅ Track latest timestamp

        val cursor = context.contentResolver.query(
            uri, arrayOf("body", "date", "address"),
            "date > ? AND date > ?", arrayOf(oneHourAgo.toString(), lastProcessedTimestamp.toString()), "date ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val message = it.getString(it.getColumnIndexOrThrow("body"))
                val sender = it.getString(it.getColumnIndexOrThrow("address"))
                val newTimestamp = it.getLong(it.getColumnIndexOrThrow("date"))

                Log.d("SMS_DEBUG", "📩 New SMS from $sender: $message")

                // ✅ Call AI model for classification
                AIModelHelper(context).classifyMessage(sender, message)

                // ✅ Keep track of the latest timestamp
                if (newTimestamp > latestTimestamp) {
                    latestTimestamp = newTimestamp
                }
            }
        }

        // ✅ Store latest processed timestamp AFTER processing all messages
        if (latestTimestamp > lastProcessedTimestamp) {
            prefs.edit().putLong("last_processed_sms", latestTimestamp).apply()
        }
    }
}
