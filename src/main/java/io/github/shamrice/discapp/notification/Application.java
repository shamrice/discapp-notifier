package io.github.shamrice.discapp.notification;

import io.github.shamrice.discapp.notification.service.ServiceRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext app = SpringApplication.run(Application.class, args);
        ServiceRunner serviceRunner =  app.getBean(ServiceRunner.class);
        serviceRunner.run();
    }
}
