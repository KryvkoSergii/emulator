package ua.com.smiddle.emulator.core.services.prototype.bean;

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
        input = new LinkedBlockingQueue<>();
        output = new LinkedBlockingQueue<>();
    }


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
                } catch (IOException e) {
                    logger.logAnyway(module, "port:" + port + ":" + "run: read messages for=" + socket.getRemoteSocketAddress() + " throw Exception=" + e.getMessage());
                    errorCount++;
                }
            }
            isDone = true;
        } catch (IOException e) {
            logger.logAnyway(module, e.getMessage());
        }
        destroyBean();
    }

    private void read(InputStream is, byte[] length) throws IOException {
        for (byte i = 0; i < length.length; i++)
            length[i] = (byte) is.read();
        int l = ByteBuffer.wrap(length).getInt();
        if (l > 5000) {
            logger.logAnyway(module, "Buffer size = " + l);
            destroyBean();
            return;
        }
        byte[] messagePart = new byte[l + 8];

        if (true) {
            int read;
            int offset = 4;
            int len = messagePart.length - 4;
            for (read = 0; read < (l + 4 - 1); ) {
                offset = read + offset;
                len = len - read;
                read = is.read(messagePart, offset, len);
            }
            if (read != l + 4)
                logger.logAnyway(module, "length read=" + read + " should=" + (l + 4));
        } else {
            for (int i = 4; i < messagePart.length; i++)
                messagePart[i] = (byte) is.read();
        }

        System.arraycopy(length, 0, messagePart, 0, length.length);
//            ByteBuffer buffer = ByteBuffer.allocate(length.length + messagePart.length);
//            buffer.put(length).put(messagePart);
//            byte[] message = buffer.array();
//            input.add(message);
        input.add(messagePart);
        lastIncommingMessage = System.currentTimeMillis();
        if (logger.getDebugLevel() > 2)
            logger.logMore_2(module, "port:" + port + ":" + "RECEIVED:" + Arrays.toString(messagePart));
    }

    private void write(OutputStream os) throws IOException, InterruptedException {
        byte[] b = output.take();
        if (b != null) {
            os.write(b);
            os.flush();
            if (logger.getDebugLevel() > 2)
                logger.logMore_2(module, currentThread().getName() + ":" + "WROTE:" + Arrays.toString(b));
        }
    }

    @PreDestroy
    public void destroyBean() {
        logger.logAnyway(module, "port:" + port + ":" + "Shutting down...");
        interrupt();
        sender.interrupt();
        try {
            if (socket != null && !socket.isClosed()) {
                if (socket.getInputStream() != null)
                    socket.getInputStream().close();
                if (socket.getOutputStream() != null)
                    socket.getOutputStream().close();
                socket.close();
            }
        } catch (IOException e) {
            logger.logAnyway(module, "port:" + port + ":" + "destroyBean: closing throw Exception=" + e.getMessage());
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
                    if (outputStream == null) break;
                    write(outputStream);
                } catch (Exception e) {
                    errorCount++;
                    logger.logAnyway(module, "port:" + port + ":" + "Sender.run: throw Exception=" + e.getMessage());
                }
            }

            while (!output.isEmpty() && errorCount <= 5) {
                try {
                    if (outputStream == null) break;
                    write(socket.getOutputStream());
                } catch (Exception e) {
                    errorCount++;
                    logger.logAnyway(module, "port:" + port + ":" + "Sender.run: writing messages to socket throw Exception=" + e.getMessage());
                }
            }
        }
    }

}
