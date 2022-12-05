package server;

import com.didisoft.pgp.*;
import com.didisoft.pgp.exceptions.NoPrivateKeyFoundException;

import java.io.*;
import java.util.Random;

/**
 * Класс позволяющий работать с криптографией.
 *
 * @author Kirill Chezlov
 * @version 1.0
 */
public class PGP {
    PGPLib pgpLib;
    String username;
    String defaultKeysFilepath = "src/server/res/";
    private int keySizeInBytes = 2048;
    private long expiresAfterDay = 5;
    private String hashingAlgorithm = HashAlgorithm.MD5;
    private String compression = CompressionAlgorithm.UNCOMPRESSED;

    public PGP(String username) {
        this.pgpLib = new PGPLib();
        this.generateKeyPair(username);
    }

    /**
     * позволяет генерировать пару ключей для асимметричной RSA криптографии
     * и сохранить их в файлы
     * @param username имя пользователя-владельца ключей
     */
    public void generateKeyPair(String username) {
        try {
            PGPKeyPair keyPair = PGPKeyPair.generateRsaKeyPair(keySizeInBytes, username, username);
            exportKeysFromKeyPair(keyPair);
        } catch (PGPException e) {
            System.err.println("Ошибка генерации ключей: " + e);
        }
    }

    /**
     * сохраняет пару ключей в файл
     * @param keyPair пара RSA ключей
     */
    private void exportKeysFromKeyPair(PGPKeyPair keyPair) {
        try {
            keyPair.exportPublicKey(defaultKeysFilepath + "PublicKey_" + keyPair.getUserID() + ".pgp", true);
            keyPair.exportPrivateKey(defaultKeysFilepath + "PrivateKey_" + keyPair.getUserID() + ".pgp", true);
        } catch (NoPrivateKeyFoundException | IOException e) {
            System.err.println("Ошибка записи файлов ключей: "+ e);
        }
    }

    /**
     * возвращает зашифрованную строку
     * @param stringToEncrypt сообщение для шифорования
     * @param username имя пользователя, шифрующего сообщение
     * @return String в случае успешного шифрования, иначе null
     */
    public String encryptString(String stringToEncrypt, String username) {
        try {
            FileInputStream publicEncryptionKeyFile = new FileInputStream(this.getPublicKeyFilepath(username));
            return pgpLib.encryptString(stringToEncrypt, publicEncryptionKeyFile);
        } catch (PGPException | IOException e) {
            System.err.println("Ошибка шифрования строки: " + e);
        }
        return null;
    }

    /**
     * возвращает расшифровыванную строку
     * @param stringToEncrypt сообщение для расшифровки
     * @param username имя пользователя, расшифрующего сообщение
     * @return String в случае успешной расшифровки, иначе null
     */
    public String decryptString(String stringToEncrypt, String username) {
        try {
            FileInputStream publicDecryptionKeyFile = new FileInputStream(this.getPrivateKeyFilepath(username));
            return pgpLib.decryptString(stringToEncrypt, publicDecryptionKeyFile, username);
        } catch (PGPException | IOException e) {
            System.err.println("Ошибка расшифровки строки: " + e);
        }
        return null;
    }

    /**
     * создает и возвращает путь к файлу публичного ключа
     * @param username имя пользователя-владельца ключей
     * @return String
     */
    public String getPublicKeyFilepath(String username) {
        return defaultKeysFilepath + "PublicKey_" + username + ".pgp";
    }

    /**
     * создает и возвращает путь к файлу приватного ключа
     * @param username имя пользователя-владельца ключей
     * @return String
     */
    public String getPrivateKeyFilepath(String username) {
        return defaultKeysFilepath + "PrivateKey_" + username + ".pgp";
    }

    /**
     * генерирует случайным образом и возвращает секретный ключ
     * @param length длина серетного кода
     * @return String
     */
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
