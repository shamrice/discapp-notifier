package io.github.shamrice.discapp.notification.repository;

import io.github.shamrice.discapp.notification.model.ApplicationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationSubscriptionRepository extends JpaRepository<ApplicationSubscription, Long> {

    List<ApplicationSubscription> findByApplicationIdAndEnabled(Long applicationId, Boolean enabled);
}
