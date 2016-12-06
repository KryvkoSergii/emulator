package ua.com.smiddle.emulator.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author srg on 09.11.16.
 * @project emulator
 */
public class Transport extends Thread {
    private static final String module = "Transport";
    private BlockingQueue<byte[]> input;
    private BlockingQueue<byte[]> output;
    private Socket socket;
    private int errorCount = 0;
    private boolean isDone;
    private int port = 0;
    private long lastIncommingMessage;
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    private Sender sender;


    //Constructors
    public Transport() {
//        input = new ConcurrentLinkedQueue<>();
//        output = new ConcurrentLinkedQueue<>();
        input = new LinkedBlockingQueue<>(1500);
        output = new LinkedBlockingQueue<>(1500);
    }

//    public Transport(Socket socket) {
//        input = new ConcurrentLinkedQueue<>();
//        output = new ConcurrentLinkedQueue<>();
//        this.socket = socket;
//        start();
//    }


    //Getters and setters
    public BlockingQueue<byte[]> getInput() {
        return input;
    }

    public void setInput(BlockingQueue<byte[]> input) {
        this.input = input;
    }

    public BlockingQueue<byte[]> getOutput() {
        return output;
    }

    public void setOutput(BlockingQueue<byte[]> output) {
        this.output = output;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public long getLastIncommingMessage() {
        return lastIncommingMessage;
    }


    //Methods
    @PostConstruct
    private void init() {
        logger.logAnyway(module, "initialized...");
    }

    @Override
    public void run() {
        logger.logAnyway(module, "started with " + socket.getRemoteSocketAddress());

        try (InputStream is = socket.getInputStream();
             OutputStream os = socket.getOutputStream()) {
            port = socket.getPort();
            byte[] length = new byte[4];
            sender = new Sender(os, "Transport.Sender.Thread:" + port);
            sender.start();
            while (!isInterrupted() && errorCount <= 5) {
                try {
                    read(is, length);
//                    write(os);
                } catch (IOException e) {
                    logger.logAnyway(module, currentThread().getName() + ":" + "run: write/read messages for=" + socket.getRemoteSocketAddress() + " throw Exception=" + e.getMessage());
                    errorCount++;
                }
            }
            isDone = true;
            destroyBean();
        } catch (IOException e) {
            logger.logAnyway(module, e.getMessage());
        }
    }

    private void read(InputStream is, byte[] length) throws IOException {
        if (is.available() > 0) {
            lastIncommingMessage = System.currentTimeMillis();
            is.read(length);
            byte[] messagePart = new byte[ByteBuffer.wrap(length).getInt() + 4];
            is.read(messagePart);
            ByteBuffer buffer = ByteBuffer.allocate(length.length + messagePart.length);
            buffer.put(length).put(messagePart);
            byte[] message = buffer.array();
            input.add(message);
            logger.logMore_2(module, currentThread().getName() + ":" + "RECEIVED:" + Arrays.toString(message));
        }
    }

    private void write(OutputStream os) throws IOException, InterruptedException {
        byte[] b = output.take();
        if (b != null) {
            os.write(b);
            os.flush();
            logger.logMore_2(module, currentThread().getName() + ":" + "WROTE:" + Arrays.toString(b));
        }
    }

    @PreDestroy
    public void destroyBean() {
        logger.logAnyway(module, "Shutting down...");
        interrupt();
        while (!output.isEmpty() && errorCount <= 5) {
            try {
                write(socket.getOutputStream());
            } catch (Exception e) {
                logger.logAnyway(module, currentThread().getName() + ":" + "destroyBean: writing messages to socket throw Exception=" + e.getMessage());
            }
        }
        sender.interrupt();
        try {
            socket.getInputStream().close();
            socket.getOutputStream().close();
            socket.close();
        } catch (IOException e) {
            logger.logAnyway(module, currentThread().getName() + ":" + "destroyBean: closing throw Exception=" + e.getMessage());
        }
    }

    class Sender extends Thread {
        private OutputStream outputStream;

        public Sender(OutputStream outputStream, String name) {
            setName(name);
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            while (!isInterrupted() && errorCount <= 5) {
                try {
                    write(outputStream);
                } catch (Exception e) {
                    errorCount++;
                    logger.logAnyway(module, currentThread().getName() + ":" + "Sender.run: throw Exception=" + e.getMessage());
                }
            }
        }
    }

}
