package microservices.registration;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class RegistrationService {
    public static void main(String[] args) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("Config File loading ERROR.  " + e.getMessage());
            return;
        }

        int registrationPort = Integer.parseInt(properties.getProperty("registration.service.port"));

        try (ServerSocket serverSocket = new ServerSocket(registrationPort)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new RegistrationWorker(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Registration Service ERROR.    " + e.getMessage());
        }
    }
}