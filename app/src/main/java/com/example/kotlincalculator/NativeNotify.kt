// Native Notify — push registration (added by Agent Notify).
//
// Gets this device's Firebase Cloud Messaging (FCM) token, registers it with
// Native Notify, asks for the notification permission on Android 13+, and shows
// the notifications Native Notify sends. Your app id and token are in
// NativeNotifyConfig.kt. There is nothing to call: NativeNotifyInitProvider
// starts all of it when the app launches (the same trick Firebase itself uses).
//
//   NativeNotify.setSubscriberId(context, "user-42")  link this device to one of YOUR users (call after login)
//   NativeNotify.deregister(context)                  stop pushes to this device (call on logout)
//   NativeNotify.start(activity)                      only if you remove the provider from AndroidManifest.xml
//
// Images: a push sent with a Big picture shows it. Firebase draws it itself while the app
// is closed or in the background; show() below draws it while the app is open.
//
// Already have a FirebaseMessagingService? Keep it, remove NativeNotifyMessagingService
// from AndroidManifest.xml and call NativeNotify.onNewToken(this, token) and
// NativeNotify.show(this, message) from yours.

package com.example.kotlincalculator

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone
import java.util.UUID

object NativeNotify {
    private const val TAG = "NativeNotify"
    private const val REGISTER_URL = "https://app.nativenotify.com/api/universal/device/register"
    private const val DEREGISTER_URL = "https://app.nativenotify.com/api/universal/device/deregister"
    private const val PREFS = "native_notify"
    private const val DEFAULT_CHANNEL = "default"
    private const val PERMISSION_REQUEST_CODE = 7741
    private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
    private const val MAX_IMAGE_SIDE = 1024

    @Volatile
    private var started = false

