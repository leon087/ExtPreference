package cm.android.preference.security.util;

import android.content.SharedPreferences;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cm.android.preference.security.PreferenceFactory;
import cm.android.preference.security.SecureSharedPreferences;
import cm.android.preference.security.SecureSharedPreferences2;

public class CompatUtil {
    public static void upgrade(SecureSharedPreferences old, SecureSharedPreferences2 prefs) {
        if (old.getVersion() < PreferenceFactory.VERSION) {
            transfer(old, prefs);
            old.setVersion(PreferenceFactory.VERSION);
            prefs.setVersion(PreferenceFactory.VERSION);
        }
    }

    /**
     * 仅更新算法
     */
    public static void upgrade(SecureSharedPreferences2 old, SecureSharedPreferences2 prefs) {
        if (old.getVersion() < PreferenceFactory.VERSION) {
            transfer(old, prefs);
            prefs.setVersion(PreferenceFactory.VERSION);
            old.setVersion(PreferenceFactory.VERSION);
        }
    }

    public static void transfer(SharedPreferences old, SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        Iterator iter = old.getAll().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            Object value = entry.getValue();

            put(editor, key, value);
        }
        editor.apply();
    }

    private static void put(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Set) {
            editor.putStringSet(key, (Set) value);
        } else {
            //TODO ggg error
        }
    }
}
