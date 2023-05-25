package microservices.api_gateway;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class ApiGateway {
    public static void main(String[] args) {

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("404;Config File loading ERROR." + e.getMessage());
            return;
        }

        int gatewayPort = Integer.parseInt(properties.getProperty("api.gateway.port"));

        try (ServerSocket serverSocket = new ServerSocket(gatewayPort)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> processRequest(clientSocket, properties)).start();
            }
        } catch (IOException e) {
            System.err.println("503;APIGateway ERROR." + e.getMessage());
        }
    }

    private static void processRequest(Socket clientSocket, Properties properties) {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = input.readLine();
            String[] requestParts = request.split(";", 2);
            String requestType = requestParts[0];

            String targetServicePort;
            String targetServiceIP;
            switch (requestType) {
                case "register" -> {
                    targetServicePort = properties.getProperty("registration.service.port");
                    targetServiceIP = properties.getProperty("registration.service.ip");
                }
                case "login" -> {
                    targetServicePort = properties.getProperty("login.service.port");
                    targetServiceIP = properties.getProperty("login.service.ip");
                }
                case "post", "get_posts" -> {
                    targetServicePort = properties.getProperty("post.service.port");
                    targetServiceIP = properties.getProperty("post.service.ip");
                }
                case "upload_file", "download_file" -> {
                    targetServicePort = properties.getProperty("file.service.port");
                    targetServiceIP = properties.getProperty("file.service.ip");
                }
                default -> {
                    System.out.println("400;Error. Unknown type of request.");
                    return;
                }
            }

            int targetPort = Integer.parseInt(targetServicePort);

            if (requestType.equals("upload_file")) {
                try (Socket targetSocket = new Socket(targetServiceIP, targetPort);
                     PrintWriter targetOutput = new PrintWriter(targetSocket.getOutputStream(), true);
                     BufferedReader targetInput = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()))) {
                    String[] parts = request.split(";");
                    String destinationFileName = parts[parts.length - 2];
                    String encodedFile = parts[parts.length - 1];

                    targetOutput.println("upload_file;" + destinationFileName + ";" + encodedFile);

                    String response = targetInput.readLine();
                    output.print(response);
                    output.flush();
                } catch (IOException e) {
                    System.err.println("503;File forwarding ERROR.  " + e.getMessage());
                }
            } else if (requestType.equals("download_file")) {
                try (Socket targetSocket = new Socket(targetServiceIP, targetPort)) {
                    PrintWriter targetOutput = new PrintWriter(targetSocket.getOutputStream(), true);
                    BufferedReader targetInput = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()));

                    targetOutput.println(request);

                    String response = targetInput.readLine();
                    output.println(response);
                    output.flush();
                } catch (IOException e) {
                    System.err.println("503;Request forwarding ERROR." + e.getMessage());
                }
            } else {
                try (Socket targetSocket = new Socket(targetServiceIP, targetPort)) {
                    PrintWriter targetOutput = new PrintWriter(targetSocket.getOutputStream(), true);
                    BufferedReader targetInput = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()));

                    targetOutput.println(request);
                    System.out.println("Received connection.");
                    String response = targetInput.readLine();
                    output.print(response);
                    output.flush();
                } catch (IOException e) {
                    System.err.println("503;Request forwarding ERROR." + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("503;Request processing ERROR." + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("500;Internal server error." + e.getMessage());
            }
        }
    }
}