package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Alexander on 16-Mar-17.
 */
public class BotClient extends Client {

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            String[] splitedMessage = message.split(": ");
            if (splitedMessage.length == 2) {
                String senderName = splitedMessage[0];
                String senderText = splitedMessage[1];
                String dateFormat = null;
                if (senderText.equals("дата")) {
                    dateFormat = "d.MM.YYYY";
                } else if (senderText.equals("день")) {
                    dateFormat = "d";
                } else if (senderText.equals("месяц")) {
                    dateFormat = "MMMM";
                } else if (senderText.equals("год")) {
                    dateFormat = "YYYY";
                } else if (senderText.equals("время")) {
                    dateFormat = "H:mm:ss";
                } else if (senderText.equals("час")) {
                    dateFormat = "H";
                } else if (senderText.equals("минуты")) {
                    dateFormat = "m";
                } else if (senderText.equals("секунды")) {
                    dateFormat = "s";
                }
                if (dateFormat != null) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
                    sendTextMessage(String.format("Информация для %s: %s", senderName, simpleDateFormat.format(Calendar.getInstance().getTime())));
                }
            }

        }
    }

    @Override
    protected String getUserName() {
        return String.format("date_bot_%d", (int) (Math.random() * 100));
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    public static void main(String[] args) {
        new BotClient().run();
    }
}
