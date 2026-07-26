# Friday — native widget starter kit

Three real pieces, not a mockup. Each needs its own tool to build:

```
friday-native/
├── server/    Node.js backend — holds your Claude API key safely
├── android/   Kotlin app: chat screen (MainActivity), widget, quick actions
└── ios/       Swift app: chat screen (ContentView), widget, quick actions
```

## Now includes a full working app screen, not just the widget
- `android/.../MainActivity.kt` — the chat screen the widget opens into, plus
  a quick-actions row (flashlight, Wi-Fi panel, Bluetooth panel, open app).
- `android/app/src/main/AndroidManifest.xml` and `app/build.gradle` — wire the
  activity, widget receiver, and dependencies together. Open the whole
  `android/` folder in Android Studio and it should sync and build.
- `ios/Friday/ContentView.swift` — same chat screen for iOS, with its own
  quick-actions row. `FridayApp` is the app's entry point (`@main`).
  In Xcode: create a new iOS App project named "Friday", then drop in
  `ContentView.swift` and `FridayQuickActions.swift` replacing the generated
  ones. Add the Widget Extension target from the earlier step alongside it.

## Why a backend exists
Phone apps are just files anyone can inspect. If your Anthropic API key were
inside the Android or iOS app, anyone could pull it out and use it on your
bill. The `server/` folder is a small proxy: it holds the real key as an
environment variable, and both apps call *it* instead of Anthropic directly.

## Setup order

**1. Backend**
```
cd server
npm install
export ANTHROPIC_API_KEY=sk-ant-...
node server.js
```
Deploy it somewhere reachable from your phone — Render, Railway, Fly.io, or
any small VPS all work. Note the public URL.

**2. Android**
- Open `android/` as a project in Android Studio.
- In `FridayWidgetProvider.kt`, set `BACKEND_URL` to your deployed server and
  `MONITOR_TOPIC` to whatever you want tracked.
- Add the `MainActivity` quick-chat screen (not included here — this kit
  covers the widget; the full chat screen can reuse the same UI from the
  Friday web app I built earlier, ported to Compose).
- Run on your device. Long-press home screen → Widgets → Friday.

**3. iOS**
- In Xcode, create the main app target first, then add a Widget Extension
  named `FridayWidget` and swap in `FridayWidget.swift`.
- Set `BACKEND_URL` and `MONITOR_TOPIC` at the top of the file.
- Build to your device (a free Apple ID works for local testing on your own
  phone; publishing to the App Store needs a $99/year Developer account).
- Long-press Lock Screen → Customize → add the Friday widget.

## Device controls (flashlight, Bluetooth, Wi-Fi, opening apps)
Added: `FridayQuickActions.kt` (Android) and `FridayQuickActions.swift` (iOS).

- **Flashlight** — works silently on both platforms, no dialogs.
- **Opening another app** — works on both, if that app supports being
  launched (Android: package name; iOS: the app must register a URL scheme).
- **Wi-Fi / Bluetooth** — neither OS lets a third-party app silently flip
  these anymore (Android since v10/v13, iOS since v13) — both makers locked
  it down after apps abused it for location tracking without GPS permission.
  The code opens the relevant system panel/settings screen instead, which is
  the closest real behavior available. There is no permission or setting
  that restores silent toggling on either platform.

## What's real vs. what's not, one more time
- **Android**: can show a live-updating widget on the home screen, and on
  lock screens for OEMs/launchers that support keyguard widgets.
- **iOS**: lock-screen widgets are glance-only by Apple's design — text and
  icons that refresh periodically, no typing directly on the lock screen.
  Tapping opens the app straight to chat.
- Neither platform lets a widget run true background "monitoring" with push
  alerts unless you add push notifications (APNs / FCM) — a further step
  beyond this starter kit, doable later if you want it.

This is a working skeleton, not a store-ready app: no app icon assets, no
onboarding, no push notifications yet. It's meant to get you from zero to
"widget on my phone talking to Claude" with the fewest blockers.
