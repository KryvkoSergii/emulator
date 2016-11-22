package ua.com.smiddle.emulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ua.com.smiddle.emulator.core.services.Processor;
import ua.com.smiddle.emulator.core.services.Transport;

import java.util.concurrent.Executor;

/**
 * Created by srg on 14.09.16.
 */
@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "ua.com.smiddle.emulator.core")
@PropertySource("classpath:application.properties")
public class Application {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
    }

    @Bean(name = "threadPoolTransfer")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("EventSenderThread-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "Transport")
    @Scope(value = "prototype")
    public Transport getTransport() {
       return new Transport();
    }

    @Bean(name = "Processor")
    @Scope(value = "prototype")
    public Processor processor() {
        return new Processor();
    }

}
