package com.streamreduce.util;

import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.shiro.codec.Base64;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.util.ByteSource;
import org.apache.shiro.util.SimpleByteSource;
import org.springframework.util.StringUtils;

public final class SecurityUtil {

    public static String passwordEncrypt(String password) {
        return new Sha256Hash(password).toBase64();
    }

    public static boolean isValidPassword(String password) {
        return !(password.isEmpty() || password.length() < 6 || password.length() >= 20);
    }

    public static String createNodeableFUID(String str, Long created) {
        str = StringUtils.trimAllWhitespace(str);
        str = str.toLowerCase();
        // remove all non alphanumeric
        str = str.replaceAll("[^a-zA-Z0-9]", "");
        str = str + "_" + created;
        return str;
    }


    /*
    * This method generates a random n digit string, which contains at least one number, lower case alphabet, upper
    * case alphabet and as special character.
    */
    public static String generateRandomString(int n) {

        Random rd = new Random();

        char lowerChars[] = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        char upperChars[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        char numbers[] = "0123456789".toCharArray();
        //char specialChars[] = "~!@#$%^&*()-_=+[{]}|;:<>/?".toCharArray();
        char specialChars[] = "!$-_+".toCharArray(); // limited subset

        List<Character> pwdLst = new ArrayList<Character>();
        for (int g = 0; g <= n; g++) {
            for (int z = 0; z < 1; z++) {
                if (g == 0) {
                    pwdLst.add(numbers[rd.nextInt(10)]); // must match char array length(s) above
                } else if (g == 1) {
                    pwdLst.add(lowerChars[rd.nextInt(26)]);
                } else if (g == 2) {
                    pwdLst.add(upperChars[rd.nextInt(26)]);
                } else if (g == 3) {
                    pwdLst.add(specialChars[rd.nextInt(5)]);
                }
            }
            if (pwdLst.size() == n) {
                break;
            }
            if (g + 1 == 4) {
                g = (int) (Math.random() * 5);

            }
        }
        StringBuilder password = new StringBuilder();
        Collections.shuffle(pwdLst);
        for (Character aPwdLst : pwdLst) {
            password.append(aPwdLst);
        }
        return password.toString();
    }

    /**
     * Generate a random string, default is 10 characters in length
     *
     * @return
     */
    public static String generateRandomString() {
        return generateRandomString(10);
    }

    /**
     * Returns encrypted password using given key
     *
     * @param password password to encrypt in plain text
     * @param key      the encryption key
     * @return encrypted password in base64 encoding
     */
    public static String encryptPassword(String password, byte[] key) {
        AesCipherService cipherService = new AesCipherService();
        ByteSource encrypted = cipherService.encrypt(password.getBytes(), key);
        return encrypted.toBase64();
    }

    /**
     * Returns decrypted password using given key
     *
     * @param password the password to decrypt in base64 encoding
     * @param key      the key used to encrypt the password
     * @return decrypted password in plain text
     */
    public static String decryptPassword(String password, byte[] key) {
        AesCipherService cipherService = new AesCipherService();
        ByteSource decrypted = cipherService.decrypt(Base64.decode(password), key);
        return new String(decrypted.getBytes());
    }

    /**
     * Returns a new encryption key
     *
     * @return encryption key in base64 encoding
     */
    public static String generateNewKey() {
        AesCipherService cipherService = new AesCipherService();
        Key newKey = cipherService.generateNewKey();
        return new SimpleByteSource(newKey.getEncoded()).toBase64();
    }

    public static String issueRandomAPIToken() {
        // we need to see our tokens with a random value so the same one isn't generated
        // for the same user each time.
        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        Object randomNumber = rng.nextBytes();

        // we also use a user agent as a validation factor
        // so when we later validate the token, we also validate the user agent
        String secret = generateRandomString();
        String salt = secret.concat(randomNumber.toString());
        return new Sha256Hash(secret, salt, 1024).toBase64();
    }

}
