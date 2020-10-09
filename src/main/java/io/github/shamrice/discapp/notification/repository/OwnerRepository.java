package io.github.shamrice.discapp.notification.repository;

import io.github.shamrice.discapp.notification.model.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {

}
