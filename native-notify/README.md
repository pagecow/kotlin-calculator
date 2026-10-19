# Push notifications for Kotlin Calculator — set up by Agent Notify

This pull request set Native Notify up inside your Android project. You do not need to edit a file — what is left is the Firebase side, which only you can do (below).

## What this pull request did

- Added the Native Notify client (`NativeNotify.kt`) and `NativeNotifyConfig.kt` to `app/src/main/java/com/example/kotlincalculator`.
- Filled in your app id and app token — nothing to copy or paste.
- Added the permissions, the Firebase messaging service, a start-up provider and Firebase's default notification channel and icon to `AndroidManifest.xml` — nothing to call from your Activity. A push with an image shows it whether the app is open or closed.
- Added Firebase Cloud Messaging to Gradle (`app/build.gradle.kts` and `build.gradle.kts`). The Google services plugin only switches on once `google-services.json` is in `app`, so the project still builds before you add it.

## Before your first push

Only you can do these:

1. **Add your Firebase key in [Native Notify](https://dashboard.nativenotify.com):** open your project's **Setup & docs → Push notifications setup** and click **Set up** under **Connect Firebase (Android)**. The wizard shows where to create it (Firebase console → Project settings → Service accounts → Generate new private key).
2. **Run the app** from Android Studio on a phone, or on an [emulator](https://developer.android.com/studio/run/managing-avds) whose system image includes Google Play services (a "Google Play" or "Google APIs" image), and tap **Allow** when Android 13 or newer asks about notifications. There is nothing to build or install first.
3. **Tell me “send my first push”** — I will send it and we will check that it arrived.

## Where it runs

On an Android phone, or an emulator whose system image includes Google Play services — nothing to build or install first. Android Studio's Logcat (filter on `NativeNotify`) prints `register ok` once the device is registered, and Native Notify counts it as a subscribed device on **Push notifications**.

## Images in notifications

Fill in **Big picture** when you send from [Native Notify](https://dashboard.nativenotify.com), or set `bigPictureURL` in the API: an `https` JPEG or PNG under 1 MB. Android shows it when the app is closed or in the background (Firebase draws it), and `NativeNotify.show` draws it when the app is open.

## Send your first push

Say “send my first push” to Agent Notify, or press **Send push notification** under **Setup & docs → Push notifications setup** in [Native Notify](https://dashboard.nativenotify.com). It is a real notification and it is saved in the notification inbox (**Push notifications → Inbox** in Native Notify, where you can delete any you do not want).

## If something goes wrong

| What you see | What it means | Fix |
| --- | --- | --- |
| `Firebase is not set up in this app yet` in Logcat | `google-services.json` is missing from the app module. | Download it from the Firebase console (Project settings → Your apps) into the app module folder and sync Gradle. |
| `registration failed (401)` in Logcat | The app id or token in `NativeNotifyConfig` is wrong (for example a rotated token). | Paste the current token from the **Keys** page in [Native Notify](https://dashboard.nativenotify.com). |
| `SENDER_ID_MISMATCH` | The Firebase key in Native Notify belongs to a different Firebase project than `google-services.json`. | Upload a service-account key from the same Firebase project. |
| `UNREGISTERED` | The app was uninstalled or its token changed. | Run the app again so it registers a fresh token. |
| No permission prompt on Android 13+ | The prompt was answered once already. | In the phone's Settings app → Apps → your app → Notifications → allow. |
| Accepted, but nothing shows | Notifications are off for the app, or the emulator image has no Google Play services. | Allow notifications for the app; use a "Google Play" or "Google APIs" emulator image. |
| The text arrives without the image | The image is not an https JPEG or PNG, or it is over 1 MB (Firebase draws the picture when the app is closed and rejects larger ones). | Send a smaller https JPEG or PNG. |

## About the app token in your code

`app/src/main/java/com/example/kotlincalculator/NativeNotifyConfig.kt` holds your app id (33963) and your app token. Native Notify apps keep them inside the app on purpose, so nothing needs copying. Keep this repository private — anyone with the token can send notifications to your users. To get a fresh token, rotate it on the **Keys** page in [Native Notify](https://dashboard.nativenotify.com) and paste the new one into that file.

## Checklist

- [ ] Add google-services.json from the Firebase console to the app module
- [ ] Add your Firebase key in the Native Notify dashboard
- [ ] Run the app (emulator or phone) and tap Allow
- [ ] Send a push from Agent Notify and read the result
