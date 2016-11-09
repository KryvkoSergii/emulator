package ua.com.smiddle.emulator.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author srg on 09.11.16.
 * @project emulator
 */
@Component("Server")
public class Server extends Thread {
    @Autowired
    private Environment env;

    public Server() {
        this.start();
    }

    @Override
    public void run() {
        ServerSocket ss;
        try {
            ss = new ServerSocket(Integer.valueOf(env.getProperty("connection.listener.port")).intValue());
            while (!isInterrupted())
                acceptingConnection(ss.accept());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void acceptingConnection(Socket s) {

    }
}
