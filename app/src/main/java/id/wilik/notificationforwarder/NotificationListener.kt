package id.wilik.notificationforwarder

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notificationText = sbn.notification.extras.getString("android.text")

        Log.d("NotificationListener", "$packageName: $notificationText")

        // Skip process if the text is null
        if (notificationText == null) {
            Log.d("NotificationListener", "Notification text is null")
            return
        }

        NotificationHandlers.rules
            .find { it.packageName == packageName }
            ?.let { rule ->
                val pattern = Pattern.compile(rule.regexPattern)
                val matcher = pattern.matcher(notificationText)

                if (matcher.matches()) {
                    val groups = (1..matcher.groupCount()).map {
                        matcher.group(it) ?: ""
                    }.toTypedArray()

                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            if (groups.isEmpty()) {
                                TelegramSender.sendMessage(rule.messageTemplate)
                            } else {
                                TelegramSender.sendMessage(String.format(rule.messageTemplate, *groups))
                            }
                        } catch (e: Exception) {
                            Log.e("NotificationListener", "Error sending message", e)
                            e.printStackTrace()
                        }
                    }
                } else {
                    Log.d("NotificationListener", "No pattern match for $packageName")
                }
            }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Do nothing here
    }
}