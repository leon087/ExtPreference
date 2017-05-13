package cm.android.preference.security;

import android.content.Context;
import android.content.SharedPreferences;

import cm.android.preference.security.crypto.Cipher;
import cm.android.preference.security.crypto.ICipher;
import cm.android.preference.security.util.Util;

public final class PreferenceFactory {

    public static final int VERSION = 2;

    private PreferenceFactory() {
    }

//    public static SecureSharedPreferences getPreferences(SharedPreferences original, int version,
//            ICipher keyCipher, ICipher valueCipher) {
//        SecureSharedPreferences sharedPreferences;
//        if (original instanceof SecureSharedPreferences) {
//            sharedPreferences = (SecureSharedPreferences) original;
//        } else {
//            sharedPreferences = new SecureSharedPreferences(original, keyCipher, valueCipher);
//        }
//
//        int oldVersion = Util.getVersion(sharedPreferences);
//        if (oldVersion < version) {
//            LOGGER.info("oldVersion = {},version = {}", version, version);
////            Util.upgrade(sharedPreferences, version);
//        }
//        return sharedPreferences;
//    }

    @Deprecated
    public static SecureSharedPreferences getPreferences(Context context, String preferencesName, String password) {
        String tag = preferencesName + password;
        ICipher valueCipher = Cipher.KeyHelper.initCipher(context, tag);

        SharedPreferences original = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        return getPreferences(context, tag, VERSION, valueCipher, original);
    }

    @Deprecated
    public static SecureSharedPreferences getPreferences(Context context, String tag, int version, ICipher valueCipher, SharedPreferences original) {
//        Util.checkVersion(original, version);

        ICipher keyCipher = Cipher.KeyHelper.initKeyCipher(context, tag, valueCipher, original);
        SecureSharedPreferences securePreferences = new SecureSharedPreferences(original, keyCipher,
                valueCipher);

        return securePreferences;
    }

    public static SecureSharedPreferences2 getPreferences(Context context, String preferencesName, String tag, String password) {
        SharedPreferences original = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        cm.android.preference.security.crypto2.ICipher cipher = cm.android.preference.security.crypto2.Cipher.DefHelper.initCipher(context, tag, password, original);
        SecureSharedPreferences2 securePreferences = getPreferences(VERSION, cipher, original);

        return securePreferences;
    }

    public static SecureSharedPreferences2 getPreferences(int version, cm.android.preference.security.crypto2.ICipher cipher, SharedPreferences original) {
//        Util.checkVersion(original, version);
        SecureSharedPreferences2 securePreferences = new SecureSharedPreferences2(original, cipher);
        return securePreferences;
    }
}
