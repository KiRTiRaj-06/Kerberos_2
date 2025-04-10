package utils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.Base64;

public class CryptoUtils {

    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        return generator.generateKey();
    }

    public static String encrypt(String plaintext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] enc = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(enc);
    }

    public static String decrypt(String ciphertext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] dec = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(dec);
    }

    public static SecretKey loadOrCreateKey(String filePath) throws Exception {
        File file = new File(filePath);
        if (file.exists()) {
            byte[] encoded = Files.readAllBytes(file.toPath());
            return new SecretKeySpec(encoded, "AES");
        } else {
            SecretKey key = generateKey();
            Files.write(file.toPath(), key.getEncoded());
            return key;
        }
    }
}
