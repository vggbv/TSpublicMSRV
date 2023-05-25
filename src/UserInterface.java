import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.Scanner;

public class UserInterface {
    private static String currentUser = "";
    private static String destinationFileName = "";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("""
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    Choose wisely!
                    """);
            if (currentUser.equals("")) {
                System.out.println("""
                        REGISTER : Register
                        LOGIN : Login
                        """
                );
            }
            System.out.println("""
                    POST : Post something
                    GET-POSTS : Show last 10 posts
                    UPLOAD : Upload file to the server
                    DOWNLOAD : Download file
                    LOGOUT : Logout
                    EXIT : Goodbye
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    Your choice:\s"""
            );

            String choice = scanner.nextLine().toUpperCase();

            if (choice.equals("EXIT")) {
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~END~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                break;
            } else if (choice.equals("LOGOUT")) {
                if (currentUser.equals("")) {
                    System.out.println("You are not logged in!");
                    continue;
                }
                currentUser = "";
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Logged out~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                continue;
            }
            String type;
            String data;
            String username = "";
            String password;

            switch (choice) {
                case "REGISTER" -> {
                    if (currentUser.equals("")) {
                        type = "register";
                        System.out.print("Username: ");
                        username = scanner.nextLine();
                        System.out.print("Password: ");
                        password = scanner.nextLine();
                        data = username + ";" + password;
                    } else {
                        System.out.println("Please, choose correct option.");
                        continue;
                    }
                }
                case "LOGIN" -> {
                    if (currentUser.equals("")) {
                        type = "login";
                        System.out.print("Username: ");
                        username = scanner.nextLine();
                        System.out.print("Password: ");
                        password = scanner.nextLine();
                        data = username + ";" + password;
                    } else {
                        System.out.println("Please, choose correct option.");
                        continue;
                    }
                }
                case "POST" -> {
                    if (currentUser.equals("")) {
                        System.out.println("In order to do that, please, log in.");
                        continue;
                    }
                    type = "post";
                    System.out.print("Post content: ");
                    data = currentUser + ";" + scanner.nextLine();
                }
                case "GET-POSTS" -> {
                    if (currentUser.equals("")) {
                        System.out.println("In order to do that, please, log in.");
                        continue;
                    }
                    type = "get_posts";
                    System.out.println("Last 10 posts:\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                    data = "";
                }
                case "UPLOAD" -> {
                    if (currentUser.equals("")) {
                        System.out.println("In order to do that, please, log in.");
                        continue;
                    }
                    type = "upload_file";
                    System.out.print("Type file path: ");
                    String filePath = scanner.nextLine();
                    if (filePath.equals("")) {
                        System.out.println("Incorrect path.");
                        continue;
                    }
                    System.out.print("Type in new name for uploaded file: ");
                    destinationFileName = scanner.nextLine();
                    data = currentUser + ";" + filePath;
                }
                case "DOWNLOAD" -> {
                    if (currentUser.equals("")) {
                        System.out.println("In order to do that, please, log in.");
                        continue;
                    }
                    type = "download_file";
                    System.out.print("Name the file you want to download: ");
                    String fileName = scanner.nextLine();
                    data = currentUser + ";" + fileName;
                }
                default -> {
                    System.out.println("Please, choose correct option.");
                    continue;
                }
            }

            String request = type + ";" + data;

            String response = sendRequestToApiGateway(request);

            String[] responseParts = response.split(";", 2);
            String responseType = responseParts[0];
            String responseData = responseParts.length > 1 ? responseParts[1] : "";

            if (responseType.equals("200")) {
                if (type.equals("login") || type.equals("register")) {
                    currentUser = username;
                } else if (type.equals("upload_file") | type.equals("download_file")) {
                    System.out.println("File transfer successful.");
                }
            }
            if (responseType.equals("299")) {
                String[] posts = responseData.split("\t%\t");
                for (String post : posts) {
                    System.out.println(post);
                }
                continue;
            }
            System.out.println(responseData);
        }
    }

    private static String sendRequestToApiGateway(String request) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("Config File loading ERROR. " + e.getMessage());
            return "404;Config File ERROR";
        }
        int apiGatewayPort = Integer.parseInt(properties.getProperty("api.gateway.port"));
        String apiGatewayIP = properties.getProperty("api.gateway.ip");

        String[] requestPart = request.split(";");
        if (requestPart[0].equals("upload_file")) {
            String filePath = requestPart[2];
            try {
                byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
                String encodedFile = Base64.getEncoder().encodeToString(fileBytes);
                request = requestPart[0] + ";" + requestPart[1] + ";" + destinationFileName + ";" + encodedFile;
            } catch (IOException e) {
                System.err.println("File reading ERROR: " + e.getMessage());
                return "415;File ERROR";
            }
            try (Socket socket = new Socket(apiGatewayIP, apiGatewayPort);
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                output.println(request);
                return input.readLine();
            } catch (IOException e) {
                System.err.println("APIGateway connection problem: " + e.getMessage());
                return "503;ApiGateway ERROR";
            }
        } else if (requestPart[0].equals("download_file")) {
            try (Socket socket = new Socket(apiGatewayIP, apiGatewayPort);
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                output.println(request);
                String response = input.readLine();
                String[] responseParts = response.split(";", 2);

                if ("200".equals(responseParts[0])) {
                    String encodedFile = responseParts[1];
                    byte[] fileBytes = Base64.getDecoder().decode(encodedFile);
                    String fileName = requestPart[2];
                    String destinationPath = System.getProperty("user.home") + File.separator + "DOWNLOADED_" + fileName;
                    try {
                        Files.write(Paths.get(destinationPath), fileBytes);
                        return "200;File downloaded successfully: " + destinationPath;
                    } catch (IOException e) {
                        System.err.println("File writing ERROR: " + e.getMessage());
                        return "415;File ERROR";
                    }
                } else {
                    return response;
                }
            } catch (IOException e) {
                System.err.println("APIGateway connection problem: " + e.getMessage());
                return "503;ApiGateway ERROR";
            }
        } else {
            try (Socket socket = new Socket(apiGatewayIP, apiGatewayPort);
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                output.println(request);
                return input.readLine();
            } catch (IOException e) {
                System.err.println("APIGateway connection problem: " + e.getMessage());
                return "503;ApiGateway ERROR";
            }
        }
    }
}