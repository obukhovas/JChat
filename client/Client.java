package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by Alexander on 16-Mar-17.
 */
public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public class SocketThread extends Thread {

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " join to chat");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " leave from chat");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message incomeMessage = connection.receive();
                if (incomeMessage.getType() == MessageType.NAME_REQUEST) {
                    String userName = getUserName();
                    Message userNameMessage = new Message(MessageType.USER_NAME, userName);
                    connection.send(userNameMessage);
                } else if (incomeMessage.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message incomeMessage = connection.receive();
                if (incomeMessage.getType() == MessageType.TEXT) {
                    processIncomingMessage(incomeMessage.getData());
                } else if (incomeMessage.getType() == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(incomeMessage.getData());
                } else if (incomeMessage.getType() == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(incomeMessage.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        public void run() {
            String serverAddress = getServerAddress();
            int serverPort = getServerPort();
            try {
                Socket socket = new Socket(serverAddress, serverPort);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }
    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Input server IP-address:");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Input server port number:");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Input user name:");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Error during sending message");
            clientConnected = false;
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Error. Exit from program..");
                return;
            }
        }
        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду ‘exit’.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
        String message;
        while (clientConnected) {
            message = ConsoleHelper.readString();
            if (message.equals("exit")) {
                break;
            }
            if (shouldSendTextFromConsole()) {
                sendTextMessage(message);
            }
        }
    }

    public static void main(String[] args) {
        new Client().run();
    }
}
