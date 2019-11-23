package io.github.shamrice.discapp.notification.service;

import io.github.shamrice.discapp.notification.model.ApplicationSubscription;
import io.github.shamrice.discapp.notification.repository.ApplicationSubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ApplicationSubscriptionNotificationService extends NotificationService {

    @Autowired
    private ApplicationSubscriptionRepository applicationSubscriptionRepository;

    public void getSubscribersOfApplication(long applicationId) {
        List<ApplicationSubscription> subscriptions = applicationSubscriptionRepository.findByApplicationIdAndEnabled(applicationId, true);

        log.info("Subscribers to application id: " + applicationId);
        for (ApplicationSubscription subscription : subscriptions) {
            log.info(subscription.toString());
        }
    }
}
