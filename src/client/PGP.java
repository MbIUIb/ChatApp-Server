package client;

import com.didisoft.pgp.*;
import com.didisoft.pgp.exceptions.NoPrivateKeyFoundException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

public class PGP {
    PGPLib pgpLib;
    String username;
    String defaultKeysFilepath = "src/client/res/";
    private int keySizeInBytes = 2048;
    private long expiresAfterDay = 5;
    private String hashingAlgorithm = HashAlgorithm.MD5;
    private String compression = CompressionAlgorithm.UNCOMPRESSED;

    public PGP(String username) {
        this.pgpLib = new PGPLib();
        this.generateKeyPair(username);
    }

    public void generateKeyPair(String username) {
        try {
            PGPKeyPair keyPair = PGPKeyPair.generateRsaKeyPair(keySizeInBytes, username, username);
            exportKeysFromKeyPair(keyPair);
        } catch (PGPException e) {
            System.err.println("Ошибка генерации ключей: " + e);
        }
    }

    private void exportKeysFromKeyPair(PGPKeyPair keyPair) {
        try {
            keyPair.exportPublicKey(defaultKeysFilepath + "PublicKey_" + keyPair.getUserID() + ".pgp", true);
            keyPair.exportPrivateKey(defaultKeysFilepath + "PrivateKey_" + keyPair.getUserID() + ".pgp", true);
        } catch (NoPrivateKeyFoundException | IOException e) {
            System.err.println("Ошибка записи файлов ключей: "+ e);
        }
    }

    public String encryptString(String stringToEncrypt, String username) {
        try {
            FileInputStream publicEncryptionKeyFile = new FileInputStream(this.getPublicKeyFilepath(username));
            return pgpLib.encryptString(stringToEncrypt, publicEncryptionKeyFile);
        } catch (PGPException | IOException e) {
            System.err.println("Ошибка шифрования строки: " + e);
        }
        return null;
    }

    public String decryptString(String stringToEncrypt, String username) {
        try {
            FileInputStream publicDecryptionKeyFile = new FileInputStream(this.getPrivateKeyFilepath(username));
            return pgpLib.decryptString(stringToEncrypt, publicDecryptionKeyFile, username);
        } catch (PGPException | IOException e) {
            System.err.println("Ошибка расшифровки строки: " + e);
        }
        return null;
    }

    public String getPublicKeyFilepath(String username) {
        return defaultKeysFilepath + "PublicKey_" + username + ".pgp";
    }

    public String getPrivateKeyFilepath(String username) {
        return defaultKeysFilepath + "PrivateKey_" + username + ".pgp";
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
