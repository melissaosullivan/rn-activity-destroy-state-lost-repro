# React Native Activity State Loss Reproducer

This project demonstrates an issue in React Native where Android Activity callbacks are lost when an app is killed in the background and then restored.

## Issue

When an Android activity starts another activity (like a browser) and the app is killed by the system while in the background, returning to the app can cause:
- The `onActivityResult` callback to be called before React Native is fully initialized
- The JavaScript callback that was registered is lost/null
- No way to communicate the result back to React Native

**Active Issue**: [React Native Issue #52114](https://github.com/facebook/react-native/issues/52114)

**Previous Issues** (for past context):
- [React Native Issue #30277](https://github.com/facebook/react-native/issues/30277)
- Originally reported in [react-native-plaid-link-sdk](https://github.com/plaid/react-native-plaid-link-sdk)

**Reproduction Rate**: 100% reproducible when following the steps exactly as described


## Building the App

```
cd ReproducerApp
npm install
npm start
npm run android
```

## Reproduction Steps

1. Run the app on an Android device
2. Tap the "Open Activity" button
3. In the second activity, tap "Go to Web" to open Google in a browser
4. Use the following command to forcibly kill the app:
   ```
   adb shell am kill com.reproducerapp
   ```
5. Resume the activity with:
   ```
   adb shell am start -n com.reproducerapp/.RedirectActivity -d "redirect://crash"
   ```
6. Tap "Return Result" in the activity
7. Check logcat for missing "RESULT" and "Completed" logs

### Expected Behavior
When returning to the app after it's been killed, the callback should work and you should see "RESULT" and "Completed" in the logs.

### Actual Behavior
When the app is killed and then restored, the activity result is processed before React Native is ready. The JavaScript callback is not executed, and you won't see "RESULT" or "Completed" logs, indicating the callback was lost.

## Environment
- React Native 0.79.2
- Android 15 (Pixel 7)
- Build ID: BP1A.250505.005.B1

## Implementation Details

This reproduction uses a simple pattern:
1. Native module (`ActivityModule`) that implements `ActivityEventListener`
2. JS method that takes a callback and starts an activity for result
3. Second activity that can launch external browser (Google.com)
4. Simple intent handling for activity results

When the app process is killed while in the background (e.g., when browser is open), the activity result mechanism still works at the Android level, but the React Native JavaScript callback is lost because the JS context must be rebuilt from scratch and it is not available when onActivityResult() is called.


### React Native Version

0.79.2

### Affected Platforms

Runtime - Android

### Output of `npx @react-native-community/cli info`

```text
System:
  OS: macOS 15.5
  CPU: (10) arm64 Apple M1 Max
  Memory: 268.70 MB / 32.00 GB
  Shell:
    version: "5.9"
    path: /bin/zsh
Binaries:
  Node:
    version: 18.20.8
    path: ~/.nvm/versions/node/v18.20.8/bin/node
  Yarn:
    version: 1.22.19
    path: /opt/homebrew/bin/yarn
  npm:
    version: 10.9.2
    path: ~/.nvm/versions/node/v18.20.8/bin/npm
  Watchman:
    version: 2025.04.28.00
    path: /opt/homebrew/bin/watchman
Managers:
  CocoaPods: Not Found
SDKs:
  iOS SDK: Not Found
  Android SDK:
    API Levels:
      - "30"
      - "31"
      - "32"
      - "33"
      - "33"
      - "34"
      - "35"
    Build Tools:
      - 28.0.3
      - 29.0.2
      - 30.0.2
      - 30.0.3
      - 33.0.1
      - 34.0.0
      - 34.0.0
      - 35.0.0
      - 35.0.0
      - 36.0.0
    System Images:
      - android-26 | Google APIs ARM 64 v8a
      - android-27 | ARM 64 v8a
      - android-27 | Google Play Intel x86 Atom
      - android-28 | ARM 64 v8a
      - android-28 | Google APIs ARM 64 v8a
      - android-28 | Google ARM64-V8a Play ARM 64 v8a
      - android-29 | Google APIs ARM 64 v8a
      - android-29 | Google Play ARM 64 v8a
      - android-30 | Google Play ARM 64 v8a
      - android-31 | Google Play ARM 64 v8a
      - android-33 | Google APIs ARM 64 v8a
      - android-34 | Google Play ARM 64 v8a
      - android-35 | Google APIs ARM 64 v8a
      - android-35 | Google Play ARM 64 v8a
      - android-UpsideDownCake | Google APIs ARM 64 v8a
    Android NDK: Not Found
IDEs:
  Android Studio: 2024.3 AI-243.24978.46.2431.13363775
  Xcode:
    version: /undefined
    path: /usr/bin/xcodebuild
Languages:
  Java:
    version: 17.0.8
    path: /usr/bin/javac
  Ruby:
    version: 2.6.10
    path: /usr/bin/ruby
npmPackages:
  "@react-native-community/cli":
    installed: 18.0.0
    wanted: 18.0.0
  react:
    installed: 19.0.0
    wanted: 19.0.0
  react-native:
    installed: 0.79.2
    wanted: 0.79.2
  react-native-macos: Not Found
npmGlobalPackages:
  "*react-native*": Not Found
Android:
  hermesEnabled: true
  newArchEnabled: true
iOS:
  hermesEnabled: Not found
  newArchEnabled: false

info React Native v0.80.0 is now available (your project is running on v0.79.2).
info Changelog: https://github.com/facebook/react-native/releases/tag/v0.80.0
info Diff: https://react-native-community.github.io/upgrade-helper/?from=0.79.2&to=0.80.0
info For more info, check out "https://reactnative.dev/docs/upgrading?os=macos".
```

### Stacktrace or Logs

```text
unknown:ReactHost com.reproducerapp E  Unhandled SoftException (Ask Gemini)
                                        com.facebook.react.bridge.ReactNoCrashSoftException: raiseSoftException(onActivityResult(activity = "com.reproducerapp.MainActivity@1afbd28", requestCode = "100", resultCode = "-1", data = "Intent { (has extras) }")): Tried to access onActivityResult while context is not ready
                                          at com.facebook.react.runtime.ReactHostImpl.raiseSoftException(ReactHostImpl.java:1025)
                                          at com.facebook.react.runtime.ReactHostImpl.raiseSoftException(ReactHostImpl.java:1018)
                                          at com.facebook.react.runtime.ReactHostImpl.onActivityResult(ReactHostImpl.java:776)
                                          at com.facebook.react.ReactDelegate.onActivityResult(ReactDelegate.java:187)
                                          at com.facebook.react.ReactActivityDelegate.onActivityResult(ReactActivityDelegate.java:192)
                                          at com.facebook.react.ReactActivity.onActivityResult(ReactActivity.java:79)
                                          at android.app.Activity.onActivityResult(Activity.java:7571)
                                          at android.app.Activity.internalDispatchActivityResult(Activity.java:9470)
                                          at android.app.Activity.dispatchActivityResult(Activity.java:9447)
                                          at android.app.ActivityThread.deliverResults(ActivityThread.java:6025)
                                          at android.app.ActivityThread.performResumeActivity(ActivityThread.java:5454)
                                          at android.app.ActivityThread.handleResumeActivity(ActivityThread.java:5500)
                                          at android.app.servertransaction.ResumeActivityItem.execute(ResumeActivityItem.java:73)
                                          at android.app.servertransaction.ActivityTransactionItem.execute(ActivityTransactionItem.java:63)
                                          at android.app.servertransaction.TransactionExecutor.executeLifecycleItem(TransactionExecutor.java:169)
                                          at android.app.servertransaction.TransactionExecutor.executeTransactionItems(TransactionExecutor.java:101)
                                          at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:80)
                                          at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2773)
                                          at android.os.Handler.dispatchMessage(Handler.java:109)
                                          at android.os.Looper.loopOnce(Looper.java:232)
                                          at android.os.Looper.loop(Looper.java:317)
                                          at android.app.ActivityThread.main(ActivityThread.java:8934)
                                          at java.lang.reflect.Method.invoke(Native Method)
                                          at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:591)
                                          at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:911)
```
