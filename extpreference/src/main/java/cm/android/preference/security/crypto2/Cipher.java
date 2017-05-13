package cm.android.preference.security.crypto2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Key;
import java.util.Properties;

import cm.android.preference.security.crypto.CryptoException;
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
    public void initKey(byte[] key, byte[] iv) {
        if (key != null) {
            this.key = key.clone();
        }
        if (iv != null) {
            this.iv = iv.clone();
        }
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
            logger.info("writeState:state = " + iv);

            File file = new File(context.getCacheDir(), FILE_NAME_CACHE);
            Properties properties = Util.loadProperties(file);
            properties.setProperty(ivName, iv);

            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                properties.store(os, "write:ivName = " + ivName);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
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
    public static class DefHelper {

        /**
         * initCipher
         */
        public static ICipher initCipher(Context context, String tag, String password, SharedPreferences original) {
            byte[] key = generateKey(context, tag, password);

            try {
                ICipher ivCipher = new Cipher();
                ivCipher.initKey(key, null);
                String ivName = Util.encodeBase64(tag.getBytes());
                byte[] iv = initIv(context, ivCipher, ivName, original);

                ICipher valueCipher = new Cipher();
                valueCipher.initKey(key, iv);
                return valueCipher;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private static byte[] initIv(Context context, ICipher ivCipher, String ivName, SharedPreferences original) throws CryptoException {
            String value = IvHolder.readIv(context, original, ivName);

            byte[] iv;
            if (value == null) {
                iv = SecureUtil.generateIv();
                byte[] encryptData = ivCipher.encrypt(iv);
                value = Util.encodeBase64(encryptData);
                IvHolder.writeIv(context, original, ivName, value);
            } else {
                byte[] encryptData = Util.decodeBase64(value);
                iv = ivCipher.decrypt(encryptData);
            }

            return iv;
        }

        private static byte[] generateKey(Context context, String tag, String pwd) {
            final String password = getPassword(context, tag, pwd);

            try {
                Key key = AESCoder.generateKey(password.toCharArray(), SecureUtil.getSaltDef(), 16);
                return key.getEncoded();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private static String getPassword(Context context, String tag, String pwd) {
            String password = new StringBuilder()
                    .append(context.getPackageName())
                    .append(tag)
                    .append(pwd)
                    .toString();
            return password;
        }
    }
}
