package io.github.shamrice.discapp.notification.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;

@Entity
@ToString
@Getter
@Setter
@NoArgsConstructor
@Table(name = "thread")
public class Thread {

    @Id
    @GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "application_id")
    private Long applicationId;

    private String submitter;
    private String email;
    private String ipAddress;
    private String userAgent;
    private String subject;

    @Column(table = "thread", name = "show_email")
    private boolean showEmail;

    private Boolean deleted;
    private Long parentId;

    private boolean isApproved;

    private Date createDt;
    private Date modDt;
}
