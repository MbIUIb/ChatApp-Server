package server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Logger {
    private String loggerFilePath;
    private FileWriter writer;
    private BufferedWriter bufferWriter;

    public Logger(String path) throws IOException {
        loggerFilePath = path;
        writer = new FileWriter(loggerFilePath, true);
        bufferWriter = new BufferedWriter(writer);
    }

    public void logMessage(String username,  String message) throws IOException {
        Date date = new Date();
        String log = "["+ date + "] - "+ "user: "+ username + " message: " + message + "\n";
        bufferWriter.write(log);
        bufferWriter.close();
    }
}
