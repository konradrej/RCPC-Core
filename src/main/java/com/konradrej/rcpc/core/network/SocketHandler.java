package com.konradrej.rcpc.core.network;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Base handler implementation which allows for easy sending
 * and receiving of messages using the input and output queue.
 *
 * @author Konrad Rej
 * @author www.konradrej.com
 * @version 1.3
 * @since 1.0
 */
public abstract class SocketHandler implements Runnable {
    private final Logger LOGGER;

    protected final Socket socket;
    protected final BlockingQueue<Message> inputQueue = new LinkedBlockingQueue<>();
    protected final BlockingQueue<Message> outputQueue = new LinkedBlockingQueue<>();

    protected Reader reader = null;
    protected Writer writer = null;
    private Thread readerThread;
    private Thread writerThread;
    protected boolean disconnect = false;

    /**
     * Simplified constructor which enables both input and output.
     *
     * @param socket the connected socket
     * @param LOGGER logger to be used, can be null to disable
     * @since 1.0
     */
    public SocketHandler(Socket socket, Logger LOGGER) {
        this(socket, true, true, LOGGER, null, null);
    }

    /**
     * Simplified constructor which enables both input and output and passes existing object streams.
     *
     * @param socket             the connected socket
     * @param LOGGER             logger to be used, can be null to disable
     * @param objectOutputStream object output stream to pass, null to create from socket
     * @param objectInputStream  object input stream to pass, null to create from socket
     * @since 1.0
     */
    public SocketHandler(Socket socket, Logger LOGGER, ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) {
        this(socket, true, true, LOGGER, objectOutputStream, objectInputStream);
    }

    /**
     * Constructor to customize input and output streams being disabled or not as well as adds possibility to pass existing streams.
     *
     * @param socket             the connected socket
     * @param inputEnabled       whether to enable input
     * @param outputEnabled      whether to enable output
     * @param LOGGER             logger to be used, can be null to disable
     * @param objectOutputStream object output stream to pass, null to create from socket
     * @param objectInputStream  object input stream to pass, null to create from socket
     * @since 1.0
     */
    public SocketHandler(Socket socket, boolean inputEnabled, boolean outputEnabled, Logger LOGGER, ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) {
        this.socket = socket;
        this.LOGGER = LOGGER;

        if (outputEnabled) {
            if (objectOutputStream != null) {
                writer = new Writer(objectOutputStream);
            } else {
                try {
                    objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
                    writer = new Writer(objectOutputStream);
                } catch (IOException e) {
                    if (LOGGER != null) {
                        LOGGER.error("Could not construct outputstream: " + e.getLocalizedMessage());
                    }
                }
            }

            writerThread = new Thread(writer);
            writerThread.start();
        }

        if (inputEnabled) {
            if (objectInputStream != null) {
                reader = new Reader(objectInputStream);
            } else {
                try {
                    objectInputStream = new ObjectInputStream(this.socket.getInputStream());
                    reader = new Reader(objectInputStream);
                } catch (IOException e) {
                    if (LOGGER != null) {
                        LOGGER.error("Could not construct inputstream: " + e.getLocalizedMessage());
                    }
                }
            }

            readerThread = new Thread(reader);
            readerThread.start();
        }
    }

    @Override
    public abstract void run();

    /**
     * Disconnects the current socket.
     *
     * @since 1.0
     */
    public void disconnect() {
        disconnect = true;

        if (reader != null) {
            while (readerThread.isAlive()) {
            }
        }

        if (writer != null) {
            while (writerThread.isAlive()) {
            }
        }

        try {
            socket.close();
        } catch (IOException ignored) {
        }

        if (LOGGER != null) {
            LOGGER.info("Socket disconnected.");
        }
    }

    private class Reader implements Runnable {
        private final ObjectInputStream objectInputStream;

        public Reader(ObjectInputStream objectInputStream) {
            this.objectInputStream = objectInputStream;
        }

        @Override
        public void run() {
            try {
                while (!disconnect) {
                    Message message = (Message) objectInputStream.readObject();

                    inputQueue.add(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                if (LOGGER != null) {
                    LOGGER.error("Error while getting message: " + e.getLocalizedMessage());
                }
            }
        }
    }

    private class Writer implements Runnable {
        private final ObjectOutputStream objectOutputStream;

        public Writer(ObjectOutputStream objectOutputStream) {
            this.objectOutputStream = objectOutputStream;
        }

        @Override
        public void run() {
            try {
                while (!disconnect) {
                    if (outputQueue.size() > 0) {
                        Message message = outputQueue.take();

                        objectOutputStream.writeObject(message);
                    }
                }

                objectOutputStream.flush();
            } catch (IOException | InterruptedException e) {
                if (LOGGER != null) {
                    LOGGER.error("Error while sending message: " + e.getLocalizedMessage());
                }
            }
        }
    }
}
