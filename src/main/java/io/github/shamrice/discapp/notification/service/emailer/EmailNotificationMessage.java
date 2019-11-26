package io.github.shamrice.discapp.notification.service.emailer;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class EmailNotificationMessage {

    private String type;
    private String to;
    private String subject;
    private String body;
}
