package cm.android.preference.security.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Base64;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;

import cm.android.preference.security.PreferenceFactory;

import static cm.android.preference.util.Util.closeQuietly;

public final class Util {

    public static final String VERSION_KEY = "SecureSharedPreferences_version";

    private Util() {
    }

    public static Logger getLogger() {
        return LoggerFactory.getLogger("preference");
    }

//    @SuppressWarnings("unchecked")
//    @TargetApi(11)
//    public static void migrateData(SharedPreferences from, SharedPreferences to, int version) {
//        Map<String, ?> all = from.getAll();
//        Set<String> keySet = all.keySet();
//        Editor edit = to.edit();
//        for (String key : keySet) {
//            Object object = all.get(key);
//            if (object == null) {
//                // should not reach here
//                edit.remove(key);
//            } else if (object instanceof String) {
//                edit.putString(key, (String) object);
//            } else if (object instanceof Integer) {
//                edit.putInt(key, (Integer) object);
//            } else if (object instanceof Long) {
//                edit.putLong(key, (Long) object);
//            } else if (object instanceof Float) {
//                edit.putFloat(key, (Float) object);
//            } else if (object instanceof Boolean) {
//                edit.putBoolean(key, (Boolean) object);
//            } else if (object instanceof Set<?>) {
//                edit.putStringSet(key, (Set<String>) object);
//            }
//        }
//        edit.putInt(VERSION_KEY, version);
//        SecureSharedPreferences.SecureEditor.compatilitySave(edit);
//    }

    public static void checkVersion(SharedPreferences original, int version) {
        int oldVersion = original.getInt(VERSION_KEY, -1);
        if (oldVersion < version) {
            Util.getLogger().info("oldVersion = {},version = {}", oldVersion, version);
//            original.edit().clear().apply();
            original.edit().putInt(VERSION_KEY, version).apply();
        }
    }

    public static boolean checkUpgrade(SharedPreferences original) {
        int oldVersion = original.getInt(VERSION_KEY, -1);
        Util.getLogger().info("oldVersion = {},version = {}", oldVersion, PreferenceFactory.VERSION);
        if (oldVersion < PreferenceFactory.VERSION) {
            return true;
        }

        return false;
    }

    @TargetApi(8)
    public static String encodeBase64(byte[] input) {
        return Base64.encodeToString(input, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
    }

    @TargetApi(8)
    public static byte[] decodeBase64(String input) {
        return Base64.decode(input, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
    }

    public static byte[] getFingerprint(Context context, String tag) {
        return getFingerprint(context, tag, context.getPackageName());
    }

    public static byte[] getFingerprint(Context context, String tag, String packageName) {
        StringBuilder sb = new StringBuilder();
        sb.append(tag);
        sb.append(packageName);

        android.content.pm.Signature[] signatures = getSignature(context.getPackageManager(),
                packageName);
        if (signatures != null) {
            sb.append(signatures[0].toCharsString());
        }

        byte[] fingerprint = HashUtil.getHmac(tag.getBytes(Charset.defaultCharset()), sb.toString().getBytes(Charset.defaultCharset()));
        return fingerprint;
    }

    public static android.content.pm.Signature[] getSignature(
            PackageManager pm, String packageName) {
        try {
            android.content.pm.Signature[] sigs = pm.getPackageInfo(
                    packageName, PackageManager.GET_SIGNATURES).signatures;
            return sigs;
        } catch (PackageManager.NameNotFoundException e) {
            Util.getLogger().error(e.getMessage(), e);
            return null;
        }
    }

    public static final Properties loadProperties(File file) {
        Properties properties = new Properties();
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            properties.load(in);
        } catch (IOException e) {
            Util.getLogger().error(e.getMessage(), e);
        } finally {
            cm.android.preference.util.Util.closeQuietly(in);
        }
        return properties;
    }

}
