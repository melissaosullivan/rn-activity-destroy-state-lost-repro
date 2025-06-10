package com.reproducerapp;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

class ActivityModule extends ReactContextBaseJavaModule implements ActivityEventListener {
  private static int REQUEST_CODE = 100;
  private static ReactApplicationContext reactContext;
  private Callback onComplete = null;

  ActivityModule(ReactApplicationContext context) {
    super(context);
    reactContext = context;
  }

  @Override
  public void initialize() {
    super.initialize();
    getReactApplicationContext().addActivityEventListener(this);
  }

  @Override
  public void onCatalystInstanceDestroy() {
    super.onCatalystInstanceDestroy();
    getReactApplicationContext().removeActivityEventListener(this);
  }

  @NonNull
  @Override
  public String getName() {
    return "ActivityModule";
  }

  @ReactMethod
  @SuppressWarnings("unused")
  public void startLinkActivityForResult(Callback onComplete) {
    Activity activity = getCurrentActivity();
    if (activity != null) {
      this.onComplete = onComplete;
      activity.startActivityForResult(new Intent(activity, SecondActivity.class), REQUEST_CODE);
    }
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
    if (requestCode == REQUEST_CODE) {
      if (intent == null) {
        return;
      }
      String x = intent.getStringExtra("RESULT");
      if (onComplete != null) {
        onComplete.invoke("Completed");
      }
    }
  }

  @Override
  public void onNewIntent(Intent intent) {
    // Do Nothing
  }
}
