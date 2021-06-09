package io.github.shamrice.discapp.notification.service.emailer;

import io.github.shamrice.discapp.notification.model.Configuration;
import io.github.shamrice.discapp.notification.repository.ConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Slf4j
public abstract class EmailNotificationService {

    protected static final String APPLICATION_NAME_PLACEHOLDER = "APPLICATION_NAME";
    protected static final String APPLICATION_ID_PLACEHOLDER = "APPLICATION_ID";
    protected static final String THREAD_ID_PLACEHOLDER = "THREAD_ID";
    protected static final String EMAIL_PLACEHOLDER = "EMAIL_ADDRESS";
    protected static final String OWNER_EMAIL_ADDRESS_PLACEHOLDER = "OWNER_EMAIL_ADDRESS";
    protected static final String GENERATED_AUTH_CODE_PLACEHOLDER = "GENERATED_AUTH_CODE";
    protected static final String REPORT_FREQUENCY_URL_PLACEHOLDER = "REPORT_FREQUENCY_URL";
    protected static final String MAINTENANCE_THREADS_UNAPPROVED_URL_PLACEHOLDER = "MAINTENANCE_THREADS_UNAPPROVED_URL";
    protected static final String TOTAL_UNAPPROVED_MESSAGES_PLACEHOLDER = "TOTAL_UNAPPROVED_MESSAGES";
    protected static final String MAINTENANCE_STATS_URL_PLACEHOLDER = "MAINTENANCE_STATS_URL";
    protected static final String TOTAL_LAST_MONTH_VISITORS_PLACEHOLDER = "TOTAL_LAST_MONTH_VISITORS";
    protected static final String MAINTENANCE_SUBSCRIBERS_URL_PLACEHOLDER = "MAINTENANCE_SUBSCRIBERS_URL";
    protected static final String LAST_SUBSCRIPTION_DATE_PLACEHOLDER = "LAST_SUBSCRIPTION_DATE";
    protected static final String TOTAL_SUBSCRIBERS_PLACEHOLDER = "TOTAL_SUBSCRIBERS";
    protected static final String MAINTENANCE_THREADS_URL_PLACEHOLDER = "MAINTENANCE_THREADS_URL";
    protected static final String LAST_THREAD_CREATION_PLACEHOLDER = "LAST_THREAD_CREATION";
    protected static final String TOTAL_THREADS_PLACEHOLDER = "TOTAL_THREADS";
    protected static final String MAINTENANCE_URL_PLACEHOLDER = "MAINTENANCE_URL";
    protected static final String BASE_SITE_URL_PLACEHOLDER = "BASE_SITE_URL";
    protected static final String HELP_FORUM_URL_PLACEHOLDER = "HELP_FORUM_URL";
    protected static final String MODIFY_ACCOUNT_URL_PLACEHOLDER = "MODIFY_ACCOUNT_URL";

    private static final String TIMEZONE_CONFIG_PROP_NAME = "timezone.location";

    @Value("${discapp.email.base-url}")
    protected String baseUrl;

    @Value("${discapp.email.throttling.enabled}")
    private boolean isEmailThrottlingEnabled;

    @Value("${discapp.email.throttling.delay}")
    private long emailThrottlingDelay;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    protected ConfigurationRepository configurationRepository;

    public abstract void process();

    public abstract void updateLastSendDate(long notificationId);

    void sendNotifications(List<EmailNotificationMessage> emailNotificationMessages) {

        for (EmailNotificationMessage emailNotificationMessage : emailNotificationMessages) {

            if (emailNotificationMessage.getSubject() == null || emailNotificationMessage.getBody() == null) {
                log.error("Failed to find subject or message body for notification type: " + emailNotificationMessage.getType()
                        + " :: message not sent.");
                continue;
            }

            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");
                mimeMessageHelper.setTo(emailNotificationMessage.getTo());
                mimeMessageHelper.setFrom(fromAddress);
                mimeMessageHelper.setReplyTo(fromAddress);
                mimeMessageHelper.setSubject(emailNotificationMessage.getSubject());
                mimeMessageHelper.setText(emailNotificationMessage.getBody(), true);

                mailSender.send(mimeMessage);
                log.info("Sent " + emailNotificationMessage);
                updateLastSendDate(emailNotificationMessage.getNotificationId());

            } catch (MessagingException mesgEx) {
                log.error("Failed to send message: " + emailNotificationMessage + " :: " + mesgEx.getMessage(), mesgEx);
            }

            if (isEmailThrottlingEnabled) {
                try {
                    log.info("Email throttling enabled. Sleeping thread for: " + emailThrottlingDelay);
                    Thread.sleep(emailThrottlingDelay);
                } catch (InterruptedException ex) {
                    log.error("Error sleeping email send thread for throttling.", ex);
                }
            }

        }
    }

    protected String getAdjustedDateStringForConfiguredTimeZone(long appId, Date date, boolean includeComma) {

        String dateFormatPattern = "EEE MMM dd, h:mma";
        String timeZoneLocation = "UTC";
        Configuration timeZoneConfig = configurationRepository.findByApplicationIdAndName(appId, TIMEZONE_CONFIG_PROP_NAME);
        if (timeZoneConfig != null && !timeZoneConfig.getValue().isEmpty()) {
            timeZoneLocation = timeZoneConfig.getValue();
        }

        log.info("Using timezone: " + timeZoneLocation + " for appId: " + appId);

        DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneLocation));

        //am and pm should be lowercase.
        String formattedString = dateFormat.format(date).replace("AM", "am").replace("PM", "pm");

        if (!includeComma) {
            formattedString = formattedString.replace(",", "");
        }

        return formattedString;
    }

}
