package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Класс сервера со свойством <b>serverSocket</b>.
 *
 * @author Kirill Chezlov
 * @version 0.1
 */
public class Server {

    private final ServerSocket serverSocket;
    Database db;

    /**
     * Конструктор класса {@code Server}.
     * @param serverSocket сервер сокет для ожидания подключения
     */
    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        db = new Database();
        addNewFlagHandler();
    }

    private void addNewFlagHandler(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Utility for adding flag is ready...\n" +
                        "command as \"flag flag_name cipherName key cost\" if not key, write 0\n" +
                        "                example: flag flag{cesar} cesar 3 100");
                try {
                    Scanner in = new Scanner(System.in);
                    String instruction = in.nextLine();
                    if (instruction.startsWith("flag")) {
                        String[] str = instruction.split(" ");
                        switch (str[2]) {
                            case "cesar":
                                db.addNewFlag(str[1], StudyCiphers.cesarEncrypt(str[1], Integer.parseInt(str[3])), Integer.parseInt(str[4]));
                            case "scytale":
                                db.addNewFlag(str[1], StudyCiphers.scytaleEncrypt(str[1], Integer.parseInt(str[3])), Integer.parseInt(str[4]));
                            case "a1z26":
                                db.addNewFlag(str[1], StudyCiphers.a1z26Encrypt(str[1]), Integer.parseInt(str[4]));
                            case "base64":
                                db.addNewFlag(str[1], StudyCiphers.base64Encrypt(str[1]), Integer.parseInt(str[4]));
                            case "base32":
                                db.addNewFlag(str[1], StudyCiphers.base32Encrypt(str[1]), Integer.parseInt(str[4]));
                            case "viginereEn":
                                db.addNewFlag(str[1], StudyCiphers.viginereEnEncrypt(str[1], (str[3])), Integer.parseInt(str[4]));
                            case "viginereRu":
                                db.addNewFlag(str[1], StudyCiphers.viginereRuEncrypt(str[1], (str[3])), Integer.parseInt(str[4]));
                        }
                    }
                    System.out.println("Flag added");
                    in.close();
                } catch (Exception ignored) {
                    System.out.println("Add failed");
                }
            }
        }).start();
    }

    /**
     * Метод <b>startServer</b> запускает сервер и ожидает подключения к себе.
     * После этого создается и запускается новый поток для работы с
     * новоподключившимся клиентом.
     */
    public void startServer() {

        System.out.println("Сервер запущен...");

        try {

            while (!serverSocket.isClosed()) {

                Socket socket = serverSocket.accept();

                Thread newClientThread = new Thread(new ClientHandler(socket, db));
                newClientThread.start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void closeServer() {

        try {

            if (serverSocket != null) {
                serverSocket.close();
            }
            if (db != null) {
                db.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(9090);
        Server server = new Server(serverSocket);
        server.startServer();

    }
}
