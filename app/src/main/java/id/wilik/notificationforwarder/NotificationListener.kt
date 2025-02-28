package id.wilik.notificationforwarder

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notificationText = sbn.notification.extras.getString("android.text")

        Log.d("NotificationListener", "Notification from: $packageName")
        Log.d("NotificationListener", "Notification text: $notificationText")

        // Skip process if the notification is not from "id.dana" app or the text is null
        if (!packageName.equals("id.dana") || notificationText == null) {
            return
        }

        // Regex matching to parse the notification text
        val danaQris: Pattern = Pattern.compile("^Kamu berhasil menerima (.*) via (.*) ke akunmu. Cek yuk!\$")
        val danaQrisMatcher: Matcher = danaQris.matcher(notificationText)

        if (danaQrisMatcher.matches()) {
            val paymentMethod = danaQrisMatcher.group(2)
            val amount = danaQrisMatcher.group(1)

            Log.d("NotificationListener", "Payment method : $paymentMethod")
            Log.d("NotificationListener", "Amount : $amount")

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    TelegramSender.sendMessage("Dana QRIS diterima.\n\nMetode: *$paymentMethod*\nNominal: $amount")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Do nothing here
    }
}