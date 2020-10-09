package io.github.shamrice.discapp.notification.service.emailer;

import io.github.shamrice.discapp.notification.model.*;
import io.github.shamrice.discapp.notification.model.Thread;
import io.github.shamrice.discapp.notification.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;


@Service
@Slf4j
public class AdminReportEmailNotificationService extends EmailNotificationService {

    private static final String MAILING_LIST_SEND_FREQUENCY_CONFIG_KEY = "mailing.list.email.admin.frequency";
    private static final String MAILING_LIST_SUBJECT_TEMPLATE_CONFIG_KEY = "mailing.list.email.admin.subject.template";
    private static final String MAILING_LIST_BODY_TEMPLATE_CONFIG_KEY = "mailing.list.email.admin.body.template";

    private static final long GLOBAL_CONFIG_APP_ID = 0L;

    private static final String MONTHLY = "MONTHLY";
    private static final String WEEKLY = "WEEKLY";
    private static final String DAILY = "DAILY";
    private static final String NEVER = "NEVER";

    @Value("${discapp.email.admin.type}")
    private String emailType;

    @Value("${discapp.email.report-frequency.update.url}")
    private String reportFrequencyUpdateUrl;

    @Value("${discapp.email.modify-account.url}")
    private String modifyAccountUrl;

    @Value("${discapp.email.help-forum.url}")
    private String helpForumUrl;

    @Value("${discapp.email.maintenance.url}")
    private String maintenanceUrl;

    @Value("${discapp.email.maintenance.subscribers.url}")
    private String maintenanceSubscriberUrl;

    @Value("${discapp.email.maintenance.stats.url}")
    private String maintenanceStatusUrl;

    @Value("${discapp.email.maintenance.threads.url}")
    private String maintenanceThreadsUrl;

    @Value("${discapp.email.maintenance.threads.unapproved.url}")
    private String maintenanceThreadsUnapprovedUrl;

    @Value("${discapp.email.admin.send-hour}")
    private int sendHour;

    @Value("${discapp.email.admin.enabled}")
    private boolean isEnabled;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private ApplicationReportCodeRepository applicationReportCodeRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private ApplicationSubscriptionRepository subscriptionRepository;

    @Autowired
    private StatsRepository statsRepository;

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

