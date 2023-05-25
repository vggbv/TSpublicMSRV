package microservices.file;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class FileWorker implements Runnable {
    private final Socket clientSocket;

    public FileWorker(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String request = input.readLine();
            String[] requestFile = request.split(";");

            if (requestFile[0].equals("upload_file")) {
                String destinationFileName = requestFile[1];
                String encodedFile = requestFile[2];
                byte[] fileBytes = Base64.getDecoder().decode(encodedFile);

                String destinationPath = System.getProperty("user.home") + File.separator + destinationFileName;
                Files.write(Paths.get(destinationPath), fileBytes);
                output.println("200;File uploaded.");
            } else if (requestFile[0].equals("download_file")) {
                String fileName = requestFile[2];
                String filePath = System.getProperty("user.home") + File.separator + fileName;

                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    byte[] fileBytes = Files.readAllBytes(path);
                    String encodedFile = Base64.getEncoder().encodeToString(fileBytes);
                    output.println("200;" + encodedFile);
                } else {
                    output.println("404;File not found.");
                }
            }
        } catch (IOException e) {
            System.err.println("503;Request processing ERROR: " + e.getMessage());
        } finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("500;Socket ERROR: " + e.getMessage());
                }
            }
        }
    }
}