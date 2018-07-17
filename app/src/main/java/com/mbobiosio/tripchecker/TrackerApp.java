package com.mbobiosio.tripchecker;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

/**
 * Created by Mbuodile Obiosio on 7/17/18
 * cazewonder@gmail.com
 */
public class TrackerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}
