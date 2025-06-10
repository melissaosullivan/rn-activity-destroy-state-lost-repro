package com.reproducerapp;

import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class ActivityPackage extends TurboReactPackage {

  @Override
  public NativeModule getModule(
      String name, ReactApplicationContext reactContext) {
    return new ActivityModule(reactContext);
  }

  @Override
  public ReactModuleInfoProvider getReactModuleInfoProvider() {
    return () -> {
      Map<String, ReactModuleInfo> map = new HashMap<>();
      map.put(
          "ActivityModule",
          new ReactModuleInfo(
              "ActivityModule",
              "com.react_native_crash.ActivityModule",
              false,
              false,
              true,
              false,
              false));
      return map;
    };
  }
}