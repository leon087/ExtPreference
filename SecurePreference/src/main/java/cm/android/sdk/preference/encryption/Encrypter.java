package cm.android.sdk.preference.encryption;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import cm.android.sdk.preference.util.SecureUtil;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 */
public class Encrypter implements IEncrypt {
    // private static final String AES_KEY_ALG = "AES/GCM/NoPadding";
    // private static final String AES_KEY_ALG = "AES/CBC/PKCS5Padding";
    private static final String AES_KEY_ALG = "AES";

    // change to SC if using Spongycastle crypto libraries
    public static final String PROVIDER = "BC";

    private byte[] key;

    public Encrypter() {
    }

    @Override
    public void initKey(byte[] key) {
        this.key = key;
    }

    @Override
    public byte[] encrypt(byte[] bytes) throws EncryptionException {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        try {
            final Cipher cipher = Cipher.getInstance(AES_KEY_ALG, PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(
                    key, AES_KEY_ALG));
            return cipher.doFinal(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public byte[] decrypt(byte[] bytes) throws EncryptionException {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        try {
            final Cipher cipher = Cipher.getInstance(AES_KEY_ALG, PROVIDER);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(
                    key, AES_KEY_ALG));
            return cipher.doFinal(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    public static class KeyHelper {

        private static final int KEY_SIZE = 256;

        private static final String PRIMARY_PBE_KEY_ALG = "PBKDF2WithHmacSHA1";
        private static final String BACKUP_PBE_KEY_ALG = "PBEWithMD5AndDES";
        private static final int ITERATIONS = 2000;

        public static byte[] initKey(Context context, SharedPreferences preference) {
            // Initialize encryption/decryption key
            try {
                final String key = generateAesKeyName(context);
                String value = preference.getString(key, null);
                if (value == null) {
                    value = generateAesKeyValue();
                    preference.edit().putString(key, value).commit();
                }
                return SecureUtil.decode(value);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private static String generateAesKeyName(Context context)
                throws InvalidKeySpecException, NoSuchAlgorithmException,
                NoSuchProviderException {
            final char[] password = context.getPackageName().toCharArray();

            final byte[] salt = getDeviceSerialNumber(context).getBytes();

            SecretKey key;
            try {
                // TODO: what if there's an OS upgrade and now supports the primary
                // PBE
                key = generatePBEKey(password, salt,
                        PRIMARY_PBE_KEY_ALG, ITERATIONS, KEY_SIZE);
            } catch (NoSuchAlgorithmException e) {
                // older devices may not support the have the implementation try
                // with a weaker
                // algorthm
                key = generatePBEKey(password, salt,
                        BACKUP_PBE_KEY_ALG, ITERATIONS, KEY_SIZE);
            }
            return SecureUtil.encode(key.getEncoded());
        }

        /**
         * Derive a secure key based on the passphraseOrPin
         *
         * @param passphraseOrPin
         * @param salt
         * @param algorthm        - which PBE algorthm to use. some <4.0 devices don;t support
         *                        the prefered PBKDF2WithHmacSHA1
         * @param iterations      - Number of PBKDF2 hardening rounds to use. Larger values
         *                        increase computation time (a good thing), defaults to 1000 if
         *                        not set.
         * @param keyLength
         * @return Derived Secretkey
         * @throws java.security.NoSuchAlgorithmException
         * @throws java.security.spec.InvalidKeySpecException
         * @throws java.security.NoSuchProviderException
         */
        private static SecretKey generatePBEKey(char[] passphraseOrPin,
                                                byte[] salt, String algorthm, int iterations, int keyLength)
                throws NoSuchAlgorithmException, InvalidKeySpecException,
                NoSuchProviderException {

            if (iterations == 0) {
                iterations = 1000;
            }

            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(
                    algorthm, Encrypter.PROVIDER);
            KeySpec keySpec = new PBEKeySpec(passphraseOrPin, salt, iterations,
                    keyLength);
            SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
            return secretKey;
        }

        /**
         * Gets the hardware serial number of this device.
         *
         * @return serial number or Settings.Secure.ANDROID_ID if not available.
         */
        @TargetApi(3)
        private static String getDeviceSerialNumber(Context context) {
            // We're using the Reflection API because Build.SERIAL is only available
            // since API Level 9 (Gingerbread, Android 2.3).
            try {
                String deviceSerial = (String) Build.class.getField("SERIAL").get(
                        null);
                if (TextUtils.isEmpty(deviceSerial)) {
                    deviceSerial = Settings.Secure.getString(
                            context.getContentResolver(),
                            Settings.Secure.ANDROID_ID);
                }
                return deviceSerial;
            } catch (Exception ignored) {
                // default to Android_ID
                return Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
            }
        }

        private static String generateAesKeyValue() throws NoSuchAlgorithmException {
            // Do *not* seed secureRandom! Automatically seeded from system entropy
            final SecureRandom random = new SecureRandom();

            // Use the largest AES key length which is supported by the OS
            final KeyGenerator generator = KeyGenerator.getInstance("AES");
            try {
                generator.init(KEY_SIZE, random);
            } catch (Exception e) {
                try {
                    generator.init(192, random);
                } catch (Exception e1) {
                    generator.init(128, random);
                }
            }
            return SecureUtil.encode(generator.generateKey().getEncoded());
        }
    }
}
