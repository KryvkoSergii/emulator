package ua.com.smiddle.emulator.core.pool;

import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ua.com.smiddle.emulator.core.model.Log;
import ua.com.smiddle.emulator.core.util.LoggerUtil;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by A.Osadchuk on 10.06.2016.
 * <br/>Компонент обертка для очереди {@link Log записей логов}.
 * @project SmiddleFinesseConnector
 */
@Component(value = "LogQueue")
@Scope("singleton")
@Description("\"Producer-Consumer\" queue implementation for storing logs in DB.")
public class LogQueue {
    private BlockingQueue<Log> queue = new LinkedBlockingQueue<>(100000);


    //Constructors
    public LogQueue() {
    }


    //Getters & setters
    public BlockingQueue<Log> getQueue() {
        return queue;
    }


    //Methods
    /**
     * Метод получает на вход запись лога в ООП представлении, подлежащую сохранению.
     * Запись помещается в очередь.
     * @param log
     * @return
     */
    public boolean offer(Log log) {
        return queue.offer(log);
    }

    /**
     * Метод возвращает запись из очереди.
     * @return
     */
    public Log take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            LoggerUtil.logAnywayStdOut("LogQueue", "take: throws InterruptedException=" + e.getMessage());
            return null;
        }
    }
}
