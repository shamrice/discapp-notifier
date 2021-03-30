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


@Service
@Slf4j
public class AdminReportEmailNotificationService extends EmailNotificationService {

    private static final String MAILING_LIST_SEND_FREQUENCY_CONFIG_KEY = "mailing.list.email.admin.frequency";
    private static final String MAILING_LIST_SUBJECT_TEMPLATE_CONFIG_KEY = "mailing.list.email.admin.subject.template";
    private static final String MAILING_LIST_BODY_TEMPLATE_CONFIG_KEY = "mailing.list.email.admin.body.template";

    private static final String APPLICATION_REPORT_DATA_START_PLACEHOLDER = "APPLICATION_REPORT_DATA_START";
    private static final String APPLICATION_REPORT_DATA_END_PLACEHOLDER = "APPLICATION_REPORT_DATA_END";
    private static final String APPLICATION_REPORT_DATA_TEMP_PLACEHOLDER = "APPLICATION_REPORT_DATA_TEMP";

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

        Configuration templateSubject = configurationRepository.findByApplicationIdAndName(GLOBAL_CONFIG_APP_ID, MAILING_LIST_SUBJECT_TEMPLATE_CONFIG_KEY);
        Configuration templateBody = configurationRepository.findByApplicationIdAndName(GLOBAL_CONFIG_APP_ID, MAILING_LIST_BODY_TEMPLATE_CONFIG_KEY);

        String emailTemplateSubject = templateSubject.getValue();
        String emailTemplateBody = templateBody.getValue();

        //get individual app report section out of body template.
        String emailTemplateAppReport = emailTemplateBody.substring(
                emailTemplateBody.indexOf(APPLICATION_REPORT_DATA_START_PLACEHOLDER) + APPLICATION_REPORT_DATA_START_PLACEHOLDER.length(),
                emailTemplateBody.indexOf(APPLICATION_REPORT_DATA_END_PLACEHOLDER));

        //get email body before report section.
        String emailTemplateBodyStart = emailTemplateBody.substring(0, emailTemplateBody.indexOf(APPLICATION_REPORT_DATA_START_PLACEHOLDER));

        //get end of email after report section.
        String emailTemplateBodyEnd = emailTemplateBody.substring(
                emailTemplateBody.indexOf(APPLICATION_REPORT_DATA_END_PLACEHOLDER) + APPLICATION_REPORT_DATA_END_PLACEHOLDER.length());


        log.debug("admin email template subject: " + emailTemplateSubject);
        log.debug("admin email template body start: " + emailTemplateBodyStart);
        log.debug("admin email template app report: " + emailTemplateAppReport);
        log.debug("admin email template body end: " + emailTemplateBodyEnd);

        List<Owner> owners = ownerRepository.findByEnabled(true);

