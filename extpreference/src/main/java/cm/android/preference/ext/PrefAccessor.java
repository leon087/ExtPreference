package cm.android.preference.ext;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cm.android.preference.security.util.Util;

class PrefAccessor {
//    private static PrefsConfig CONFIG = null;
//
//    public static void init(PrefsConfig config) {
//        CONFIG = config;
//    }

    private static int getColumnIndex(Cursor cursor) {
        return cursor.getColumnIndex(PreferenceProvider.COLUMNS_VALUE);
    }

    public static boolean contains(Context context, String name, String key) {
        Uri URI = PreferenceProvider.buildUri(PreferenceProvider.config().getAuthority(), name, key, PreferenceProvider.PREF_EXIST);
        Cursor cursor = context.getContentResolver().query(URI, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(getColumnIndex(cursor)) == 1;
            } else {
                return false;
            }
        } finally {
            cm.android.preference.util.Util.closeQuietly(cursor);
        }
    }

    public static String getString(Context context, String name, String key, String defaultValue) {
        Uri URI = PreferenceProvider.buildUri(PreferenceProvider.config().getAuthority(), name, key, PreferenceProvider.PREF_STRING);
        Cursor cursor = context.getContentResolver().query(URI, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(getColumnIndex(cursor));
            } else {
                return defaultValue;
            }
        } finally {
            cm.android.preference.util.Util.closeQuietly(cursor);
        }
    }

    private static File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException(
                "File " + name + " contains a path separator");
    }

    public static Map<String, ?> getAll(Context context, String name) {
        Uri URI = PreferenceProvider.buildUri(PreferenceProvider.config().getAuthority(), name, "", PreferenceProvider.PREF_ALL);
        Cursor cursor = context.getContentResolver().query(URI, null, null, null, null);
        try {
            Map<String, Object> map = new HashMap<>();
            if (cursor == null || !cursor.moveToFirst()) {
                return map;
            }

            do {
                String key = cursor.getString(cursor.getColumnIndex(PreferenceProvider.COLUMNS_KEY));
                int type = cursor.getInt(cursor.getColumnIndex(PreferenceProvider.COLUMNS_TYPE));

                map.put(key, getValue(cursor, type));

            } while (cursor.moveToNext());

            return map;
        } finally {
            cm.android.preference.util.Util.closeQuietly(cursor);
        }
    }

    private static Object getValue(Cursor cursor, int type) {
        switch (type) {
            case PreferenceProvider.PREF_INT:
                return cursor.getInt(getColumnIndex(cursor));
            case PreferenceProvider.PREF_LONG:
                return cursor.getLong(getColumnIndex(cursor));
            case PreferenceProvider.PREF_FLOAT:
                return cursor.getFloat(getColumnIndex(cursor));
            case PreferenceProvider.PREF_STRING:
                return cursor.getString(getColumnIndex(cursor));
            case PreferenceProvider.PREF_SET:
                byte[] bs = cursor.getBlob(getColumnIndex(cursor));
                return PrefsCursor.Util.getSet(bs);
            case PreferenceProvider.PREF_BOOLEAN:
                int i = cursor.getInt(getColumnIndex(cursor));
                return PrefsCursor.Util.getBoolean(i);
            default:
                return cursor.getString(getColumnIndex(cursor));
        }
    }

    public static int getInt(Context context, String name, String key, int defaultValue) {
        Uri URI = PreferenceProvider.buildUri(PreferenceProvider.config().getAuthority(), name, key, PreferenceProvider.PREF_INT);
        Cursor cursor = context.getContentResolver().query(URI, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(getColumnIndex(cursor));
            } else {
                return defaultValue;
            }
        } finally {
            cm.android.preference.util.Util.closeQuietly(cursor);
        }
    }

    public static long getLong(Context context, String name, String key, long defaultValue) {
        Uri URI = PreferenceProvider.buildUri(PreferenceProvider.config().getAuthority(), name, key, PreferenceProvider.PREF_LONG);
        Cursor cursor = context.getContentResolver().query(URI, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(getColumnIndex(cursor));
            } else {
                return defaultValue;
            }
        } finally {
            cm.android.preference.util.Util.closeQuietly(cursor);
        }
    }

    public static boolean getBoolean(Context context, String name, String key, boolean defaultValue) {
        Uri URI = PreferenceProvider.buildUri(PreferenceProvider.config().getAuthority(), name, key, PreferenceProvider.PREF_BOOLEAN);
        Cursor cursor = context.getContentResolver().query(URI, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int value = cursor.getInt(getColumnIndex(cursor));
                return PrefsCursor.Util.getBoolean(value);
            } else {
                return defaultValue;
            }
        } finally {
            cm.android.preference.util.Util.closeQuietly(cursor);
        }
    }

    public static float getFloat(Context context, String name, String key, float defaultValue) {
        Uri URI = PreferenceProvider.buildUri(PreferenceProvider.config().getAuthority(), name, key, PreferenceProvider.PREF_FLOAT);
        Cursor cursor = context.getContentResolver().query(URI, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getFloat(getColumnIndex(cursor));
            } else {
                return defaultValue;
            }
        } finally {
            cm.android.preference.util.Util.closeQuietly(cursor);
        }
    }

    public static int apply(Context context, String name, ContentValues cv) {
        Uri URI = PreferenceProvider.buildUri(PreferenceProvider.config().getAuthority(), name, "", PreferenceProvider.PREF_APPLY);
        return context.getContentResolver().update(URI, cv, null, null);
    }

    public static void clear(ContentValues cv, String key) {
        String k = PreferenceProvider.getKey(PreferenceProvider.KEY_CLEAR, key);
        cv.put(k, "");
    }

    public static void remove(ContentValues cv, String key) {
        String k = PreferenceProvider.getKey(PreferenceProvider.KEY_REMOVE, key);
        cv.put(k, "");
    }

    public static void putBoolean(ContentValues cv, String key, Object value) {
        String k = PreferenceProvider.getKey(PreferenceProvider.KEY_PUT_BOOLEAN, key);
        cv.put(k, (Boolean) value);
    }

    public static void putString(ContentValues cv, String key, Object value) {
        String k = PreferenceProvider.getKey(PreferenceProvider.KEY_PUT_STRING, key);
        cv.put(k, (String) value);
    }

    public static void putLong(ContentValues cv, String key, Object value) {
        String k = PreferenceProvider.getKey(PreferenceProvider.KEY_PUT_LONG, key);
        cv.put(k, (Long) value);
    }

    public static void putInt(ContentValues cv, String key, Object value) {
        String k = PreferenceProvider.getKey(PreferenceProvider.KEY_PUT_INT, key);
        cv.put(k, (Integer) value);
    }

    public static void putFloat(ContentValues cv, String key, Object value) {
        String k = PreferenceProvider.getKey(PreferenceProvider.KEY_PUT_FLOAT, key);
        cv.put(k, (Float) value);
    }

    public static Set<String> getStringSet(Context context, String name, String key, Set<String> defaultValue) {
        Uri URI = PreferenceProvider.buildUri(PreferenceProvider.config().getAuthority(), name, key, PreferenceProvider.PREF_SET);
        Cursor cursor = context.getContentResolver().query(URI, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                byte[] bs = cursor.getBlob(getColumnIndex(cursor));
                return PrefsCursor.Util.getSet(bs);
            } else {
                return defaultValue;
            }
        } finally {
            cm.android.preference.util.Util.closeQuietly(cursor);
        }
    }

    public static void putStringSet(ContentValues cv, String key, Object value) {
        String k = PreferenceProvider.getKey(PreferenceProvider.KEY_PUT_SET, key);
        byte[] bs = PrefsCursor.Util.setToBlob((Set<String>) value);
        cv.put(k, bs);
    }

    public static ContentObserver registerContentObserver(final Context context, final String name, final SharedPreferences.OnSharedPreferenceChangeListener listener) {
        Uri uri = PreferenceProvider.buildUri(PreferenceProvider.config().getAuthority(), name, "", PreferenceProvider.PREF_CHANGE);
        ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                //解析URI，得到最后一位key
                List<String> pathSegments = uri.getPathSegments();
                String key = pathSegments.get(2);
                SharedPreferences sharedPreferences = PreferenceProvider.config().getFactory().getSharedPreferences(context, name);
                listener.onSharedPreferenceChanged(sharedPreferences, key);
            }
        };
        context.getContentResolver().registerContentObserver(uri, true, observer);
        return observer;
    }

    public static void unregisterContentObserver(Context context, ContentObserver observer) {
        if (observer != null) {
            context.getContentResolver().unregisterContentObserver(observer);
        }
    }

}