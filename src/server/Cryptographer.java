package server;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Random;

public class Cryptographer {
    private Cipher encryptCipher;
    private Cipher decryptCipher;

    public Cryptographer(String algorithm) {
        try {
            encryptCipher = Cipher.getInstance(algorithm);
            decryptCipher = Cipher.getInstance(algorithm);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.out.println("Ошибка создания Cryptographer: " + e);
        }
    }

    public String encryptString(String messageToEncrypt, PublicKey clientPublicKey) {

        try {
            encryptCipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);

            byte[] messageToEncryptBytes = messageToEncrypt.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedMessage = encryptCipher.doFinal(messageToEncryptBytes);

            return Base64.getEncoder().encodeToString(encryptedMessage);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            System.out.println("Ошибка шифровки сообщения:" + e);
        }
        return null;
    }

    public String decryptString(String messageToDecrypt, PrivateKey privateKey) {

        try {
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] messageToDecryptBytes = Base64.getDecoder().decode(messageToDecrypt);
            byte[] decryptedMessage = decryptCipher.doFinal(messageToDecryptBytes);

            return new String(decryptedMessage, StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            System.out.println("Ошибка расшифровки сообщения:" + e);
        }
        return null;
    }

    public String generateSecretCode(int length) {
        String characters = "qwertyuiopasdfghjklzxcvbnm1234567890QWERTYUIOASDFGHJKLZXCVBNM";
        Random rnd = new Random();
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = characters.charAt(rnd.nextInt(characters.length()));
        }
        return new String(text);
    }

}
