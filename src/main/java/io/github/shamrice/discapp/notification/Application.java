package io.github.shamrice.discapp.notification;

import io.github.shamrice.discapp.notification.service.ServiceRunner;
import lombok.var;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        var app = SpringApplication.run(Application.class, args);
        var serviceRunner =  app.getBean(ServiceRunner.class);
        serviceRunner.run();
    }
}
