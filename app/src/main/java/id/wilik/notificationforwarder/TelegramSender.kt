package id.wilik.notificationforwarder

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Request
import java.io.IOException
import kotlin.text.Typography.times

object TelegramSender {
    private val client = OkHttpClient()
    private const val BOT_TOKEN = BuildConfig.TELEGRAM_BOT_TOKEN
    private const val CHAT_ID = BuildConfig.TELEGRAM_CHAT_ID
    private const val TELEGRAM_API_URL = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"

    suspend fun sendMessage(message: String) {
        val json = """{"chat_id":"$CHAT_ID","text":"$message"}"""
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(TELEGRAM_API_URL)
            .post(body)
            .build()

        retryIO {
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                }
            }
        }

//        withContext(Dispatchers.IO) {
//            client.newCall(request).execute().use { response: Response ->
//                if (!response.isSuccessful) throw IOException("Unexpected code $response")
//            }
//        }
    }

    private suspend fun <T> retryIO(
        times: Int = 60,
        initialDelay: Long = 1000,
        maxDelay: Long = 600000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            Log.e("TelegramSender", "Times ${times} currentDelay ${currentDelay}")
            try {
                return block()
            } catch (e: IOException) {
//                logMessages.add(e.localizedMessage ?: "")
//                e.printStackTrace()

                Log.e("TelegramSender", "IOException")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // last attempt
    }
}