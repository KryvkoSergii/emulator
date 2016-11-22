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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author srg on 09.11.16.
 * @project emulator
 */
public class Transport extends Thread {
    private final String module = "Transport";
    private Queue<byte[]> input;
    private Queue<byte[]> output;
    private Socket socket;
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;


    //Constructors
    public Transport() {
        input = new ConcurrentLinkedQueue<>();
        output = new ConcurrentLinkedQueue<>();
    }

    public Transport(Socket socket) {
        input = new ConcurrentLinkedQueue<>();
        output = new ConcurrentLinkedQueue<>();
        this.socket = socket;
        start();
    }


    //Getters and setters
    public Queue<byte[]> getInput() {
        return input;
    }

    public void setInput(Queue<byte[]> input) {
        this.input = input;
    }

    public Queue<byte[]> getOutput() {
        return output;
    }

    public void setOutput(Queue<byte[]> output) {
        this.output = output;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }


    //Methods
    @PostConstruct
    private void init() {
        logger.logAnyway(module, "initialized...");
    }

    @Override
    public void run() {
        logger.logAnyway(module, "started");
        try (InputStream is = socket.getInputStream();
             OutputStream os = socket.getOutputStream()) {
            byte[] length = new byte[4];
            while (!isInterrupted()) {
                read(is, length);
                write(os);
            }
        } catch (IOException e) {
            logger.logAnyway(module, e.getMessage());
        }
    }

    private void read(InputStream is, byte[] length) throws IOException {
        if (is.available() > 0) {
            is.read(length);
            byte[] messagePart = new byte[ByteBuffer.wrap(length).getInt() + 4];
            is.read(messagePart);
            ByteBuffer buffer = ByteBuffer.allocate(length.length + messagePart.length);
            buffer.put(length).put(messagePart);
            byte[] message = buffer.array();
            input.add(message);
            logger.logMore_2(module, "RECEIVED:" + Arrays.toString(message));
        } else return;
    }

    private void write(OutputStream os) throws IOException {
        byte[] b = output.poll();
        if (b != null) {
            os.write(b);
            os.flush();
            logger.logMore_2(module, "WROTE:" + Arrays.toString(b));
        }
    }

    @PreDestroy
    public void destroy() {
        logger.logAnyway(module, "Shutting down...");
        interrupt();
        while (!output.isEmpty()) {
            try {
                write(socket.getOutputStream());
            } catch (IOException e) {
                logger.logAnyway("module", "destroy: throw Exception=" + e.getMessage());
            }
        }
        try {
            socket.getInputStream().close();
            socket.getOutputStream().close();
            socket.close();
        } catch (IOException e) {
            logger.logAnyway("module", "destroy: throw Exception=" + e.getMessage());
        }
    }


}
