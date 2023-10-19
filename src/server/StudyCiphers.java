package server;

import org.bouncycastle.util.encoders.Base32;

import java.util.Base64;

import com.stackoverflow.ru.*;

public class StudyCiphers {
    public static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    public static String cesarEncrypt(String message, int shift){
        StringBuilder cipherText = new StringBuilder();

        for (int symbolIndex = 0; symbolIndex < message.length(); symbolIndex++) {
            int messageCharPosition = ALPHABET.indexOf(message.toLowerCase().charAt(symbolIndex));
            int encryptCharPosition = (shift + messageCharPosition) % 26;
            char replaceVal = ALPHABET.charAt(encryptCharPosition);

            if (Character.isUpperCase(message.charAt(symbolIndex))){
                cipherText.append(Character.toUpperCase(replaceVal));
            } else {
                cipherText.append(Character.toLowerCase(replaceVal));
            }
        }
        return cipherText.toString();
    }

    public static String scytaleEncrypt(String message, int shift){
        StringBuilder cipherText = new StringBuilder();

        for (int startSymbolIndex = 0; startSymbolIndex < shift+1; startSymbolIndex++){
            for (int cryptSymbolIndex = startSymbolIndex; cryptSymbolIndex < message.length(); cryptSymbolIndex += shift+1){
                cipherText.append(message.charAt(cryptSymbolIndex));
            }
        }
        return cipherText.toString();
    }

    public static String a1z26Encrypt(String message) {
        StringBuilder cipherText = new StringBuilder();

        for (int symbolIndex = 0; symbolIndex < message.length(); symbolIndex++) {
            if (ALPHABET.indexOf(message.charAt(symbolIndex)) != -1 &&
                    ALPHABET.indexOf(message.charAt(symbolIndex)) != 0){
                cipherText.append(ALPHABET.indexOf(message.charAt(symbolIndex))+1);
            } else {
                cipherText.append(message.charAt(symbolIndex));
            }
        }

        return cipherText.toString();
    }

    public static String base64Encrypt(String message) {
        return Base64.getEncoder().encodeToString(message.getBytes());
    }

    public static String base32Encrypt(String message) {
        return Base32.toBase32String(message.getBytes());
    }

    public static String viginereEnEncrypt(String message, String key) {
        Vigenener v = new Vigenener(97, 26);
        return v.encrypt(message, key);
    }

    public static String viginereRuEncrypt(String message, String key) {
        final Vigenener v = new Vigenener(1072, 33);
        return v.encrypt(message, key);
    }

    public static void main(String[] args) {
        String s = "flag{newFlag6}";
        System.out.println(viginereRuEncrypt(s, "ру"));
    }
}
