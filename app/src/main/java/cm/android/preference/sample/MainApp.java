package cm.android.preference.sample;

import android.app.Application;
import android.content.Context;

import cm.android.extpreference.sample.BuildConfig;
import cm.android.preference.ext.ExtPreferences;
import cm.android.preference.ext.PrefsConfig;

public class MainApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ExtPreferences.attach(new PrefsConfig.Builder()
                .authority(BuildConfig.APPLICATION_ID + ".preference.provider")
                .build());
    }
}
