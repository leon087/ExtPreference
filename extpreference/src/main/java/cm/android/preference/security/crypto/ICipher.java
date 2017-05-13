package cm.android.preference.security.crypto;

public interface ICipher {

    void initKey(byte[] key, byte[] iv, String tag);

    byte[] encrypt(byte[] bytes) throws CryptoException;

    byte[] decrypt(byte[] bytes) throws CryptoException;
}
