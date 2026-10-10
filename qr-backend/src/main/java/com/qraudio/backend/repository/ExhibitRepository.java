package com.qraudio.backend.repository;

import com.qraudio.backend.entity.Exhibit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface ExhibitRepository extends JpaRepository<Exhibit, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exhibit e where e.id = :id")
    Optional<Exhibit> findByIdForUpdate(@Param("id") Long id);
}
