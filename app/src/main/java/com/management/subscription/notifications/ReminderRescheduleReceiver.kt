package com.management.subscription.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

class ReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        Thread {
            try {
                ReminderNotifier.ensureChannel(appContext)
                runBlocking {
                    ReminderScheduler.getInstance(appContext).sync()
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
