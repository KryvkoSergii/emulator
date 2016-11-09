package ua.com.smiddle.emulator.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * Added to ${PACKAGE_NAME} by A.Osadchuk on 10.05.2016 at 16:11.
 * Project: Manager
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Log implements Serializable {
    private Long id;
    private Date dateTime;
    private String moduleName;
    private String message;


    //Constructors
    public Log() {
    }

    public Log(Date dateTime, String moduleName, String message) {
        this.dateTime = dateTime;
        this.moduleName = moduleName;
        this.message = message;
    }


    //Getters & setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    //Methods
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Log{");
        sb.append("id=").append(id);
        sb.append(", dateTime=").append(dateTime);
        sb.append(", componentName='").append(moduleName).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
