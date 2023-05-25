package microservices.registration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegistrationWorker implements Runnable {
    private final Socket clientSocket;

    public RegistrationWorker(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = input.readLine();
            String[] userData = request.split(";");

            if (userData.length != 3) {
                output.println("406;Wrong registration data.");
                return;
            }

            String username = userData[1];
            String password = userData[2];

            try (Connection connection = DatabaseConnection.getConnection()) {
                PreparedStatement checkUserStatement = connection.prepareStatement("SELECT * FROM users WHERE username = ?");
                checkUserStatement.setString(1, username);
                ResultSet resultSet = checkUserStatement.executeQuery();

                if (resultSet.next()) {
                    output.println("409;User already existing in DB.");
                    output.flush();
                    return;
                }

                PreparedStatement insertUserStatement = connection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
                insertUserStatement.setString(1, username);
                insertUserStatement.setString(2, password);
                insertUserStatement.executeUpdate();

                output.println("200;Successfully registered. Congratulations.");
                output.flush();
            } catch (SQLException e) {
                System.err.println("503;Registration Worker ERROR." + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("503;Registration Worker ERROR." + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("500;Registration Worker: Socket ERROR." + e.getMessage());
            }
        }
    }
}