package io.github.shamrice.discapp.notification.controller;

import io.github.shamrice.discapp.notification.service.emailer.ApplicationSubscriptionEmailNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@Controller
public class HealthCheckController {

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
}
