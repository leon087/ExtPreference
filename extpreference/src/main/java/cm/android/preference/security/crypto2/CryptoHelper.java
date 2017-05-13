package cm.android.preference.security.crypto2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.SharedPreferences;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cm.android.preference.security.crypto.CryptoException;
import cm.android.preference.security.util.Util;

/**
 * CryptoHelper
 */
public class CryptoHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("ExtPreference");

    private ICipher valueCipher;

    public CryptoHelper(ICipher valueCipher) {
        this.valueCipher = valueCipher;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(SharedPreferences prefs, String key, T defValue) {
        String keyEncrypt = encryptKey(key);
        String stringValue = prefs.getString(keyEncrypt, null);
        if (stringValue == null) {
            return defValue;
        }

        try {
            T result = readDecoded(stringValue);
            return result;
        } catch (CryptoException e) {
            LOGGER.error("Error reading value by key: {}", key, e);
            return defValue;
        }
    }

    public <T> void putValue(SharedPreferences.Editor editor, String key, T value) {
        String keyEncrypt = encryptKey(key);

        String valueEncrypt = encode(value);
        editor.putString(keyEncrypt, valueEncrypt);
    }

    public void remove(SharedPreferences.Editor editor, String key) {
        String keyEncrypt = encryptKey(key);
        editor.remove(keyEncrypt);
    }

    public Map<String, ?> getAll(SharedPreferences prefs) {
        Map<String, ?> tmp = prefs.getAll();
        Map<String, Object> decryptedMap = new HashMap<>(tmp.size());

        Iterator iterator = tmp.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ?> entry = (Map.Entry<String, ?>) iterator.next();
            try {
                String key = decryptKey(entry.getKey());
                if (key == null) {
                    continue;
                }

                Object value = readDecoded((String) entry.getValue());
                decryptedMap.put(key, value);
            } catch (CryptoException e) {
                continue;
            }
        }

        return decryptedMap;
    }

    private <T> String encode(T value) {
        String result = null;
        if (value != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(value);
                byte[] byteArray = baos.toByteArray();
                result = encrypt(byteArray);
            } catch (IOException e) {
                LOGGER.error("Error encoding value", e);
            }
        }
        return result;
    }

    private <T> String encrypt(byte[] byteArray) {
        try {
            byte[] encrypt = valueCipher.encrypt(byteArray);
            String result = Util.encodeBase64(encrypt);
            return result;
        } catch (CryptoException e) {
            LOGGER.error("Error encoding value", e);
            return new String(byteArray, Charset.defaultCharset());
        }
    }

    private byte[] decrypt(String stringValue) throws CryptoException {
        byte[] decodedBytes = Util.decodeBase64(stringValue);
        byte[] decoded = valueCipher.decrypt(decodedBytes);
        return decoded;
    }

    private <T> T readDecoded(String stringValue) throws CryptoException {
        ObjectInputStream ois = null;
        try {
            byte[] decoded = decrypt(stringValue);
            ois = new ObjectInputStream(new ByteArrayInputStream(decoded));
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new CryptoException(e);
        } finally {
            cm.android.preference.util.Util.closeQuietly(ois);
        }
    }

    public boolean contains(SharedPreferences preference, String key) {
        String keyEncrypt = encryptKey(key);
        return preference.contains(keyEncrypt);
    }

    public String encryptKey(String key) {
        return key;
    }

    public String decryptKey(String stringValue) {
        return stringValue;
    }
}
