package server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
    private String clientUsername;

    /**
     * Конструктор класса {@code server.ClientHandler}. Определяет
     * потоки ввода и вывода для дальнейшей работы.
     * @param socket сокет клиента
     */
    public ClientHandler(Socket socket) {
        try {

            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
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
            // ожидание получения имени клиета
            this.clientUsername = bufferedReader.readLine();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
        clientHandlers.add(this);
        broadcastMessage(clientUsername + " has connected.", true);

        // получение сообщений клиента
        String messageFromClient;
        while (socket.isConnected()) {
             try {
                 messageFromClient = bufferedReader.readLine();
                 broadcastMessage(messageFromClient, false);
             } catch (IOException e) {
                 closeEverything(socket, bufferedReader, bufferedWriter);
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
            SimpleDateFormat formatter = new SimpleDateFormat("dd:MM:yyyy HH:mm");

            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {

                    if (!isService) {
                        senderUsername = clientUsername + ": ";
                    } else senderUsername = "SERVER: ";

                    clientHandler.bufferedWriter.write("[" + formatter.format(new Date()) + "]" +
                            senderUsername + messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();

                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }

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
     * @param socket
     * @param bufferedReader
     * @param bufferedWriter
     */
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {

        removeClientHandler();
        try {

            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
