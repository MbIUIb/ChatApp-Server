package server;


import org.sqlite.JDBC;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс, содержащий методы для работы с базой данных.
 *
 * @author Kirill Chezlov
 * @version 1.0
 */
public class Database {
    private final String dbPath = "jdbc:sqlite:src/server/res/db/database.db";
    private Connection connection;

    /**
     * подключение к менеджеру драйверов для работы с БД, создание таблиц
     * с данными пользователей и историей сообщений, если их нет
     */
    public Database() {
        try {
            DriverManager.registerDriver(new JDBC());
            this.connection = DriverManager.getConnection(dbPath);
            connection.setAutoCommit(true);
            createUsersTable();
            createChatHistoryTable();
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к БД: " + e);
        }
    }

    /**
     * создание таблицы с данными пользователей
     * <p>
     * 'username' TEXT PRIMARY KEY
     * <p>
     * 'password' TEXT
     * <p>
     * 'email' TEXT
     */
    public void createUsersTable() {
        try{
            String query = "CREATE TABLE IF NOT EXISTS 'users'" +
                    "('username' TEXT PRIMARY KEY, 'password' TEXT, 'email' TEXT)";
            Statement statement = connection.createStatement();
            statement.execute(query);

        } catch (SQLException e) {
            System.err.println("Ошибка создания таблицы 'users': " + e);
        }
    }

    /**
     * создание таблицы для хранения истории сообщений
     * <p>
     * 'id' INTEGER PRIMARY KEY AUTOINCREMENT
     * <p>
     * 'date' TEXT
     * <p>
     * 'sender' TEXT
     * <p>
     * 'message' TEXT
     */
    public void createChatHistoryTable() {
        try{
            String query = "CREATE TABLE IF NOT EXISTS 'chat_history'" +
                    "('id' INTEGER PRIMARY KEY AUTOINCREMENT, 'date' TEXT, 'sender' TEXT, 'message' TEXT)";
            Statement statement = connection.createStatement();
            statement.execute(query);

        } catch (SQLException e) {
            System.err.println("Ошибка создания таблицы 'chat_history': " + e);
        }
    }

    /**
     * добавление нового пользователя
     * @param username login нового пользователя
     * @param password пароль нового пользователя
     * @param email email нового пользователя
     */
    public void createUser(String username, String password, String email) {
        try {
            String query = "INSERT INTO 'users' ('username', 'password', 'email') VALUES(?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, email);
            statement.execute();

        } catch (SQLException e) {
            System.err.println("Ошибка добавления данных: " + e);
        }
    }

    /**
     * аутентификация пользователя
     * @param username login пользователя
     * @param password пароль пользователя
     * @return {@code true} в случает совпадения данных, иначе {@code false}
     */
    public boolean authenticationUser(String username, String password) {
        try {
            String query = "SELECT username FROM users WHERE username=? AND password=?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);

            ResultSet resultSet = statement.executeQuery();
            return resultSet.getString("username").equals(username);
        } catch (SQLException ignored) {}
        return false;
    }

    /**
     * возвращает email пользователя
     * @param username имя пльзователя
     * @return {@code String}
     */
    public String getEmail(String username) {
        try {
            String query = "SELECT email FROM users WHERE username=?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();
            return resultSet.getString("email");
        } catch (SQLException ignored) {}
        return null;
    }

    /**
     * обновляет пароль пользователя на новый
     * @param username имя пользователя
     * @param password новый пароль
     */
    public void setPassword(String username, String password) {
        try {
            String query = "UPDATE users SET password=? WHERE username=?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, password);
            statement.setString(2, username);

            statement.execute();
        } catch (SQLException e) {
            System.err.println("Ошибка изменения пароля: " + e);
        }
    }

    /**
     * добавляет сообщение в историю сообщений
     * @param date дата сообщения
     * @param sender имя отправителя
     * @param message сообщение
     */
    public void addNewMessage(String date, String sender, String message) {
        try {
            String query = "INSERT INTO 'chat_history' ('date', 'sender', 'message') VALUES(?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, date);
            statement.setString(2, sender);
            statement.setString(3, message);
            statement.execute();

        } catch (SQLException e) {
            System.err.println("Ошибка добавления данных: " + e);
        }
    }

    /**
     * возвращает словарь массивов строк, содержащих данные сообщений
     * @return {@code Map<Integer, String[]>}
     */
    public Map<Integer, String[]> getAllMessage() {
        try {
            Map<Integer, String[]> messages = new HashMap<>();
            Statement statement = connection.createStatement();
            String query = "SELECT * FROM 'chat_history'";
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                messages.put(resultSet.getInt("id"), new String[]{resultSet.getString("date"),
                        resultSet.getString("sender"),
                        resultSet.getString("message")});
            }
            return messages;

        } catch (SQLException e) {
            System.err.println("Ошибка получения данных: " + e);
        }
        return null;
    }

    /**
     * возвращает true или false в зависимости от наличия пользователя в истории чата
     * @param username имя пользователя
     * @return {@code true}, если пользователь хотябы раз отправлял сообщение в чат, {@code false} иначе
     */
    public boolean userInChat(String username) {
        try {
            String query = "SELECT sender FROM chat_history WHERE sender=?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();
            return resultSet.getString("sender").equals(username);
        } catch (SQLException ignored) {}
        return false;
    }

    /**
     * закрывает соединение
     */
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.err.println("Ошибка закрытия БД: " + e);
        }

    }


    public static void main(String[] args) {
        Database database = new Database();
    }
}
