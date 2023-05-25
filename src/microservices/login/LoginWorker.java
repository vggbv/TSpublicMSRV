package microservices.login;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginWorker implements Runnable {
    private final Socket clientSocket;

    public LoginWorker(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = input.readLine();
            String[] userData = request.split(";");
            if (userData.length != 3) {
                output.println("403;Wrong credentials.");
                return;
            }

            String username = userData[1];
            String password = userData[2];
            try (Connection connection = DatabaseConnection.getConnection()) {
                PreparedStatement checkUserStatement = connection.prepareStatement("SELECT * FROM users WHERE username = ?");
                checkUserStatement.setString(1, username);
                ResultSet resultSet = checkUserStatement.executeQuery();

                if (resultSet.next()) {

                    if (resultSet.getString("password").equals(password)) {
                        output.println("200;Logged in.");
                    } else {
                        output.println("403;Wrong password.");
                    }
                } else {
                    output.println("403;User doesn't exist in DB.");
                }
            } catch (SQLException e) {
                System.err.println("503;Login Worker ERROR. " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("503;Login Worker ERROR. " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("500;LoginWorker: Socket ERROR. " + e.getMessage());
            }
        }
    }
}