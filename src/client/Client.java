package client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private PGP pgp;
    private String serverName;
    private String username;
    private String age;
    private String email;
    private String clientPublicKey;
    private String clientPrivateKey;
    private String serverPublicKey;

    public Client(Socket socket, String username, String email) {
        this.socket = socket;
        this.username = username;
        this.email = email;

        pgp = new PGP(username);
        clientPublicKey = getStringFromFile(pgp.getPublicKeyFilepath(username));
        clientPrivateKey = getStringFromFile(pgp.getPrivateKeyFilepath(username));
        serverName = pgp.generateSecretCode(15);

        try {
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            closeEverything();
        }

    }

    public void startClient() {

        try {
            // отправка публичного ключа клиента
            objectOutputStream.writeObject(clientPublicKey);
            objectOutputStream.flush();

            // получение публичного ключа сервера
            serverPublicKey = (String) objectInputStream.readObject();
            writeStringToFile(serverPublicKey, serverName);

        } catch (Exception e) {
            System.err.println("Ошибка обмена ключами: "+ e);
        }

        try {
            objectOutputStream.writeObject(pgp.encryptString(username, serverName));
            objectOutputStream.flush();

            objectOutputStream.writeObject(pgp.encryptString(email, serverName));
            objectOutputStream.flush();
        } catch (IOException e) {
            System.err.println("Ошибка отправки имени!");
        }


        listenForMessage();
        sendMessage();
    }

    public void sendMessage() {

        try {

            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {

                String messageToSend = scanner.nextLine();

                objectOutputStream.writeObject(pgp.encryptString(messageToSend, serverName));
                objectOutputStream.flush();

            }

        } catch (IOException e) {
            closeEverything();
            System.err.println("Ошибка отправки сообщения");
        }

    }

    public void listenForMessage() {
        // создание и запуск нового потока для получения сообщений
        new Thread(new Runnable() { // реализация анонимного класса
            @Override
            public void run() {

                String messageFromChat;

                while (socket.isConnected()) {
                    try {

                        messageFromChat = (String) objectInputStream.readObject();
                        System.out.println(pgp.decryptString(messageFromChat, username));

                    } catch (IOException | ClassNotFoundException e) {
                        closeEverything();
                    }
                }

            }
        }).start();
    }

    private String getStringFromFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e);
        }
        return null;
    }

    private void writeStringToFile(String str, String username) {
        try {
            String path = "src/client/res/PublicKey_" + username + ".pgp";
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(str);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.err.println("Ошибка записи ключа в файл: " + e);
        }
    }

    public void closeEverything() {

        try {
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

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        String username = null;
        String email = null;
        boolean flag = true;

        while (flag) {
            System.out.println("Введите свой ник для чата: ");
            username = scanner.nextLine();

            System.out.println("Введите email: ");
            email = scanner.nextLine();

            if (username != null && !username.equals("")) {
                flag = false;
            }
        }


        Socket socket = new Socket("localhost", 9090);
        Client client = new Client(socket, username, email);
        client.startClient();

    }
}
