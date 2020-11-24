package com.arpan.log;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class LoggingThread extends Thread {

    private final Logger logger = Logger.getLogger("MyLog");

    private final LinkedBlockingQueue<String> logQueue;

    public LoggingThread(LinkedBlockingQueue<String> logQueue, String filename) {
        this.logQueue = logQueue;
        try {
            FileHandler fileHandler = new FileHandler(filename);
            logger.addHandler(fileHandler);

            LoggingFormatter formatter = new LoggingFormatter();
            fileHandler.setFormatter(formatter);

//            logger.setUseParentHandlers(false);   // Remove log to console

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startLogging() {

        while (true) {
            try {
                String logMessage = logQueue.take();
                logger.info(logMessage);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
