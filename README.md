# React Native Activity State Loss Reproducer

This project demonstrates an issue in React Native where Android Activity callbacks are lost when an app is killed in the background and then restored.

## Issue

When an Android activity starts another activity (like a browser) and the app is killed by the system while in the background, returning to the app can cause:
- The `onActivityResult` callback to be called before React Native is fully initialized
- The JavaScript callback that was registered is lost/null
- No way to communicate the result back to React Native

**Reference**: [React Native Issue #30277](https://github.com/facebook/react-native/issues/30277)

## Prerequisites

Enable the following settings in Android Developer Options:
1. Set "Background process limit" to "No background processes"
2. Enable "Don't keep activities" option

These settings help simulate low memory conditions where apps are more likely to be killed in the background.

## Reproduction Steps

1. Build and run the app on an Android device
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
6. Check logcat for missing "RESULT" and "Completed" logs

### Expected Behavior
When returning to the app after it's been killed, the callback should work and you should see "RESULT" and "Completed" in the logs.

### Actual Behavior
When the app is killed and then restored, the activity result is processed before React Native is ready. The JavaScript callback is not executed, and you won't see "RESULT" or "Completed" logs, indicating the callback was lost.

## Environment
- React Native 0.73.x
- Android 10+
