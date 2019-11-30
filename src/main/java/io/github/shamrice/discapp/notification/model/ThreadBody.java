package io.github.shamrice.discapp.notification.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Entity
@ToString
@Getter
@Setter
@NoArgsConstructor
public class ThreadBody {

    @Id
    @GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "thread_id")
    private Long threadId;

    private String body;
    private Date createDt;
    private Date modDt;
}
