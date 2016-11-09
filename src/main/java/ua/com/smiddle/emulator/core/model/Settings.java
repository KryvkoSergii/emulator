package ua.com.smiddle.emulator.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.env.Environment;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Properties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@XmlRootElement
public class Settings implements Serializable {
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss");
    private volatile Long id;
    private volatile int debugLevel;
    private volatile String loggerPath;

    //Constructors
    public Settings() {
    }

    public Settings(Properties props) {
        setLoggerPath(props.getProperty("connector.logger.path"));
        setDebugLevel(Integer.valueOf(props.getProperty("connector.debuglevel")));
    }

    public static SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    //Getters & setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getDebugLevel() {
        return debugLevel;
    }

    public void setDebugLevel(int debugLevel) {
        this.debugLevel = debugLevel;
    }

    public String getLoggerPath() {
        return loggerPath;
    }

    public void setLoggerPath(String loggerPath) {
        this.loggerPath = loggerPath;
    }

    //Methods
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Settings{");
        sb.append("id=").append(id);
        sb.append(", debugLevel=").append(debugLevel);
        sb.append(", loggerPath='").append(loggerPath).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public void fillFromEnviroment(Environment env) {
        setLoggerPath(env.getProperty("connector.logger.path"));
        setDebugLevel(Integer.valueOf(env.getProperty("connector.debuglevel")));
    }
}