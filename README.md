# React Native Activity State Loss Reproducer

This project demonstrates an issue in React Native where Android Activity callbacks are lost when an app is killed in the background and then restored.

## Issue

When an Android activity starts another activity (like a browser) and the app is killed by the system while in the background, returning to the app can cause:
- The `onActivityResult` callback to be called before React Native is fully initialized
- The JavaScript callback that was registered is lost/null
- No way to communicate the result back to React Native

**Reference**: [React Native Issue #30277](https://github.com/facebook/react-native/issues/30277)

**Reproduction Rate**: 100% reproducible when following the steps exactly as described

**Related Issues**:
- Originally reported in [react-native-plaid-link-sdk](https://github.com/plaid/react-native-plaid-link-sdk)
- Similar to [Issue #21455](https://github.com/facebook/react-native/issues/21455)


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

## Technical Explanation

If a native module opens an activity that redirects out of the application and the application is killed in the background by the Android OS, the react context is lost and the native module's activity is unable to call onActivityResult.

If the redirect from the third party app is back to the activity in the native module, the ReactActivity is not created at this point. When the native activity then sets result and finishes to return back to the ReactActivity, the ReactActivity has to start up.

The React Native startup flow is:
```
ReactActivity#onCreate
ReactActivityDelegate#onCreate
ReactActivityDelegate#loadApp
ReactDelegate#loadApp
ReactRootView#startReactApplication
ReactInstanceManager#createReactContextInBackground
ReactInstanceManager#recreateReactContextInBackgroundInner
ReactInstanceManager#recreateReactContextInBackgroundFromBundleLoader
ReactInstanceManager#recreateReactContextInBackground
ReactInstanceManager#runCreateReactContextOnNewThread
```

At this point `ReactInstanceManager#setupReactContext` is called on a background thread.

Meanwhile, `ReactActivity#onActivityResult` is called. In the Android activity lifecycle, `onActivityResult` is called even before `onResume`. The calls from `ReactActivity#onActivityResult` pass through a few layers and end up at `ReactInstanceManager#onActivityResult` which checks if the ReactContext is null:

```java
@ThreadConfined(UI)
public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
  ReactContext currentContext = getCurrentReactContext();
  if (currentContext != null) {
    currentContext.onActivityResult(activity, requestCode, resultCode, data);
  }
}
```

Because the context is being built on a background thread, there is no guarantee that `currentContext` will be initialized at this point. Furthermore, `ReactInstanceManager#createReactContext` is a fairly intensive function, so it is actually very likely that `currentContext` is null.

This means that `ReactContext#onActivityResult` is not called and therefore, the data cannot be passed back to the `ActivityEventListener` which in this sample is the `ReactContextBaseJavaModule`.
