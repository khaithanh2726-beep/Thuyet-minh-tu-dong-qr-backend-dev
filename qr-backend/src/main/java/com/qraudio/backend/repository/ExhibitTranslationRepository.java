package com.qraudio.backend.repository;

import com.qraudio.backend.entity.ExhibitTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ExhibitTranslationRepository extends JpaRepository<ExhibitTranslation, Long> {
    Optional<ExhibitTranslation> findByExhibitIdAndLanguageCode(Long exhibitId, String languageCode);
    List<ExhibitTranslation> findAllByExhibitIdOrderByLanguageCodeAsc(Long exhibitId);
    boolean existsByExhibitIdAndLanguageCode(Long exhibitId, String languageCode);
}
