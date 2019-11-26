package io.github.shamrice.discapp.notification.repository;

import io.github.shamrice.discapp.notification.model.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {

    Configuration findByApplicationIdAndName(Long applicationId, String name);
}
