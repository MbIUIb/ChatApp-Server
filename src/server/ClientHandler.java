package server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Класс обработчика клиента, реализующий интерфейс {@code Runnable}.
 * <p>
 * Метод {@code run()} переопределен для обработки запросов клиента в
 * отдельном потоке.
 *
 * @author Kirill Chezlov
 * @version 1.1
 */
public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private final Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private EmailSender emailSender;
    private final PGP pgp;
    private final String serverName;
    private String clientUsername;
    private final String serverPublicKey;
    private String clientPublicKey;
    static int severNum = 0;
    private final Database db;

    /**
     * Конструктор класса {@code ClientHandler}. Определяет
     * потоки ввода и вывода и pgp криптографер для дальнейшей работы.
     * @param socket сокет клиента
     */
    public ClientHandler(Socket socket, Database db) {
        this.socket = socket;
        this.db = db;

        severNum += 1;
        serverName = "sever_" + severNum;

        pgp = new PGP(serverName);
        serverPublicKey = getStringFromFile(pgp.getPublicKeyFilepath(serverName));

        try {
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Ошибка создания I/O потоков: " + e);
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
            clientPublicKey = (String) objectInputStream.readObject();

            //отправка публичного ключа сервера клиенту
            objectOutputStream.writeObject(serverPublicKey);
            objectOutputStream.flush();

        } catch (Exception e) {
            System.out.println("Ошибка обмена ключами: " + e);
        }

        try {
            // ожидание получения имени клиета
            String username = (String) objectInputStream.readObject();
            this.clientUsername = pgp.decryptString(username, serverName);
            System.out.println(clientUsername);

            // сохранение ключа в файл
            writeStringToFile(clientPublicKey, clientUsername);

        } catch (IOException | ClassNotFoundException e) {
            closeEverything();
            System.out.println("Ошибка получения имени клиента!");
        }

        listenForMessage();

    }

    /**
     * Постоянно прослушивает входной поток и ожидает получения сообщений.
     * При поучении собщения рассылает его всем клиентам
     */
    public void listenForMessage() {
        String messageFromClient;
        boolean runningFlag = true;

        while (socket.isConnected() & runningFlag) {
            try {
                messageFromClient = pgp.decryptString((String) objectInputStream.readObject(), serverName);

                if (messageFromClient.length() > 7 &&
                        messageFromClient.startsWith("sign_in")) {
                    runningFlag = signInProcess(messageFromClient);

                } else if (messageFromClient.length() > 7 &&
                        messageFromClient.startsWith("sign_up")) {
                    runningFlag = signUpProcess(messageFromClient);

                } else if (messageFromClient.length() > 17 &&
                        messageFromClient.startsWith("password_recovery")) {
                    runningFlag = passwordRecovery(messageFromClient);

                } else {
                    broadcastMessage(messageFromClient, false);
                }

            } catch (IOException | ClassNotFoundException e) {
                closeEverything();
                break;
            }
        }
    }

    /**
     * Обрабатывает процесс входа клиента
     * @param messageFromClient служебная строка с данными от клиента
     */
    private boolean signInProcess(String messageFromClient) {
        String[] str = messageFromClient.split("\\|");
        String username = str[1];
        String password = str[2];

        if (db.authenticationUser(username, password) && !clientInClientHandlers()) {
            sendMessage("successful_sign_in");
            // добавление подключившегося клиента в общий список
            clientHandlers.add(this);

            sendChatHistory();
            if (!db.userInChat(clientUsername)) {
                broadcastMessage(clientUsername + " has connected!", true);
            }

            return true;
        } else {
            sendMessage("failed_sign_in");
            closeEverything();
            return false;
        }
    }

    /**
     * Обрабатывает процесс регистрации клиента
     * @param messageFromClient служебная строка с данными от клиента
     */
    private boolean signUpProcess(String messageFromClient) {
        String[] str = messageFromClient.split("\\|");
        String username = str[1];
        String password = str[2];
        String email = str[3];

        if (db.userNotRegistered(username)) {
            sendMessage("successful_pre_sign_up");

            String secretCode = pgp.generateSecretCode(6);

            emailSender = new EmailSender(email);
            emailSender.sendMessage("JavaChat registration secret code", "Secret code: " + secretCode);

            while (socket.isConnected()) {

                String userSecretCode = waitMessage();
                if (userSecretCode.equals("back")) {
                    return false;
                }

                if (secretCode.equals(userSecretCode)) {
                    db.createUser(username, password, email);
                    sendMessage("successful_sign_up");
                    closeEverything();
                    return true;
                } else {
                    sendMessage("failed_sign_up");
                }
            }

        } else {
            sendMessage("failed_pre_sign_up");
            closeEverything();
        }
        return false;
    }

    /**
     * Обрабатывает процесс сброса пароля
     * @param messageFromClient служебная строка с данными от кклиента
     */
    private boolean passwordRecovery(String messageFromClient) {
        String[] str = messageFromClient.split("\\|");
        String username = str[1];

        if (!db.userNotRegistered(username)) {
            sendMessage("begin_password_recovery");

            String secretCode = pgp.generateSecretCode(6);

            emailSender = new EmailSender(db.getEmail(username));
            emailSender.sendMessage("JavaChat confirm new password secret code",
                    "Secret code:" + secretCode);

            while (socket.isConnected()) {
                String s = waitMessage();
                if (s.equals("back")) {
                    return false;
                }
                String[] str1 = s.split("\\|");
                username = str1[1];
                String newPassword = str1[2];
                String userSecretCode = str1[3];

                if (secretCode.equals(userSecretCode)) {
                    db.setPassword(username, newPassword);
                    sendMessage("successful_password_recovery");
                    closeEverything();
                    return true;
                } else {
                    sendMessage("invalid_password_recovery");
                }

            }

        } else {
            sendMessage("failed_begin_password_recovery");
            closeEverything();
        }
        return false;
    }

    /**
     * Отправляет сообщение клиенту
     * @param message отправляемое сообщение
     */
    public void sendMessage(String message) {
        try {
            objectOutputStream.writeObject(pgp.encryptString(message, clientUsername));
            objectOutputStream.flush();
        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения: " + e);
            removeClientHandler();
        }
    }

    /**
     * Ожидает получение сообщения от клиента и возвращает его
     * @return возвращает присланное сообщение, либо пустую строку в случае неудачного получения
     */
    public String waitMessage() {
        try {
            return pgp.decryptString((String) objectInputStream.readObject(), serverName);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка получения сообщения: " + e);
            e.printStackTrace();
            removeClientHandler();
        }
        return "";
    }

    /**
     * Рассылает сообщение всем подключенным клиентам, кроме отправившего
     * @param messageToSend сообщение
     * @param isService если {@code true}, то сообщение отправляется от
     *                  имени сервера
     */
    public void broadcastMessage(String messageToSend, boolean isService) {
        String senderUsername;
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yy H:mm");
        String date = formatter.format(new Date());

        if (!isService) {
            senderUsername = clientUsername;
        } else senderUsername = "SERVER";

        String msg = date + "|" + senderUsername + "|" + messageToSend;

        db.addNewMessage(date, senderUsername, messageToSend);

        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {

                    clientHandler.objectOutputStream.writeObject(pgp.encryptString(msg, clientHandler.clientUsername));
                    clientHandler.objectOutputStream.flush();

                }
            } catch (IOException e) {
                removeClientHandler();
            }

        }

    }

    /**
     * Отправляет историю переписки, записанную в БД
     */
    private void sendChatHistory() {
        Map<Integer, String[]> history = db.getAllMessage();

        for (int i = 1; i < history.size()+1; i++) {
            String msg = history.get(i)[0] + "|" + history.get(i)[1] + "|" + history.get(i)[2];

            try {
                objectOutputStream.writeObject(pgp.encryptString(msg, clientUsername));
                objectOutputStream.flush();
            } catch (IOException e) {
                removeClientHandler();
            }
        }
    }

    /**
     * позволяет получить весь текст из файла с заданным путем
     * @param path путь к файлу
     * @return String, содержащий весь текст из файла
     */
    private String getStringFromFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e);
        }
        return null;
    }

    /**
     * позволяет записать текст в файл, содержащий в названии
     * имя пользователя-владельца
     * @param str текст для записи в файл
     * @param username имя пользователя-владельца файла
     */
    private void writeStringToFile(String str, String username) {
        try {
            String path = pgp.defaultKeysFilepath + "PublicKey_" + username + ".pgp";
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(str);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.err.println("Ошибка записи ключа в файл: " + e);
        }
    }

    public boolean clientInClientHandlers() {
        boolean in = false;
        if (clientHandlers.size() > 0) {
            for (ClientHandler clientHandler : clientHandlers) {
                if (clientHandler.clientUsername.equals(clientUsername)) {
                    in = true;
                    break;
                }
            }
        }
        return in;
    }

    /**
     * Удаляет пользователя из списка подключенных и уведомляет
     * об этом всех в чате
     */
    public void removeClientHandler() {
        clientHandlers.remove(this);
    }

    /**
     * Закрывает все сокеты и потоки ввода/вывода
     */
    public void closeEverything() {

        removeClientHandler();
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

}
