package ua.com.smiddle.emulator.core.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ua.com.smiddle.emulator.core.model.Log;
import ua.com.smiddle.emulator.core.pool.LogQueue;
import ua.com.smiddle.emulator.core.services.logger.LogPersister;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


@Component("LogQueueConsumer")
@Scope("singleton")
@Description("\"Producer-Consumer\" consumer implementation for storing logs in DB.")
public class LogQueueConsumer implements Runnable {
    @Autowired
    @Qualifier("LogQueue")
    private LogQueue queue;
    @Autowired
    @Qualifier("FilePersister")
    private LogPersister logPersister;

    private Thread thread;


    //Constructors
    public LogQueueConsumer() {
    }


    //Getters & setters
    public LogQueue getQueue() {
        return queue;
    }

    public void setQueue(LogQueue queue) {
        this.queue = queue;
    }


    //Methods
    @PostConstruct
    private void setUp() {
        thread = new Thread(this);
        thread.start();
    }

    @PreDestroy
    private void tierDown() {
        thread.interrupt();
    }


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Log log = queue.take();
            if (log == null) continue;
            try {
                logPersister.persist(log);
            } catch (Exception e) {
                LoggerUtil.logAnywayStdOut("LogQueueConsumer", "run: can't persist Log. Cause: " + e.getMessage());
                retryPersistLog(log);
            }
        }
    }

    private void retryPersistLog(Log log) {
        try {
            Thread.sleep(5 * 60 * 1000);
            logPersister.persist(log);
        } catch (Exception e) {
            LoggerUtil.logAnywayStdOut("LogQueueConsumer", "retryPersistLog: can't persist Log. Cause: " + e.getMessage());
            retryPersistLog(log);
        }
    }
}