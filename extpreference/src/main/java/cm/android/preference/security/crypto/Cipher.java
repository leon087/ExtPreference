package cm.android.preference.security.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.Key;
import java.util.Properties;

import cm.android.preference.security.util.AESCoder;
import cm.android.preference.security.util.SecureUtil;
import cm.android.preference.security.util.Util;

/**
 * Cipher
 */
public class Cipher implements ICipher {

    private static final Logger logger = LoggerFactory.getLogger("ExtPreference");

    private byte[] key;

    private byte[] iv;

    public Cipher() {
    }

    @Override
    public void initKey(byte[] key, byte[] iv, String tag) {
//        this.key = Arrays.copyOf(key, key.length);
//        this.iv = Arrays.copyOf(iv, iv.length);
        this.key = key.clone();
        this.iv = iv.clone();
    }

    @Override
    public byte[] encrypt(byte[] bytes) throws CryptoException {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        try {
            return AESCoder.encrypt(key, iv, bytes);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public byte[] decrypt(byte[] bytes) throws CryptoException {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        try {
            return AESCoder.decrypt(key, iv, bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static class IvHolder {

        private static final String FILE_NAME_CACHE = "SecurePreference_cache";

        private static void write(Context context, String ivName, String iv) {
            logger.info("write:ivName = " + ivName);

            File file = new File(context.getCacheDir(), FILE_NAME_CACHE);
            Properties properties = Util.loadProperties(file);
            properties.setProperty(ivName, iv);

            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                properties.store(os, "write:ivName = " + ivName);
            } catch (IOException e) {
                logger.debug(e.getMessage(), e);
            } finally {
                cm.android.preference.util.Util.closeQuietly(os);
            }
        }

        private static String read(Context context, String ivName) {
            File file = new File(context.getCacheDir(), FILE_NAME_CACHE);
            Properties properties = Util.loadProperties(file);
            String iv = properties.getProperty(ivName, null);

            logger.info("read:ivName = " + ivName);
            return iv;
        }

        public static void clear(Context context) {
            File file = new File(context.getCacheDir(), FILE_NAME_CACHE);
            boolean delete = file.delete();
            if (!delete) {
                logger.error("delete = false:file = {}", file.getAbsolutePath());
            }
        }

        public static void writeIv(Context context, SharedPreferences original, String ivName, String value) {
            original.edit().putString(ivName, value).apply();
            write(context, ivName, value);
        }

        public static String readIv(Context context, SharedPreferences original, String ivName) {
            String value = original.getString(ivName, null);
            if (!TextUtils.isEmpty(value)) {
                clear(context);
                return value;
            }

            String data = read(context, ivName);
            return data;
        }
    }

    /**
     * KeyHelper
     */
    public static class KeyHelper {

        /**
         * initKeyCipher
         */
        public static ICipher initKeyCipher(Context context, String tag, ICipher valueCipher, SharedPreferences original) {
            byte[] key = generateKey(context, tag);
            ICipher keyCipher = new Cipher();
            keyCipher.initKey(key, SecureUtil.getIvDef(), tag);

            try {
                byte[] keyValue = keyCipher.encrypt(tag.getBytes(Charset.defaultCharset()));
                String ivName = Util.encodeBase64(keyValue);
                byte[] iv = initIv(context, ivName, valueCipher, original);

                keyCipher.initKey(key, iv, tag);
                return keyCipher;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private static byte[] initIv(Context context, String ivName, ICipher valueCipher,
                                     SharedPreferences original) throws CryptoException {
            String value = IvHolder.readIv(context, original, ivName);

            byte[] iv;
            if (value == null) {
                iv = SecureUtil.generateIv();
                byte[] encryptData = valueCipher.encrypt(iv);
                value = Util.encodeBase64(encryptData);
                IvHolder.writeIv(context, original, ivName, value);
            } else {
                byte[] encryptData = Util.decodeBase64(value);
                iv = valueCipher.decrypt(encryptData);
            }

            return iv;
        }

        /**
         * initCipher
         */
        public static ICipher initCipher(Context context, String tag) {
            ICipher cipher = new Cipher();
            byte[] key = Cipher.KeyHelper.generateKey(context, tag);
            byte[] iv = SecureUtil.getIvDef();
            cipher.initKey(key, iv, tag);

            return cipher;
        }

        private static byte[] generateKey(Context context, String tag) {
            final String password = getPassword(context, tag);

            try {
                Key key = AESCoder.generateKey(password.toCharArray(), SecureUtil.getSaltDef(), 16);
                return key.getEncoded();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private static String getPassword(Context context, String tag) {
            String deviceId = getDeviceSerialNumber(context);
            byte[] fingerprint = Util.getFingerprint(context, tag + deviceId);
            return Util.encodeBase64(fingerprint);
        }

//        public static byte[] initKey(Context context, String tag, SharedPreferences preference) {
//            final String password = getPassword(context, tag);
//
//            try {
//                Key key = AESCoder.generateKey(password.toCharArray(), null, 16);
//                return key.getEncoded();
//            } catch (Exception e) {
//                throw new IllegalStateException(e);
//            }
//        }

        @TargetApi(3)
        private static String getDeviceSerialNumber(Context context) {
            String deviceSerial = "";
            try {
                deviceSerial = (String) Build.class.getField("SERIAL").get(null);
            } catch (NoSuchFieldException e) {
                logger.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                logger.error(e.getMessage(), e);
            }
            if (TextUtils.isEmpty(deviceSerial)) {
                deviceSerial = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
            return deviceSerial;
        }
    }
}
