package com.qraudio.backend.repository;

import com.qraudio.backend.entity.AudioAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AudioAssetRepository extends JpaRepository<AudioAsset, Long> {
    Optional<AudioAsset> findByTranslationIdAndContentHashAndFormat(Long translationId, String contentHash, String format);
}