package io.github.shamrice.discapp.notification.service.emailer;

import io.github.shamrice.discapp.notification.model.Application;
import io.github.shamrice.discapp.notification.model.ApplicationSubscription;
import io.github.shamrice.discapp.notification.model.Thread;
import io.github.shamrice.discapp.notification.repository.ApplicationRepository;
import io.github.shamrice.discapp.notification.repository.ApplicationSubscriptionRepository;
import io.github.shamrice.discapp.notification.repository.ThreadRepository;
import io.github.shamrice.discapp.notification.service.emailer.EmailNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@Qualifier("applicationSubscriptionEmailNotificationService")
public class ApplicationSubscriptionEmailNotificationService extends EmailNotificationService {

    @Value("${discapp.email.unsubscribe-url}")
    private String unsubscribeUrl;

    @Value("${discapp.email.daily.type}")
    private String emailType;

    @Value("${discapp.email.daily.subject}")
    private String subjectTemplate;

    @Value("${discapp.email.daily.thread-link-url}")
    private String threadLinkUrlTemplate;

    @Value("${discapp.email.daily.send-hour}")
    private int sendHour;

    @Autowired
    private ApplicationSubscriptionRepository applicationSubscriptionRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private boolean isProcessed = false;

    public int getSendHour() {
        //reset isProcessed if not the current send hour... sort of gross but can be fixed later.
        Calendar calendar = GregorianCalendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        if (currentHour != sendHour) {
            isProcessed = false;
        }

        return sendHour;
    }

    @Override
    public void process() {

        if (isProcessed) {
            log.info("Email notification: " + emailType
                    + " has already been processed. Will sleep until next time window comes around at: " + sendHour);
            return;
        }

        Calendar calendar = Calendar.getInstance();

        Date endDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date startDate = calendar.getTime();

        List<EmailNotificationMessage> notificationMessages = new ArrayList<>();

        List<Application> currentApplications = applicationRepository.findByDeletedAndEnabled(false, true);

        for (Application application : currentApplications) {

            List<ApplicationSubscription> subscriptions = getSubscribersOfApplication(application.getId());
            if (subscriptions != null) {

                List<Thread> latestThreads = threadRepository.getThreadByApplicationIdAndDeletedAndIsApprovedAndCreateDtBetweenOrderByCreateDtAsc(
                        application.getId(), false, true, startDate, endDate);

                if (latestThreads != null && !latestThreads.isEmpty()) {

                    String emailBodyStart = "<HTML><BODY><BR>";
                    String threadEmailLinks = emailBodyStart;

                    log.info("Threads found: " + startDate + " -> " + endDate);
                    for (Thread thread : latestThreads) {
                        log.info(thread.toString());
                        String linkUrl = threadLinkUrlTemplate
                                .replace(THREAD_ID_PLACEHOLDER, thread.getId().toString())
                                .replace(APPLICATION_ID_PLACEHOLDER, application.getId().toString());

                        String adjustedCreateDate = getAdjustedDateStringForConfiguredTimeZone(application.getId(), thread.getCreateDt(), false);

                        threadEmailLinks += "<a href=\"" + baseUrl + linkUrl + "\">" + thread.getSubject() + "</a> - "
                                + thread.getSubmitter() + ", " + adjustedCreateDate + "<br>";
                    }
                    threadEmailLinks += "<br>";

                    for (ApplicationSubscription subscription : subscriptions) {

                        String unsubscribeLinkUrl = unsubscribeUrl
                                .replace(APPLICATION_ID_PLACEHOLDER, application.getId().toString())
                                .replace(EMAIL_PLACEHOLDER, subscription.getSubscriberEmail());

                        String emailBody = threadEmailLinks + "<p><a href=\"" + baseUrl + unsubscribeLinkUrl
                                + "\">Click here to unsubscribe.</a></p>--------------<br><a href=\"" + baseUrl
                                + "\"><b>Create your own free message board</b></a><br>";

                        EmailNotificationMessage message = new EmailNotificationMessage();
                        message.setType(emailType);
                        message.setSubject(subjectTemplate.replace(APPLICATION_NAME_PLACEHOLDER, application.getName()));
                        message.setBody(emailBody);
                        message.setTo(subscription.getSubscriberEmail());
                        message.setNotificationId(subscription.getId());

                        log.info("Adding notification message to be sent out: " + message.toString());
                        notificationMessages.add(message);
                    }
                }
            }
        }

        isProcessed = true;
        super.sendNotifications(notificationMessages);
    }

    @Override
    public void updateLastSendDate(long notificationId) {
        ApplicationSubscription subscription = applicationSubscriptionRepository.findById(notificationId).orElse(null);
        if (subscription != null) {
            subscription.setLastSendDt(new Date());
            applicationSubscriptionRepository.save(subscription);
            log.info("Updating last send date for subscription: " + subscription.toString() + " to now.");
        }
    }

    private List<ApplicationSubscription> getSubscribersOfApplication(long applicationId) {
        List<ApplicationSubscription> subscriptions = applicationSubscriptionRepository.findByApplicationIdAndEnabled(applicationId, true);

        if (subscriptions == null || subscriptions.isEmpty()) {
            log.info("No subscribers to application id: " + applicationId + " to process. Returning.");
            return null;
        }
        return subscriptions;
    }

}