        //stats are always one month of stats.
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.MONTH, -1);
        Date startDate = calendar.getTime();

        List<EmailNotificationMessage> notificationMessages = new ArrayList<>();
        List<Application> currentApplications = applicationRepository.findByDeletedAndEnabled(false, true);

        Configuration templateSubject = configurationRepository.findByApplicationIdAndName(GLOBAL_CONFIG_APP_ID, MAILING_LIST_SUBJECT_TEMPLATE_CONFIG_KEY);
        Configuration templateBody = configurationRepository.findByApplicationIdAndName(GLOBAL_CONFIG_APP_ID, MAILING_LIST_BODY_TEMPLATE_CONFIG_KEY);

        for (Application application : currentApplications) {

            Configuration mailingListFrequencyConfig = configurationRepository.findByApplicationIdAndName(application.getId(), MAILING_LIST_SEND_FREQUENCY_CONFIG_KEY);
            if (mailingListFrequencyConfig == null || mailingListFrequencyConfig.getValue() == null) {
                log.info("No mailing list configuration for report type: " + emailType + " for appId: " + application.getId() + " :: skipping.");
                ;
                continue;
            }

            //check if correct day of week, month, etc or should be skipped.
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            if (NEVER.equalsIgnoreCase(mailingListFrequencyConfig.getValue())) {
                log.info("AppId: " + application.getId() + " has " + emailType + " set to NEVER. Skipping...");
                continue;
            } else if (WEEKLY.equalsIgnoreCase(mailingListFrequencyConfig.getValue())) {
                if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    log.info("AppId: " + application.getId() + " has " + emailType + " set to WEEKLY but not SUNDAY. Skipping...");
                    continue;
                }
            } else if (MONTHLY.equalsIgnoreCase(mailingListFrequencyConfig.getValue())) {

                if (cal.get(Calendar.DAY_OF_MONTH) != 1) {
                    log.info("AppId: " + application.getId() + " has " + emailType + " set to MONTHLY but not first day. Skipping...");
                    continue;
                }
            } //otherwise should be daily and should sent.

            Owner owner = ownerRepository.findById(application.getOwnerId()).orElse(null);
            if (owner != null && owner.getEmail() != null) {
                String email = owner.getEmail();

                String reportCode = UUID.randomUUID().toString();
                ApplicationReportCode code = applicationReportCodeRepository.findByApplicationIdAndEmail(application.getId(), email).orElse(null);
                if (code == null) {
                    code = new ApplicationReportCode();
                    code.setCreateDt(new Date());
                    code.setApplicationId(application.getId());
                    code.setEmail(email);
                }
                code.setModDt(new Date());
                code.setCode(reportCode);
                applicationReportCodeRepository.save(code);
                log.info("Setting and saving application report code: " + code.toString());

                EmailNotificationMessage message = new EmailNotificationMessage();
                message.setTo(email);
                message.setType(emailType);
                message.setNotificationId(owner.getId());

                String emailSubject = templateSubject.getValue();
                emailSubject = emailSubject.replace(APPLICATION_NAME_PLACEHOLDER, application.getName());
                message.setSubject(emailSubject);

                String emailBody = templateBody.getValue();
                emailBody = emailBody.replaceAll(BASE_SITE_URL_PLACEHOLDER, baseUrl);
                emailBody = emailBody.replaceAll(OWNER_EMAIL_ADDRESS_PLACEHOLDER, owner.getEmail());
                emailBody = emailBody.replaceAll(MODIFY_ACCOUNT_URL_PLACEHOLDER, baseUrl + modifyAccountUrl);
                emailBody = emailBody.replaceAll(HELP_FORUM_URL_PLACEHOLDER, baseUrl + helpForumUrl);
                emailBody = emailBody.replaceAll(APPLICATION_NAME_PLACEHOLDER, application.getName());
                emailBody = emailBody.replaceAll(APPLICATION_ID_PLACEHOLDER, application.getId().toString());
                emailBody = emailBody.replaceAll(GENERATED_AUTH_CODE_PLACEHOLDER, reportCode);
                emailBody = emailBody.replaceAll(MAINTENANCE_URL_PLACEHOLDER, baseUrl + maintenanceUrl + application.getId().toString());
                emailBody = emailBody.replaceAll(MAINTENANCE_THREADS_URL_PLACEHOLDER, baseUrl + maintenanceThreadsUrl + application.getId().toString());
                emailBody = emailBody.replaceAll(MAINTENANCE_SUBSCRIBERS_URL_PLACEHOLDER, baseUrl + maintenanceSubscriberUrl + application.getId().toString());
                emailBody = emailBody.replaceAll(MAINTENANCE_STATS_URL_PLACEHOLDER, baseUrl + maintenanceStatusUrl + application.getId().toString());
                emailBody = emailBody.replaceAll(MAINTENANCE_THREADS_UNAPPROVED_URL_PLACEHOLDER, baseUrl + maintenanceThreadsUnapprovedUrl + application.getId().toString());
                emailBody = emailBody.replaceAll(REPORT_FREQUENCY_URL_PLACEHOLDER, baseUrl + reportFrequencyUpdateUrl);

                //total threads.
                long totalThreads = threadRepository.countByApplicationIdAndIsApprovedAndDeleted(application.getId(), true, false);
                emailBody = emailBody.replaceAll(TOTAL_THREADS_PLACEHOLDER, String.valueOf(totalThreads));

                //latest thread
                Thread lastThread = threadRepository.findTopByApplicationIdAndDeletedOrderByCreateDtDesc(application.getId(), false);
                if (lastThread != null) {
                    String lastThreadStr = "";
                    long numDaysLastThread = ChronoUnit.DAYS.between(lastThread.getCreateDt().toInstant(), endDate.toInstant());
                    if (numDaysLastThread == 0) {
                        long numHoursLastThread = ChronoUnit.HOURS.between(lastThread.getCreateDt().toInstant(), endDate.toInstant());
                        if (numHoursLastThread == 0) {
                            long numMinLastThread = ChronoUnit.MINUTES.between(lastThread.getCreateDt().toInstant(), endDate.toInstant());
                            if (numMinLastThread == 0) {
                                long numSecLastThread = ChronoUnit.SECONDS.between(lastThread.getCreateDt().toInstant(), endDate.toInstant());
                                lastThreadStr = numSecLastThread + " seconds ago";
                            } else {
                                lastThreadStr = numMinLastThread + " minutes ago";
                            }
                        } else {
                            lastThreadStr = numHoursLastThread + " hours ago";
                        }
                    } else {
                        lastThreadStr = numDaysLastThread + " days ago";
                    }
                    emailBody = emailBody.replaceAll(LAST_THREAD_CREATION_PLACEHOLDER, lastThreadStr);
                } else {
                    emailBody = emailBody.replaceAll(LAST_THREAD_CREATION_PLACEHOLDER, "");
                }

                //total unapproved threads.
                long totalUnapprovedThreads = threadRepository.countByApplicationIdAndIsApprovedAndDeleted(application.getId(), false, false);
                if (totalUnapprovedThreads > 0) {
                    emailBody = emailBody.replaceAll(TOTAL_UNAPPROVED_MESSAGES_PLACEHOLDER, String.valueOf(totalUnapprovedThreads));
                } else {
                    emailBody = emailBody.replaceAll(TOTAL_UNAPPROVED_MESSAGES_PLACEHOLDER, "");
                }

                //total subscribers.
                long totalSubscribers = subscriptionRepository.countByApplicationIdAndEnabled(application.getId(), true);
                if (totalSubscribers > 0) {
                    emailBody = emailBody.replaceAll(TOTAL_SUBSCRIBERS_PLACEHOLDER, String.valueOf(totalSubscribers));
                } else {
                    emailBody = emailBody.replaceAll(TOTAL_SUBSCRIBERS_PLACEHOLDER, "");
                }

                //latest subscription
                ApplicationSubscription lastSubscription = subscriptionRepository.findTopByApplicationIdAndEnabledOrderByCreateDt(application.getId(), true);
                if (lastSubscription != null) {
                    String lastSubStr = "";
                    long numDaysLastSubscription = ChronoUnit.DAYS.between(lastSubscription.getCreateDt().toInstant(), endDate.toInstant());
                    if (numDaysLastSubscription == 0) {
                        long numHoursLastSubscription = ChronoUnit.HOURS.between(lastSubscription.getCreateDt().toInstant(), endDate.toInstant());
                        if (numHoursLastSubscription == 0) {
                            long numMinLastSubscription = ChronoUnit.MINUTES.between(lastSubscription.getCreateDt().toInstant(), endDate.toInstant());
                            if (numMinLastSubscription == 0) {
                                long numSecLastSubscription = ChronoUnit.SECONDS.between(lastSubscription.getCreateDt().toInstant(), endDate.toInstant());
                                lastSubStr = numSecLastSubscription + " seconds ago";
                            } else {
                                lastSubStr = numMinLastSubscription + " minutes ago";
                            }
                        } else {
                            lastSubStr = numHoursLastSubscription + " hours ago";
                        }
                    } else {
                        lastSubStr = numDaysLastSubscription + " days ago";
                    }

                    emailBody = emailBody.replaceAll(LAST_SUBSCRIPTION_DATE_PLACEHOLDER, lastSubStr);
                } else {
                    emailBody = emailBody.replaceAll(LAST_SUBSCRIPTION_DATE_PLACEHOLDER, "");
                }

                //stats
                float numPageViews = 0;
                List<Stats> monthStats = statsRepository.findByApplicationIdAndCreateDtBetween(application.getId(), startDate, endDate);
                for (Stats stat : monthStats) {
                    if (stat.getPageViews() != null) {
                        numPageViews += stat.getPageViews();
                    }
                }
                float days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                float avgPageViews = numPageViews / days;
                emailBody = emailBody.replaceAll(TOTAL_LAST_MONTH_VISITORS_PLACEHOLDER, String.format("%.2f", avgPageViews));

                message.setBody(emailBody);
                notificationMessages.add(message);
                log.info("Finished processing " + emailType + " for appId: " + application.getId());
            }
        }

        isProcessed = true;
        super.sendNotifications(notificationMessages);
    }

    @Override
    public void updateLastSendDate(long notificationId) {
        log.info("Email notification: " + emailType + " does not set last send date in DB. Last send date for "
                + notificationId + " is " + new Date().toString());
    }
}
