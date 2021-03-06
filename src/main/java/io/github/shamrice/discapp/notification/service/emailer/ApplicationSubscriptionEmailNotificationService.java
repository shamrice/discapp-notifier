package io.github.shamrice.discapp.notification.service.emailer;

import io.github.shamrice.discapp.notification.model.*;
import io.github.shamrice.discapp.notification.model.Thread;
import io.github.shamrice.discapp.notification.repository.ApplicationRepository;
import io.github.shamrice.discapp.notification.repository.ApplicationSubscriptionRepository;
import io.github.shamrice.discapp.notification.repository.ThreadBodyRepository;
import io.github.shamrice.discapp.notification.repository.ThreadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
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

    @Value("${discapp.email.daily.enabled}")
    private boolean isEnabled;

    @Value("${discapp.email.daily.preview.length.max:500}")
    private int maxPreviewLength;

    @Value("${discapp.email.daily.notification.text:}")
    private String notificationText;

    @Autowired
    private ApplicationSubscriptionRepository applicationSubscriptionRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private ThreadBodyRepository threadBodyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private static final String MAILING_LIST_TYPE_CONFIGURATION_KEY = "mailing.list.email.update.settings";
    private static final String MAILING_LIST_TYPE_ALL_MESSAGES = "all";
    private static final String MAILING_LIST_TYPE_ALL_PREVIEW = "allPreview";
    private static final String MAILING_LIST_TYPE_FIRST_ONLY = "first";
    private static final String MAILING_LIST_TYPE_FIRST_ONLY_WITH_PREVIEW = "preview";

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

        if (!isEnabled) {
            log.info("Email notification: " + emailType + " is not currently enabled. Skipping processing.");
            return;
        }

        if (isProcessed) {
            log.info("Email notification: " + emailType
                    + " has already been processed. Will sleep until next time window comes around at: " + sendHour);
            return;
        }

        Calendar calendar = Calendar.getInstance();

        Date endDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date startDate = calendar.getTime();

        log.info("Generating daily email subscriptions for: " + startDate + " -> " + endDate);

        List<EmailNotificationMessage> notificationMessages = new ArrayList<>();

        List<Application> currentApplications = applicationRepository.findByDeletedAndEnabled(false, true);

        for (Application application : currentApplications) {

            String mailingListType = MAILING_LIST_TYPE_ALL_MESSAGES;

            Configuration mailingListConfig = configurationRepository.findByApplicationIdAndName(application.getId(), MAILING_LIST_TYPE_CONFIGURATION_KEY);
            if (mailingListConfig != null) {
                mailingListType = mailingListConfig.getValue();
            }

            switch (mailingListType) {
                case MAILING_LIST_TYPE_ALL_MESSAGES -> notificationMessages.addAll(getAllDailyMessages(application, startDate, endDate));
                case MAILING_LIST_TYPE_ALL_PREVIEW -> notificationMessages.addAll(getAllDailyMessagesWithPreview(application, startDate, endDate));
                case MAILING_LIST_TYPE_FIRST_ONLY -> notificationMessages.addAll(getFirstDailyMessages(application, startDate, endDate));
                case MAILING_LIST_TYPE_FIRST_ONLY_WITH_PREVIEW -> notificationMessages.addAll(getFirstDailyMessagesWithPreview(application, startDate, endDate));
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

    private List<EmailNotificationMessage> getAllDailyMessages(Application application, Date startDate, Date endDate) {

        List<EmailNotificationMessage> notificationMessages = new ArrayList<>();

        List<ApplicationSubscription> subscriptions = getSubscribersOfApplication(application.getId());
        if (subscriptions != null) {

            List<Thread> latestThreads = threadRepository.getThreadByApplicationIdAndDeletedAndIsApprovedAndCreateDtBetweenOrderByCreateDtAsc(
                    application.getId(), false, true, startDate, endDate);

            if (latestThreads != null && !latestThreads.isEmpty()) {
                String threadEmailLinks = getMessagesWithNoPreviewEmailBody(application, latestThreads);
                notificationMessages = generateSubscriberEmailMessages(application, subscriptions, threadEmailLinks);
            }
        }
        return notificationMessages;
    }

    private List<EmailNotificationMessage> getAllDailyMessagesWithPreview(Application application, Date startDate, Date endDate) {

        List<EmailNotificationMessage> notificationMessages = new ArrayList<>();

        List<ApplicationSubscription> subscriptions = getSubscribersOfApplication(application.getId());
        if (subscriptions != null) {

            List<Thread> latestThreads = threadRepository.getThreadByApplicationIdAndDeletedAndIsApprovedAndCreateDtBetweenOrderByCreateDtAsc(
                    application.getId(), false, true, startDate, endDate);

            if (latestThreads != null && !latestThreads.isEmpty()) {
                String threadEmailLinks = getMessagesWithPreviewEmailBody(application, latestThreads);
                notificationMessages = generateSubscriberEmailMessages(application, subscriptions, threadEmailLinks);
            }
        }
        return notificationMessages;
    }


    private List<EmailNotificationMessage> getFirstDailyMessages(Application application, Date startDate, Date endDate) {

        List<EmailNotificationMessage> notificationMessages = new ArrayList<>();

        List<ApplicationSubscription> subscriptions = getSubscribersOfApplication(application.getId());
        if (subscriptions != null) {

            List<Thread> latestThreads = threadRepository.getThreadByApplicationIdAndDeletedAndIsApprovedAndParentIdAndCreateDtBetweenOrderByCreateDtAsc(
                    application.getId(), false, true, 0L, startDate, endDate);

            if (latestThreads != null && !latestThreads.isEmpty()) {
                String threadEmailLinks = getMessagesWithNoPreviewEmailBody(application, latestThreads);
                notificationMessages = generateSubscriberEmailMessages(application, subscriptions, threadEmailLinks);
            }
        }
        return notificationMessages;
    }

    private List<EmailNotificationMessage> getFirstDailyMessagesWithPreview(Application application, Date startDate, Date endDate) {

        List<EmailNotificationMessage> notificationMessages = new ArrayList<>();

        List<ApplicationSubscription> subscriptions = getSubscribersOfApplication(application.getId());
        if (subscriptions != null) {

            List<Thread> latestThreads = threadRepository.getThreadByApplicationIdAndDeletedAndIsApprovedAndParentIdAndCreateDtBetweenOrderByCreateDtAsc(
                    application.getId(), false, true, 0L, startDate, endDate);

            if (latestThreads != null && !latestThreads.isEmpty()) {
                String threadEmailLinks = getMessagesWithPreviewEmailBody(application, latestThreads);
                notificationMessages = generateSubscriberEmailMessages(application, subscriptions, threadEmailLinks);
            }
        }
        return notificationMessages;
    }

    private String getMessagesWithNoPreviewEmailBody(Application application, List<Thread> latestThreads) {
        StringBuilder threadEmailLinks = new StringBuilder("<HTML><BODY><BR>");

        for (Thread thread : latestThreads) {
            log.info(thread.toString());
            String linkUrl = threadLinkUrlTemplate
                    .replace(THREAD_ID_PLACEHOLDER, thread.getId().toString())
                    .replace(APPLICATION_ID_PLACEHOLDER, application.getId().toString());

            String adjustedCreateDate = getAdjustedDateStringForConfiguredTimeZone(application.getId(), thread.getCreateDt(), false);

            String plainSubject = removeHtmlTags(thread.getSubject());

            threadEmailLinks.append("<a href=\"").append(baseUrl).append(linkUrl).append("\">")
                    .append(plainSubject).append("</a> - ").append(thread.getSubmitter())
                    .append(", ").append(adjustedCreateDate).append("<br>");

            log.info("Adding new thread with no preview :: AppId: " + application.getId()
                    + " : Subject: " + plainSubject + " : Submitter: " + thread.getSubmitter());
        }
        threadEmailLinks.append("<br>");
        return threadEmailLinks.toString();
    }

    private String getMessagesWithPreviewEmailBody(Application application, List<Thread> latestThreads) {
        StringBuilder threadEmailLinks = new StringBuilder("<HTML><BODY><BR><TABLE>");

        for (Thread thread : latestThreads) {

            String threadBodyPreview = "";

            ThreadBody threadBody = threadBodyRepository.findByThreadId(thread.getId());

            if (threadBody != null && threadBody.getBody() != null) {

                threadBodyPreview = removeHtmlTags(threadBody.getBody());

                //shorten body if over max limit after html tags removed.
                if (threadBodyPreview.length() > maxPreviewLength) {
                    threadBodyPreview = threadBodyPreview.substring(0, maxPreviewLength) + "...";
                }
            }

            String linkUrl = threadLinkUrlTemplate
                    .replace(THREAD_ID_PLACEHOLDER, thread.getId().toString())
                    .replace(APPLICATION_ID_PLACEHOLDER, application.getId().toString());

            String adjustedCreateDate = getAdjustedDateStringForConfiguredTimeZone(application.getId(), thread.getCreateDt(), false);

            String plainSubject = removeHtmlTags(thread.getSubject());

            threadEmailLinks.append("<TR><TD style=\"background-color: #dde;\"><a href=\"").append(baseUrl)
                    .append(linkUrl).append("\">").append(plainSubject).append("</a> - ")
                    .append(thread.getSubmitter()).append(", ").append(adjustedCreateDate)
                    .append("</TD></TR>")
                    .append("<TR><TD style=\"border-bottom: solid; border-width: 1px;\"><p style=\"font-size:smaller;\">")
                    .append(threadBodyPreview).append("</p></TD></TR>");

            log.info("Adding new thread with preview ::  AppId: " + application.getId()
                    + " : Subject: " + plainSubject + " : Submitter: " + thread.getSubmitter()
                    + " : Body Preview: " + threadBodyPreview + " : Max preview length: " + maxPreviewLength);
        }
        threadEmailLinks.append("</TABLE><br>");

        return threadEmailLinks.toString();
    }

    private String removeHtmlTags(String input) {
        return input.replaceAll("<[^>]*>", " ")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;").trim();
    }

    private List<EmailNotificationMessage>  generateSubscriberEmailMessages(Application application, List<ApplicationSubscription> subscriptions, String emailBody) {
        List<EmailNotificationMessage> notificationMessages = new ArrayList<>();
        for (ApplicationSubscription subscription : subscriptions) {

            String urlEmail = UriUtils.encode(subscription.getSubscriberEmail(), StandardCharsets.UTF_8);

            String unsubscribeLinkUrl = unsubscribeUrl
                    .replace(APPLICATION_ID_PLACEHOLDER, application.getId().toString())
                    .replace(EMAIL_PLACEHOLDER, urlEmail);

            String finalEmailBody = emailBody + "<p><a href=\"" + baseUrl + unsubscribeLinkUrl
                    + "\">Click here to unsubscribe.</a></p>";

            if (notificationText != null && !notificationText.isBlank()) {
                finalEmailBody += "<p><b>Please note:</b> " + notificationText + "</p>";
            }

            finalEmailBody += "--------------<br><a href=\"" + baseUrl
                    + "\"><b>Create your own free message board</b></a><br></BODY></HTML>";

            EmailNotificationMessage message = new EmailNotificationMessage();
            message.setType(emailType);
            message.setSubject(subjectTemplate.replace(APPLICATION_NAME_PLACEHOLDER, application.getName()));
            message.setBody(finalEmailBody);
            message.setTo(subscription.getSubscriberEmail());
            message.setNotificationId(subscription.getId());

            log.info("Adding notification message to be sent out: " + message);
            notificationMessages.add(message);
        }
        return notificationMessages;
    }
}
