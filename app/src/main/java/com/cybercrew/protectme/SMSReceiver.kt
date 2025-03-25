package com.cybercrew.protectme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.w("SMS_DEBUG", "❌ SMS Broadcast blocked on Android 12+")
            return
        }

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                val sender = smsMessage.originatingAddress
                val message = smsMessage.messageBody

                Log.d("SMS_DEBUG", "📩 Sender: $sender, Message: $message")

            }
        }
    }
}