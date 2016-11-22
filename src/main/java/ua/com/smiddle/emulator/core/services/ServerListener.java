package ua.com.smiddle.emulator.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author srg on 09.11.16.
 * @project emulator
 */
@Component("ServerListener")
@Scope("singleton")
@Description("Listener for incoming connection")
public class ServerListener extends Thread {
    private final String module = "ServerListener";
    @Autowired
    private Environment env;
    @Autowired
    private ApplicationContext context;
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;

    @PostConstruct
    private void init(){
        logger.logAnyway(module,"initialized...");
        start();
    }

    public ServerListener() {

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
        Processor pr = context.getBean(Processor.class);
        pr.setSocket(s);
        pr.start();
    }
}
