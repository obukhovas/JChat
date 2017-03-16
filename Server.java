package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alexander on 15-Mar-17.
 */
public class Server {

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            Message nameRequest = new Message(MessageType.NAME_REQUEST);
            while (true) {
                connection.send(nameRequest);
                Message answer = connection.receive();
                if (answer.getType() == MessageType.USER_NAME) {
                    String userName = answer.getData();
                    if (!userName.isEmpty() && !connectionMap.containsKey(userName)) {
                        connectionMap.put(userName, connection);
                        connection.send(new Message(MessageType.NAME_ACCEPTED));
                        return userName;
                    }
                }
            }
        }

        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> user : connectionMap.entrySet()) {
                if (!user.getKey().equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, user.getKey()));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message clientMsg = connection.receive();
                if (clientMsg.getType() == MessageType.TEXT) {
                    Message sendMsq = new Message(MessageType.TEXT, userName + ": " + clientMsg.getData());
                    sendBroadcastMessage(sendMsq);
                } else {
                    ConsoleHelper.writeMessage("Error: incorrect message type");
                }
            }
        }

        public void run() {
            ConsoleHelper.writeMessage("New connection established with: " + socket.getRemoteSocketAddress());
            String newClientName = null;
            try (Connection connection = new Connection(socket)) {
                newClientName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, newClientName));
                sendListOfUsers(connection, newClientName);
                serverMainLoop(connection, newClientName);

            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Error during data exchange with remote server");
            }
            if (newClientName != null) {
                connectionMap.remove(newClientName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, newClientName));
            }
            ConsoleHelper.writeMessage(String.format("Connection with %s is closed", socket.getRemoteSocketAddress()));
        }
    }

    public static void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> user : connectionMap.entrySet()) {
            try {
                user.getValue().send(message);
            } catch (IOException e) {
                System.out.println("Can't sent message to " + user.getKey());
            }
        }
    }

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Input server port:");
        int port = ConsoleHelper.readInt();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running...");
            while (true) {
                Socket inputSocket = serverSocket.accept();
                Handler handler = new Handler(inputSocket);
                handler.start();
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage(e.getMessage());
        }
    }
}
