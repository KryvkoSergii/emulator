package ua.com.smiddle.emulator.core.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.com.smiddle.emulator.core.model.Log;
import ua.com.smiddle.emulator.core.pool.LogQueue;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

@Component("LoggerUtil")
@Scope("singleton")
@Description("Logger for persiste logs to stdout & Storage")
public class LoggerUtil {
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss.SSS", Locale.ENGLISH);
    @Autowired
    private Environment environment;
    @Autowired
    @Qualifier("LogQueue")
    private LogQueue queue;
    private volatile int debugLevel = 3;
    @Autowired
    @Qualifier("SettingsUtil")
    private SettingsUtil settingsUtil;


    //Constructor
    public LoggerUtil() {
    }


    //Methods
    public static void logAnywayStdOut(String componentName, String message) {
        System.out.println(new StringBuilder().append(LocalDateTime.now().format(formatter)).append(" MODULE: ").append("CTI-EMULATOR")
                .append(" COMPONENT: ").append(componentName).append(" MESSAGE: ").append(message).toString());
    }


    //Getters & setters
    public int getDebugLevel() {
        return debugLevel;
    }

    public void setDebugLevel(int debugLevel) {
        this.debugLevel = debugLevel;
    }

    public void logAnyway(String componentName, String message) {
        System.out.println(new StringBuilder().append(LocalDateTime.now().format(formatter)).append(" MODULE: ").append(environment.getProperty("module.name"))
                .append(" COMPONENT: ").append(componentName).append(" MESSAGE: ").append(message).toString());
        queue.offer(new Log(new Date(), componentName, message));
    }

    public void logMore_0(String componentName, String message) {
        if (debugLevel > 0) {
            System.out.println(new StringBuilder().append(LocalDateTime.now().format(formatter)).append(" MODULE: ").append(environment.getProperty("module.name"))
                    .append(" COMPONENT: ").append(componentName).append(" MESSAGE: ").append(message).toString());
            queue.offer(new Log(new Date(), componentName, message));
        }
    }

    public void logMore_1(String componentName, String message) {
        if (debugLevel > 1) {
            System.out.println(new StringBuilder().append(LocalDateTime.now().format(formatter)).append(" MODULE: ").append(environment.getProperty("module.name"))
                    .append(" COMPONENT: ").append(componentName).append(" MESSAGE: ").append(message).toString());
            queue.offer(new Log(new Date(), componentName, message));
        }
    }

    public void logMore_2(String componentName, String message) {
        if (debugLevel > 2) {
            System.out.println(new StringBuilder().append(LocalDateTime.now().format(formatter)).append(" MODULE: ").append(environment.getProperty("module.name"))
                    .append(" COMPONENT: ").append(componentName).append(" MESSAGE: ").append(message).toString());
            queue.offer(new Log(new Date(), componentName, message));
        }
    }

    /**
     * Обновление настроек (каждую 1 мин)
     */
    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 60 * 1000)
    private void updateSettings() {
        debugLevel = settingsUtil.getSettings().getDebugLevel();
    }

    //Methods
    @PostConstruct
    private void init() {
        debugLevel = settingsUtil.getSettings().getDebugLevel();
    }

}
