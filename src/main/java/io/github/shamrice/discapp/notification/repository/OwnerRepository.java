package io.github.shamrice.discapp.notification.repository;

import io.github.shamrice.discapp.notification.model.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
    Optional<Owner> findByIdAndEnabled(Long id, Boolean enabled);
    List<Owner> findByEnabled(Boolean enabled);
}
