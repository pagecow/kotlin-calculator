// Native Notify — your app's id and token (written by Agent Notify).
//
// These two values tell Native Notify which app a device belongs to. They live
// inside the app on purpose, and they are already filled in.
// Keep this repository private: anyone who has the token can send notifications
// to your users. To get a fresh token, rotate it on the Keys page of your
// project at https://dashboard.nativenotify.com, then paste it here.

package com.example.kotlincalculator

object NativeNotifyConfig {
    const val APP_ID: Int = 33963
    const val APP_TOKEN: String = "m4xZKdPFlGYiNqvlGdkf8O"
}
