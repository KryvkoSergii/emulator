package ua.com.smiddle.emulator.core.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.com.smiddle.emulator.core.model.Log;
import ua.com.smiddle.emulator.core.util.LoggerUtil;
import ua.com.smiddle.emulator.core.util.SettingsUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * @project emulator
 * @author srg on 22.09.16.
 */
@Service("FilePersister")
@Scope("singleton")
@Description("Implementation of LogPersister")
public class LogFilePersister implements LogPersister {
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss.SSS", Locale.ENGLISH);
    @Autowired
    private Environment environment;
    @Autowired
    @Qualifier("SettingsUtil")
    private SettingsUtil settingsUtil;
    private File file;
    private String fileAbsolutePath;
    private RandomAccessFile raFile;
    private Date currentDate;
    private static final String fileNamePrefix = "cti-emulator";
    private String newPath;


    @PostConstruct
    private void init() {
        currentDate = new Date();
        newPath = buildNewFilePath();
        findOrCreateFile();
    }

    @Scheduled(cron = "0/30 * * * * *")
    private void updateCurrentDate() {
        currentDate = new Date();
        newPath = buildNewFilePath();
    }

    /**
     * находит старый файл по имени или создает новый.
     */
    private void findOrCreateFile() {
        if (fileAbsolutePath == null || !newPath.equals(fileAbsolutePath) || file == null || !file.exists()) {
            fileAbsolutePath = newPath;
            try {
                //закрыть файл со старым именем
                if (raFile != null) destroy();
                //создать новый файл
                file = new File(fileAbsolutePath);
                raFile = new RandomAccessFile(file, "rw");
            } catch (Exception e) {
                LoggerUtil.logAnywayStdOut("LogQueue", "take: throws InterruptedException=" + e.getMessage());
            }
        }
    }

    @Override
    public void persist(Log log) {
        try {
            //проверка на существование файла (удален ли) и обновления даты (изменения названия файла)
            if (!file.exists() || !newPath.equals(fileAbsolutePath)) findOrCreateFile();
            raFile.seek(file.length());
            String m = new StringBuilder()
                    .append(LocalDateTime.now().format(formatter))
                    .append(" MODULE: ").append(environment.getProperty("module.name"))
                    .append(" COMPONENT: ").append(log.getModuleName())
                    .append(" MESSAGE: ").append(log.getMessage())
                    .append('\n').toString();
            raFile.write(m.getBytes());
        } catch (IOException e) {
            LoggerUtil.logAnywayStdOut("LogQueue", "take: throws InterruptedException=" + e.getMessage());
        }
    }

    @PreDestroy
    private void destroy() {
        try {
            raFile.close();
        } catch (IOException e) {
            LoggerUtil.logAnywayStdOut("LogQueue", "take: throws InterruptedException=" + e.getMessage());
        }
    }

    private String buildNewFilePath() {
        String loggerpath = settingsUtil.getSettings().getLoggerPath();
        return loggerpath.concat("//").concat(fileNamePrefix).concat(dateFormat.format(currentDate)).concat(".txt");
    }
}
