package com.zhuandian.bluetooth;

import android.app.Application;

import cn.bmob.v3.Bmob;

/**
 * desc :
 * author：xiedong
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Bmob.initialize(this, "e1a748d320a1a3eed70f2acb9f8c6222");
        // 本地崩溃log
        MyCrashHandler mCrashHandler = MyCrashHandler.getInstance();
        mCrashHandler.init(this);
        Thread.setDefaultUncaughtExceptionHandler(mCrashHandler);
    }
}
