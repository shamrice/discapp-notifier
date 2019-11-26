package io.github.shamrice.discapp.notification.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Entity
@ToString
@Getter
@Setter
@NoArgsConstructor
public class Configuration {

    @Id
    @GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
    private Long id;

    private Long applicationId;
    private String name;
    private String value;
    private Date createDt;
    private Date modDt;

}
