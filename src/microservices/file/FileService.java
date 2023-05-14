package microservices.file;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class FileService {
    public static void main(String[] args) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("Config File loading ERROR. " + e.getMessage());
            return;
        }

        int loginPort = Integer.parseInt(properties.getProperty("file.service.port"));

        try (ServerSocket serverSocket = new ServerSocket(loginPort)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new FileWorker(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("File Service ERROR: " + e.getMessage());
        }
    }
}