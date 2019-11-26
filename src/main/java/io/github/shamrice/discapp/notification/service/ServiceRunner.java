package io.github.shamrice.discapp.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.github.shamrice.discapp.notification.service.emailer.ApplicationSubscriptionEmailNotificationService;

import java.util.Calendar;
import java.util.GregorianCalendar;

@Slf4j
@Service
public class ServiceRunner {

    @Value("${discapp.service-runner.sleep-duration}")
    private long sleepDuration;

    @Autowired
    private ApplicationSubscriptionEmailNotificationService applicationSubscriptionNotificationService;

    public void run() {
        log.info("Service runner started. Using sleep duration of: " + sleepDuration);

        while (true) {

            Calendar calendar = GregorianCalendar.getInstance();
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            if (currentHour == applicationSubscriptionNotificationService.getSendHour()) {
                log.info("Send hour reached for processing of: " + applicationSubscriptionNotificationService.getClass().getSimpleName());
                applicationSubscriptionNotificationService.process();
                log.info("Finished processing notifications for: " + applicationSubscriptionNotificationService.getClass().getSimpleName());
            }

            try {
                log.info("Sleeping...");
                Thread.sleep(sleepDuration);
            } catch (InterruptedException ex) {
                log.error("Message: " + ex.getMessage(), ex);
            }
        }
    }
}
