package ua.com.smiddle.emulator.core.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ua.com.smiddle.emulator.core.model.Settings;

import javax.annotation.PostConstruct;

@Component("SettingsUtil")
@Scope("singleton")
public class SettingsUtil {
    private final Settings settings = new Settings();
    @Autowired
    private Environment environment;
    @Autowired
    @Qualifier("LoggerUtil")
    private LoggerUtil logger;


    //Constructors
    public SettingsUtil() {
    }


    //Getter & setters
    public Settings getSettings() {
        return settings;
    }


    //Methods
    private void setSettings(Settings settings) {
        if (settings.getDebugLevel() != this.settings.getDebugLevel())
            this.settings.setDebugLevel(settings.getDebugLevel());
        if (settings.getLoggerPath() != null && !settings.getLoggerPath().isEmpty())
            this.settings.setLoggerPath(settings.getLoggerPath());

    }

    @PostConstruct
    private void setUP() {
        settings.fillFromEnviroment(environment);
        logger.setDebugLevel(settings.getDebugLevel());
        logger.logAnyway("SettingsUtil", "Initialized with" + settings);
    }

}
