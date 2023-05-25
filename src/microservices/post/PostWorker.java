package microservices.post;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostWorker implements Runnable {
    private final Socket clientSocket;

    public PostWorker(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = input.readLine();
            String[] requestData = request.split(";");
            String requestType = requestData[0];

            if (requestType.equals("post")) {

                try (Connection connection = DatabaseConnection.getConnection()) {
                    String username = requestData[1];
                    String postData = requestData[2];
                    PreparedStatement getUserIdStatement = connection.prepareStatement("SELECT id FROM users WHERE username = ?");
                    getUserIdStatement.setString(1, username);
                    ResultSet resultSet = getUserIdStatement.executeQuery();

                    if (!resultSet.next()) {
                        output.println("404;User doesn't exist in DB.");
                        output.flush();
                        return;
                    }
                    int userId = resultSet.getInt("id");

                    PreparedStatement insertPostStatement = connection.prepareStatement("INSERT INTO posts (user, content) VALUES (?, ?)");
                    insertPostStatement.setInt(1, userId);
                    insertPostStatement.setString(2, postData);
                    insertPostStatement.executeUpdate(); //jest 3:54 w nocy i przeraża mnie słowo execute tutaj

                    output.println("200;Post successfully added.");
                    output.flush();
                } catch (SQLException e) {
                    System.err.println("503;Post Worker ERROR." + e.getMessage());
                }
            } else if (requestType.equals("get_posts")) {
                try (Connection connection = DatabaseConnection.getConnection()) {
                    PreparedStatement returnPostStatement = connection.prepareStatement("SELECT * FROM posts ORDER BY ID DESC LIMIT 10");
                    ResultSet resultSet = returnPostStatement.executeQuery();

                    StringBuilder posts = new StringBuilder();
                    while (resultSet.next()) {
                        String content = resultSet.getString("content");
                        int userId = resultSet.getInt("user");
                        String tstamp = resultSet.getString("tstamp");

                        PreparedStatement getUsernameStatement = connection.prepareStatement("SELECT username FROM users WHERE id = ?");
                        getUsernameStatement.setInt(1, userId);
                        ResultSet userResultSet = getUsernameStatement.executeQuery();

                        if (userResultSet.next()) {
                            String uname = userResultSet.getString("username");
                            posts.append("User ").append(uname).append("\s\sWrote:\t").append(content).append("\t%\tAdded: ").append(tstamp).append("\t%\t\t%\t");
                        }
                    }
                    output.print("299;" + posts);
                } catch (SQLException e) {
                    System.err.println("418;Post Worker ERROR." + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("503;Post Worker ERROR." + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("500;PostWorker: Socket ERROR." + e.getMessage());
            }
        }
    }
}