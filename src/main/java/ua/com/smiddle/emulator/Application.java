package ua.com.smiddle.emulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import ua.com.smiddle.emulator.core.model.ServerDescriptor;
import ua.com.smiddle.emulator.core.services.Transport;

import java.util.concurrent.Executor;

/**
 * @author srg on 14.09.16.
 * @project emulator
 */
@SpringBootApplication
@EnableWebMvc
@Configuration
@EnableAutoConfiguration
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = "ua.com.smiddle.emulator.core")
@PropertySource("classpath:application.properties")
public class Application extends WebMvcConfigurerAdapter {

    public static void main(String[] args) {
//        ApplicationContext ctx =
        SpringApplication.run(Application.class, args);
    }

    @Bean(name = "threadPoolSender")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("EventSenderThread-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "Transport", destroyMethod = "destroyBean")
    @Scope(value = "prototype")
    public Transport getTransport() {
        return new Transport();
    }

    @Bean(name = "ServerDescriptor")
    @Scope(value = "prototype")
    public ServerDescriptor clientConnection() {
        return new ServerDescriptor();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/recourses/**")
                .addResourceLocations("/recourses/");
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Bean
    @Description("Вспомагательный класс который указывает фреймворку откуда брать страницы для отображения")
    public ViewResolver setupViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/pages/");
        resolver.setSuffix(".jsp");
        resolver.setViewClass(JstlView.class);
        resolver.setOrder(1);
        return resolver;
//
//        UrlBasedViewResolver resolver = new UrlBasedViewResolver();
//        resolver.setPrefix("/WEB-INF/pages/");
//        resolver.setSuffix(".jsp");
//        resolver.setViewClass(JstlView.class);
//        resolver.setOrder(1);
//        return resolver;
    }
}
