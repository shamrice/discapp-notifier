package io.github.shamrice.discapp.notification.controller;

import io.github.shamrice.discapp.notification.service.emailer.ApplicationSubscriptionEmailNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@Controller
@Slf4j
public class HealthCheckController {

    @Value("${discapp.manual-processing.enabled}")
    private boolean isManualProcessingEnabled;

    @Autowired
    private ApplicationSubscriptionEmailNotificationService applicationSubscriptionNotificationService;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public String getRoot(HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        return "Ok!";
    }

    @GetMapping("/check/{id}")
    @ResponseBody
    public String getSubscriptions(@PathVariable Long id, HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
       // applicationSubscriptionNotificationService.getSubscribersOfApplication(id);
        return "See logs.";
    }

    @GetMapping("/process")
    @ResponseBody
    public String process(HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        if (isManualProcessingEnabled) {
            log.info("Manual processing enabled and was called by url. Processing records");
            applicationSubscriptionNotificationService.process();
        } else {
            log.info("Manual processing is not enabled via browser call. This request will be ignored.");
        }

        return "See logs";
    }
}
