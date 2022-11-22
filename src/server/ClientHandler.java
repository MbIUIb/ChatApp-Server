package server;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Класс обработчика клиента, реализующий интерфейс {@code Runnable}.
 * <p>
 * Метод {@code run()} переопределен для обработки запросов клиента в
 * отдельном потоке.
 *
 * @author Kirill Chezlov
 * @version 0.1
 */
public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private String clientUsername;
    private PublicKey serverPublicKey;
    private PrivateKey serverPrivateKey;
    PublicKey clientPublicKey = null;
    private static Map<String, PublicKey> clientsPublicKeys = new HashMap<>();

    /**
     * Конструктор класса {@code server.ClientHandler}. Определяет
     * потоки ввода и вывода для дальнейшей работы.
     * @param socket сокет клиента
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;

        try {
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            closeEverything();
        }

        try {
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e); // обработать исключение
        }

        try {
            // создание публичного и приватного ключей сервера
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            serverPrivateKey = keyPair.getPrivate();
            serverPublicKey = keyPair.getPublic();

        } catch (NoSuchAlgorithmException e) {
            System.out.println("Ошибка создания ключей сервера!");
        }

    }

    /**
     * Переопределенный метод {@code run()} является онсновным
     * при работе обработчика клиентов. Он запускает вспомогательные
     * методы-обработчики.
     */
    @Override
    public void run() {
        try {
            // получение публичного ключа клиента
            clientPublicKey = (PublicKey) objectInputStream.readObject();

            //отправка публичного ключа сервера клиенту
            objectOutputStream.writeObject(serverPublicKey);
            objectOutputStream.flush();

        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            // ожидание получения имени клиета
            String mes = bufferedReader.readLine();
            this.clientUsername = decryptMessage(mes);
            addClientPublicKey(clientUsername, clientPublicKey);
            System.out.println(clientUsername);
        } catch (IOException e) {
            closeEverything();
            System.out.println("Ошибка получения имени клиента!");
        }

        clientHandlers.add(this);
        broadcastMessage(clientUsername + " has connected.", true);

        // получение сообщений клиента
        String messageFromClient;
        while (socket.isConnected()) {
             try {
                 messageFromClient = bufferedReader.readLine();
                 broadcastMessage(decryptMessage(messageFromClient), false);
             } catch (IOException e) {
                 closeEverything();
                 break;
             }
        }

    }

    /**
     * Рассылает сообщение всем подключенным клиентам, кроме отправившего
     * @param messageToSend сообщение
     * @param isService если {@code true}, то сообщение отправляется от
     *                  имени сервера
     */
    public void broadcastMessage(String messageToSend, boolean isService) {

        for (ClientHandler clientHandler : clientHandlers) {
            String senderUsername;
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");

            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {

                    if (!isService) {
                        senderUsername = clientUsername + ": ";
                    } else senderUsername = "SERVER: ";

                    String msg = "[" + formatter.format(new Date()) + "]" + senderUsername + messageToSend;

                    clientHandler.bufferedWriter.write(encryptMessage(msg, clientsPublicKeys.get(clientHandler.clientUsername)));
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();

                }
            } catch (IOException e) {
                closeEverything();
            }

        }

    }

    private void addClientPublicKey(String clientUsername, PublicKey publicKey) {
        clientsPublicKeys.put(clientUsername, publicKey);
    }

    private String encryptMessageByName(String messageToEncrypt, String clientUsername) {
        PublicKey clientPublicKey = clientsPublicKeys.get(clientUsername);
        return this.encryptMessage(messageToEncrypt, clientPublicKey);
    }

    private String encryptMessage(String messageToEncrypt, PublicKey clientPublicKey) {
        if (clientPublicKey == null)
            throw new RuntimeException("Неизвестный клиент!");

        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);

            byte[] messageToEncryptBytes = messageToEncrypt.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedMessage = cipher.doFinal(messageToEncryptBytes);

            return Base64.getEncoder().encodeToString(encryptedMessage);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private String decryptMessage(String messageToDecrypt) {
        byte[] messageToDecryptBytes = Base64.getDecoder().decode(messageToDecrypt);

        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);

            byte[] decryptedMessage = cipher.doFinal(messageToDecryptBytes);

            return new String(decryptedMessage, StandardCharsets.UTF_8);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Удаляет пользователя из списка подключенных и уведомляет
     * об этом всех в чате
     */
    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage(clientUsername + " has disconnected!", true);
    }

    /**
     * Закрывает все сокеты и потоки ввода/вывода
     */
    public void closeEverything() {

        removeClientHandler();
        try {

            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (objectInputStream != null) {
                objectInputStream.close();
            }
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
