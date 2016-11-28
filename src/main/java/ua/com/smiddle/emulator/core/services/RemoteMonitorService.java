package ua.com.smiddle.emulator.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.com.smiddle.emulator.core.model.remorte.monitoring.RemoteMonitorDescriptor;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author ksa on 28.11.16.
 * @project emulator
 */
@Component
public class RemoteMonitorService extends Thread {
    private List<RemoteMonitorDescriptor> connections = new CopyOnWriteArrayList<>();
    private final String module = "RemoteMonitorService";
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;
    @Autowired
    @Qualifier("Pools")
    private Pools pool;


    //Methods
    @PostConstruct
    private void init() {
        logger.logAnyway(module, "initialized");
        start();
    }

    @Override
    public void run() {
        try {
            ServerSocket ss = new ServerSocket(10505);
            Socket s;
            while (!isInterrupted()) {
                s = ss.accept();
                connections.add(new RemoteMonitorDescriptor(s));
                logger.logMore_0(module, "accepted=" + s.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Scheduled(initialDelay = 5 * 1000, fixedDelay = 5 * 1000)
    private void getPoolsState() {
        if (!connections.isEmpty()) {
            connections.forEach(monitor -> {
                try {
                    monitor.write(pool.getAgentMapping().values());
                    logger.logMore_2(module, "wrote to " + monitor.getSocket().getRemoteSocketAddress() + "=" + pool.getAgentMapping().values());
                } catch (IOException e) {
                    connections.remove(monitor);
                    e.printStackTrace();
                }
            });
        } else System.out.println("empty...");
    }


}
