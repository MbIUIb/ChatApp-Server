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
                // command as "flag flag_name cost"
                //flag flag{cesar} 100
                Scanner in = new Scanner(System.in);
                String instruction = in.nextLine();
                if (instruction.startsWith("flag")){
                    String[] str = instruction.split(" ");
                    db.addNewFlag(str[1], Integer.parseInt(str[2]));
                }
                System.out.println("Flag added");
                in.close();
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
