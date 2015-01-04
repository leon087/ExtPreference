package cm.android.preference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.SharedPreferences;

import cm.android.preference.encryption.Encrypter;
import cm.android.preference.encryption.IEncrypt;
import cm.android.preference.util.Util;

public final class PreferenceFactory {

    private static final String INITIALIZATION_ERROR = "Can not initialize SecureSharedPreferences";

    public static final int VERSION_1 = 1;

    public static final int LATEST_VERSION = VERSION_1;

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferenceFactory.class);

    private PreferenceFactory() {
    }

    public static SecureSharedPreferences getPreferences(SharedPreferences original,
            IEncrypt keyEncrypter, IEncrypt encryption) {
        SecureSharedPreferences sharedPreferences;
        if (original instanceof SecureSharedPreferences) {
            sharedPreferences = (SecureSharedPreferences) original;
        } else {
            sharedPreferences = new SecureSharedPreferences(original, keyEncrypter, encryption);
        }
        if (Util.getVersion(sharedPreferences) < VERSION_1) {
            LOGGER.info("Initial migration to Secure storage.");
            //Util.migrateData(original, sharedPreferences, VERSION_1);
        }
        return sharedPreferences;
    }

    public static SecureSharedPreferences getPreferences(Context context, String preferencesName,
            IEncrypt keyEncrypter, IEncrypt encryption) {
        SharedPreferences preference = context
                .getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        return getPreferences(preference, keyEncrypter, encryption);
    }

    public static SecureSharedPreferences getPreferences(Context context, String preferencesName) {
        SharedPreferences preference = context
                .getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        IEncrypt encryption = new Encrypter();
        byte[] key = Encrypter.KeyHelper.initKey(context, preferencesName, preference);
        byte[] iv = Encrypter.KeyHelper.initIv(context, preferencesName, preference);
        encryption.initKey(key, iv, preferencesName);

        return getPreferences(preference, encryption, encryption);
    }

}
