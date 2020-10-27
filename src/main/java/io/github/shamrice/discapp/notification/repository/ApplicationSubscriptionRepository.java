package io.github.shamrice.discapp.notification.repository;

import io.github.shamrice.discapp.notification.model.ApplicationSubscription;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Qualifier("applicationSubscriptionRepository")
public interface ApplicationSubscriptionRepository extends JpaRepository<ApplicationSubscription, Long> {

    List<ApplicationSubscription> findByApplicationIdAndEnabled(Long applicationId, Boolean enabled);

    ApplicationSubscription findTopByApplicationIdAndEnabledOrderByCreateDtDesc(Long applicationId, Boolean enabled);
    long countByApplicationIdAndEnabled(Long applicationId, Boolean enabled);
}
