package com.aashdit.myimageupload;

import android.app.Application;

import androidx.multidex.MultiDex;

import io.realm.Realm;

public class App  extends Application {
    private static final String TAG = "App";
    private static App mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(this);
        Realm.init(this);
        mInstance = this;
    }
    public static synchronized App getInstance() {
        return mInstance;
    }

    public void setConnectivityListener(ConnectivityChangeReceiver.ConnectivityReceiverListener listener) {
        new ConnectivityChangeReceiver().connectivityReceiverListener = listener;
    }
}
