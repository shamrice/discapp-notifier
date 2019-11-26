package io.github.shamrice.discapp.notification.service.emailer;

import io.github.shamrice.discapp.notification.model.Configuration;
import io.github.shamrice.discapp.notification.repository.ConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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

    private static final String TIMEZONE_CONFIG_PROP_NAME = "timezone.location";

    @Value("${discapp.email.base-url}")
    protected String baseUrl;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ConfigurationRepository configurationRepository;

    public abstract void process();

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
                mimeMessageHelper.setSubject(emailNotificationMessage.getSubject());
                mimeMessageHelper.setText(emailNotificationMessage.getBody(), true);

                mailSender.send(mimeMessage);
                log.info("Sent " + emailNotificationMessage.toString());

            } catch (MessagingException mesgEx) {
                log.error("Failed to send message: " + emailNotificationMessage.toString() + " :: " + mesgEx.getMessage(), mesgEx);
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
