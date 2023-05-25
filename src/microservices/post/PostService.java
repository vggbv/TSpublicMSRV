package microservices.post;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class PostService {
    public static void main(String[] args) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("404;Config File loading ERROR. " + e.getMessage());
            return;
        }

        int postPort = Integer.parseInt(properties.getProperty("post.service.port"));

        try (ServerSocket serverSocket = new ServerSocket(postPort)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new PostWorker(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("500;Post Service ERROR. " + e.getMessage());
        }
    }
}