    /** Get this device's FCM token and register it. Pass an Activity to also ask for the notification permission (Android 13+). */
    @JvmStatic
    fun start(context: Context) {
        val app = context.applicationContext
        if (context is Activity) askForPermission(context)
        if (started) return
        started = true
        createChannel(app, DEFAULT_CHANNEL)
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    register(app, task.result!!)
                } else {
                    started = false
                    Log.w(TAG, "Firebase could not give this app a push token: ${task.exception?.message}")
                }
            }
        } catch (e: IllegalStateException) {
            started = false
            Log.w(TAG, "Firebase is not set up in this app yet. Download google-services.json from the Firebase console (Project settings → Your apps) into this module's folder, sync Gradle, and run again.")
        }
    }

    /** Called with a new FCM token (the messaging service does this for you). */
    @JvmStatic
    fun onNewToken(context: Context, token: String) {
        register(context.applicationContext, token)
    }

    /** Link this device to one of your own users (after login). Pass null to unlink it. */
    @JvmStatic
    fun setSubscriberId(context: Context, id: String?) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (id.isNullOrEmpty()) prefs.edit().remove("subscriberId").apply() else prefs.edit().putString("subscriberId", id).apply()
        prefs.getString("fcmToken", null)?.let { register(context.applicationContext, it) }
    }

    /** Stop pushes to this device (call on logout). */
    @JvmStatic
    fun deregister(context: Context) {
        val body = JSONObject().apply {
            put("appId", NativeNotifyConfig.APP_ID)
            put("appToken", NativeNotifyConfig.APP_TOKEN)
            put("deviceId", deviceId(context.applicationContext))
        }
        post(DEREGISTER_URL, body, "deregister")
    }

    /** Show a notification from a Native Notify message: its title and text and, when the push has one, its image. */
    @JvmStatic
    fun show(context: Context, message: RemoteMessage) {
        show(context, message.data, message.notification?.imageUrl?.toString())
    }

    /** Show a notification from a Native Notify data message (title / message / channelId / body), with the https image of the push when it has one. */
    @JvmStatic
    @JvmOverloads
    fun show(context: Context, data: Map<String, String>, imageUrl: String? = null) {
        val title = data["title"] ?: return
        val app = context.applicationContext
        val id = System.currentTimeMillis().toInt()
        if (imageUrl.isNullOrEmpty()) {
            post(app, id, data, title, null)
        } else if (Looper.myLooper() == Looper.getMainLooper()) {
            // Never download on the main thread: show the text now and swap in the picture when it arrives.
            post(app, id, data, title, null)
            Thread { loadImage(imageUrl)?.let { post(app, id, data, title, it) } }.start()
        } else {
            post(app, id, data, title, loadImage(imageUrl))
        }
    }

    /**
     * A tap on a notification that Firebase drew itself (a push with an image, while the app was closed) opens the app with
     * the push's data in plain extras. This copies its pushData to the "nn_push_data" extra that show() uses, so your code
     * reads one place. NativeNotifyInitProvider calls it for every activity; there is nothing to call yourself.
     */
    @JvmStatic
    fun adoptLaunchData(intent: Intent?) {
        val extras = intent?.extras ?: return
        if (extras.containsKey("nn_push_data") || !extras.containsKey("google.message_id")) return
        extras.getString("body")?.let { intent.putExtra("nn_push_data", it) }
    }

    // Internals

    private fun post(app: Context, id: Int, data: Map<String, String>, title: String, picture: Bitmap?) {
        val text = data["message"] ?: ""
        val channelId = data["channelId"] ?: DEFAULT_CHANNEL
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(app, channelId)

        // A tap opens the app; your pushData arrives as JSON in the "nn_push_data" extra.
        val launch = app.packageManager.getLaunchIntentForPackage(app.packageName)
        launch?.putExtra("nn_push_data", data["body"])
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val tap = launch?.let { PendingIntent.getActivity(app, id, it, flags) }

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(app, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(app)
        }
        builder.setSmallIcon(R.drawable.native_notify_small_icon).setContentTitle(title).setContentText(text).setAutoCancel(true)
        if (tap != null) builder.setContentIntent(tap)
        if (picture != null) {
            builder.setLargeIcon(picture)
            builder.setStyle(Notification.BigPictureStyle().bigPicture(picture).bigLargeIcon(null as Bitmap?))
        }
        manager.notify(id, builder.build())
    }

    /** Download the push's image (https only, at most 5 MB, 8 seconds) and shrink it to a size a notification can show. null = no picture. */
    private fun loadImage(url: String): Bitmap? {
        if (!url.startsWith("https://", ignoreCase = true)) return null
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                if (connection.responseCode !in 200..299) return null
                val bytes = connection.inputStream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (out.size() + read > MAX_IMAGE_BYTES) return null
                        out.write(buffer, 0, read)
                    }
                    out.toByteArray()
                }
                decode(bytes)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "the notification image could not be loaded — ${e.message}")
            null
        }
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_IMAGE_SIDE) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun askForPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < 33) return
        val permission = "android.permission.POST_NOTIFICATIONS"
        if (activity.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        activity.requestPermissions(arrayOf(permission), PERMISSION_REQUEST_CODE)
    }

    private fun createChannel(context: Context, id: String) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(id) == null) {
            manager.createNotificationChannel(NotificationChannel(id, "Notifications", NotificationManager.IMPORTANCE_HIGH))
        }
    }

    private fun register(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString("fcmToken", token).apply()
        if (NativeNotifyConfig.APP_ID <= 0 || NativeNotifyConfig.APP_TOKEN.isEmpty() || NativeNotifyConfig.APP_TOKEN.startsWith("PASTE_")) {
            Log.w(TAG, "The app id / app token are not set — open NativeNotifyConfig.kt and paste them in.")
            return
        }
        val body = JSONObject().apply {
            put("appId", NativeNotifyConfig.APP_ID)
            put("appToken", NativeNotifyConfig.APP_TOKEN)
            put("deviceId", deviceId(context))
            put("platform", "android")
            put("timezone", TimeZone.getDefault().id)
            put("tokens", JSONObject().put("fcmToken", token))
            try {
                put("appVersion", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
            } catch (e: Exception) {
                // the version is optional
            }
            prefs.getString("subscriberId", null)?.let { put("subscriberId", it) }
        }
        post(REGISTER_URL, body, "register")
    }

    /** One id per install: ANDROID_ID is stable for this app on this device (it survives reinstalls); a stored UUID is the fallback. */
    private fun deviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        if (!androidId.isNullOrBlank()) return androidId
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString("deviceId", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("deviceId", it).apply() }
    }

    private fun post(url: String, body: JSONObject, label: String) {
        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = connection.responseCode
                if (code in 200..299) {
                    Log.i(TAG, "$label ok — this device can now receive pushes.")
                } else {
                    val detail = (connection.errorStream ?: connection.inputStream).bufferedReader().use { it.readText() }
                    Log.w(TAG, "$label failed ($code) $detail")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "$label failed — ${e.message}")
            }
        }.start()
    }
}

/** Receives the FCM token (and every notification Native Notify sends) and hands them to NativeNotify. */
class NativeNotifyMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        NativeNotify.onNewToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        NativeNotify.show(applicationContext, message)
    }
}

/** Starts NativeNotify when the app launches — no code to add to your Activity. */
class NativeNotifyInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val app = context?.applicationContext as? Application ?: return true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var started = false

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                NativeNotify.adoptLaunchData(activity.intent)
            }

            override fun onActivityResumed(activity: Activity) {
                NativeNotify.adoptLaunchData(activity.intent)
                if (started) return
                started = true
                NativeNotify.start(activity)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
