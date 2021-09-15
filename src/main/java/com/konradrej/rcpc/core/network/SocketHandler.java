package com.konradrej.rcpc.core.network;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Base handler implementation which allows for easy sending
 * and receiving of messages using the input and output queue.
 *
 * @author Konrad Rej
 * @author www.konradrej.com
 * @version 1.0
 */
public abstract class SocketHandler implements Runnable {
    private final Logger LOGGER;

    protected final Socket socket;
    protected final BlockingQueue<Message> inputQueue = new ArrayBlockingQueue<>(16);
    protected final BlockingQueue<Message> outputQueue = new ArrayBlockingQueue<>(16);

    protected Reader reader = null;
    protected Writer writer = null;
    protected boolean disconnect = false;

    /**
     * Simplified constructor which enables both input and output.
     *
     * @param socket the connected socket
     */
    public SocketHandler(Socket socket, Logger LOGGER) {
        this(socket, true, true, LOGGER);
    }

    /**
     * Constructor to customize input and output streams being disabled or not.
     *
     * @param socket        the connected socket
     * @param inputEnabled  whether to enable input
     * @param outputEnabled whether to enable output
     */
    public SocketHandler(Socket socket, boolean inputEnabled, boolean outputEnabled, Logger LOGGER) {
        this.socket = socket;
        this.LOGGER = LOGGER;

        if (inputEnabled) {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(this.socket.getInputStream());
                reader = new Reader(objectInputStream);
                Thread readerThread = new Thread(reader);
                readerThread.start();
            } catch (IOException e) {
                LOGGER.error("Could not construct inputstream: " + e.getLocalizedMessage());
            }
        }

        if (outputEnabled) {
            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
                writer = new Writer(objectOutputStream);
                Thread writerThread = new Thread(writer);
                writerThread.start();
            } catch (IOException e) {
                LOGGER.error("Could not construct outputstream: " + e.getLocalizedMessage());
            }
        }
    }

    @Override
    public abstract void run();

    /**
     * Disconnects the current socket.
     */
    public void disconnect() {
        disconnect = true;

        try {
            socket.close();
        } catch (IOException ignored) {
        }

        LOGGER.info("Socket disconnected.");
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
                LOGGER.error("Error while getting message: " + e.getLocalizedMessage());
            }

            disconnect();
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
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Error while sending message: " + e.getLocalizedMessage());
            }
        }
    }
}
