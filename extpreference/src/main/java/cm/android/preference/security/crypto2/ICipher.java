package cm.android.preference.security.crypto2;

import cm.android.preference.security.crypto.CryptoException;

public interface ICipher {

    void initKey(byte[] key, byte[] iv);

    byte[] encrypt(byte[] bytes) throws CryptoException;

    byte[] decrypt(byte[] bytes) throws CryptoException;
}
