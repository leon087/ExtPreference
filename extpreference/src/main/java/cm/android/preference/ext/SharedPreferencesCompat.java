package cm.android.preference.ext;

import android.content.SharedPreferences;
import android.os.Build;

public class SharedPreferencesCompat {
    public static void apply(SharedPreferences.Editor editor) {
        if (Build.VERSION.SDK_INT >= 9) {
            try {
                editor.apply();
            } catch (AbstractMethodError unused) {
                // The app injected its own pre-Gingerbread
                // SharedPreferences.Editor implementation without
                // an apply method.
                editor.commit();
            }
        } else {
            editor.commit();
        }
    }
}
