package interfaces;

import com.didisoft.pgp.PGPKeyPair;

public interface PGPInterface {
    public void generateKeyPair(String username);
    public String encryptString(String stringToEncrypt, String username);
    public String decryptString(String stringToEncrypt, String username);
    public String getPublicKeyFilepath(String username);
}