        for (Owner owner : owners) {

            //generate one report code for all owner's apps.
            String reportCode = UUID.randomUUID().toString();

            StringBuilder appIdsStrList = new StringBuilder();
            StringBuilder currentBodyAppReports = new StringBuilder();

            List<Application> ownedApps = applicationRepository.findByOwnerIdAndDeletedAndEnabled(owner.getId(), false, true);

            for (Application application : ownedApps) {

                Configuration mailingListFrequencyConfig = configurationRepository.findByApplicationIdAndName(application.getId(), MAILING_LIST_SEND_FREQUENCY_CONFIG_KEY);
                if (mailingListFrequencyConfig == null || mailingListFrequencyConfig.getValue() == null) {
                    log.info("No mailing list configuration for report type: " + emailType + " for appId: " + application.getId() + " :: skipping.");
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

                //save report code
                saveReportCodeForApplication(reportCode, application.getId(), owner.getEmail());

                //append appid to csv list.
                appIdsStrList.append(application.getId().toString()).append(",");

                //replace app report urls in template.
                String currentAppReportForBody = emailTemplateAppReport; //get fresh each app
                currentAppReportForBody = currentAppReportForBody.replaceAll(APPLICATION_NAME_PLACEHOLDER, application.getName());
                currentAppReportForBody = currentAppReportForBody.replaceAll(APPLICATION_ID_PLACEHOLDER, application.getId().toString());
                currentAppReportForBody = currentAppReportForBody.replaceAll(MAINTENANCE_URL_PLACEHOLDER, baseUrl + maintenanceUrl + application.getId().toString());
                currentAppReportForBody = currentAppReportForBody.replaceAll(MAINTENANCE_THREADS_URL_PLACEHOLDER, baseUrl + maintenanceThreadsUrl + application.getId().toString());
                currentAppReportForBody = currentAppReportForBody.replaceAll(MAINTENANCE_SUBSCRIBERS_URL_PLACEHOLDER, baseUrl + maintenanceSubscriberUrl + application.getId().toString());
                currentAppReportForBody = currentAppReportForBody.replaceAll(MAINTENANCE_STATS_URL_PLACEHOLDER, baseUrl + maintenanceStatusUrl + application.getId().toString());
                currentAppReportForBody = currentAppReportForBody.replaceAll(MAINTENANCE_THREADS_UNAPPROVED_URL_PLACEHOLDER, baseUrl + maintenanceThreadsUnapprovedUrl + application.getId().toString());


                //total threads.
                long totalThreads = threadRepository.countByApplicationIdAndDeleted(application.getId(), false);
                currentAppReportForBody = currentAppReportForBody.replaceAll(TOTAL_THREADS_PLACEHOLDER, String.valueOf(totalThreads));

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
                    currentAppReportForBody = currentAppReportForBody.replaceAll(LAST_THREAD_CREATION_PLACEHOLDER, lastThreadStr);
                } else {
                    currentAppReportForBody = currentAppReportForBody.replaceAll(LAST_THREAD_CREATION_PLACEHOLDER, "");
                }

                //total unapproved threads.
                long totalUnapprovedThreads = threadRepository.countByApplicationIdAndIsApprovedAndDeleted(application.getId(), false, false);
                if (totalUnapprovedThreads > 0) {
                    currentAppReportForBody = currentAppReportForBody.replaceAll(TOTAL_UNAPPROVED_MESSAGES_PLACEHOLDER, String.valueOf(totalUnapprovedThreads));
                } else {
                    currentAppReportForBody = currentAppReportForBody.replaceAll(TOTAL_UNAPPROVED_MESSAGES_PLACEHOLDER, "");
                }

                //total subscribers.
                long totalSubscribers = subscriptionRepository.countByApplicationIdAndEnabled(application.getId(), true);
                if (totalSubscribers > 0) {
                    currentAppReportForBody = currentAppReportForBody.replaceAll(TOTAL_SUBSCRIBERS_PLACEHOLDER, String.valueOf(totalSubscribers));
                } else {
                    currentAppReportForBody = currentAppReportForBody.replaceAll(TOTAL_SUBSCRIBERS_PLACEHOLDER, "");
                }

                //latest subscription
                ApplicationSubscription lastSubscription = subscriptionRepository.findTopByApplicationIdAndEnabledOrderByCreateDtDesc(application.getId(), true);
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

                    currentAppReportForBody = currentAppReportForBody.replaceAll(LAST_SUBSCRIPTION_DATE_PLACEHOLDER, lastSubStr);
                } else {
                    currentAppReportForBody = currentAppReportForBody.replaceAll(LAST_SUBSCRIPTION_DATE_PLACEHOLDER, "");
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
                currentAppReportForBody = currentAppReportForBody.replaceAll(TOTAL_LAST_MONTH_VISITORS_PLACEHOLDER, String.format("%.2f", avgPageViews));

                //add current app report to output report html
                currentBodyAppReports.append(currentAppReportForBody);
            }

            if (currentBodyAppReports.length() == 0) {
                log.info("No app reports generated for owner: " + owner.toString() + " :: no report to email.");
                continue;
            }

            //start setting up top of email
            EmailNotificationMessage message = new EmailNotificationMessage();
            message.setTo(owner.getEmail());
            message.setType(emailType);
            message.setNotificationId(owner.getId());
            message.setSubject(templateSubject.getValue());

            //email start template replacements.
            String currentBodyStart = emailTemplateBodyStart;
            currentBodyStart = currentBodyStart.replaceAll(BASE_SITE_URL_PLACEHOLDER, baseUrl);
            currentBodyStart = currentBodyStart.replaceAll(HELP_FORUM_URL_PLACEHOLDER, baseUrl + helpForumUrl);
            currentBodyStart = currentBodyStart.replaceAll(OWNER_EMAIL_ADDRESS_PLACEHOLDER, owner.getEmail());
            currentBodyStart = currentBodyStart.replaceAll(MODIFY_ACCOUNT_URL_PLACEHOLDER, baseUrl + modifyAccountUrl);

            //replace template placeholders for end of email.
            String currentBodyEnd = emailTemplateBodyEnd;
            currentBodyEnd = currentBodyEnd.replaceAll(REPORT_FREQUENCY_URL_PLACEHOLDER, baseUrl + reportFrequencyUpdateUrl);
            currentBodyEnd = currentBodyEnd.replaceAll(OWNER_EMAIL_ADDRESS_PLACEHOLDER, owner.getEmail());
            currentBodyEnd = currentBodyEnd.replaceAll(GENERATED_AUTH_CODE_PLACEHOLDER, reportCode);
            currentBodyEnd = currentBodyEnd.replaceAll(APPLICATION_ID_PLACEHOLDER, appIdsStrList.toString());

            //combine all three sections of email body.
            String emailBody = currentBodyStart + currentBodyAppReports.toString() + currentBodyEnd;

            message.setBody(emailBody);
            notificationMessages.add(message);
            log.info("Finished processing " + emailType + " for owner: " + owner.toString());
        }

        isProcessed = true;
        super.sendNotifications(notificationMessages);
    }

    @Override
    public void updateLastSendDate(long notificationId) {
        log.info("Email notification: " + emailType + " does not set last send date in DB. Last send date for "
                + notificationId + " is " + new Date().toString());
    }

    private void saveReportCodeForApplication(String reportCode, long appId, String ownerEmail) {
        ApplicationReportCode code = applicationReportCodeRepository.findByApplicationIdAndEmail(appId, ownerEmail).orElse(null);
        if (code == null) {
            code = new ApplicationReportCode();
            code.setCreateDt(new Date());
            code.setApplicationId(appId);
            code.setEmail(ownerEmail);
        }
        code.setModDt(new Date());
        code.setCode(reportCode);
        applicationReportCodeRepository.save(code);
        log.info("Saved application report code: " + code.toString());
    }
}
