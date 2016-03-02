import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * Provides encrypting and decrypting
 */
public class Security {

    public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
    private static final String UNICODE_FORMAT = "UTF8";
    byte[] arrayBytes;
    SecretKey key;
    private KeySpec ks;
    private SecretKeyFactory skf;
    private Cipher cipher;
    private String encryptionKey;
    private String encryptionScheme;

    // Need to handle exceptions properly!!!

    public Security(String encryptionKey) throws Exception {
        this.encryptionKey = encryptionKey;
        encryptionScheme = DESEDE_ENCRYPTION_SCHEME;
        arrayBytes = encryptionKey.getBytes(UNICODE_FORMAT);
        ks = new DESedeKeySpec(arrayBytes);
        skf = SecretKeyFactory.getInstance(encryptionScheme);
        cipher = Cipher.getInstance(encryptionScheme);
        key = skf.generateSecret(ks);
    }

    public String encrypt(String unencryptedString) {
        String encryptedString = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] plainText = unencryptedString.getBytes(UNICODE_FORMAT);
            byte[] encryptedText = cipher.doFinal(plainText);
            encryptedString = new String(Base64.encodeBase64(encryptedText));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedString;
    }

    public String decrypt(String encryptedString) {
        String decryptedText = null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encryptedText = Base64.decodeBase64(encryptedString);
            byte[] plainText = cipher.doFinal(encryptedText);
            decryptedText = new String(plainText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedText;
    }
}
