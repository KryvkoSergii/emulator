package ua.com.smiddle.emulator.core.services;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author srg on 09.11.16.
 * @project emulator
 */
public class Transport extends Thread {
    private Queue<byte[]> input;
    private Queue<byte[]> output;
    private Socket socket;

    public Transport() {
        input = new ConcurrentLinkedQueue<>();
        output = new ConcurrentLinkedQueue<>();
    }

    public Transport(Socket socket) {
        input = new ConcurrentLinkedQueue<>();
        output = new ConcurrentLinkedQueue<>();
        this.socket = socket;
        this.start();
    }

    @Override
    public void run() {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (is != null && os != null) {

        }

    }

    @PreDestroy
    public void destroy() {

    }
}
