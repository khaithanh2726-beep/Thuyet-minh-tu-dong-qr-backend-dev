package com.qraudio.backend.service;

import com.qraudio.backend.dto.ExhibitResponse;
import com.qraudio.backend.dto.ExhibitInfoResponse;
import com.qraudio.backend.entity.Exhibit;
import com.qraudio.backend.entity.ExhibitTranslation;
import com.qraudio.backend.exception.ResourceNotFoundException;
import com.qraudio.backend.repository.ExhibitRepository;
import com.qraudio.backend.repository.ExhibitTranslationRepository;
import com.qraudio.backend.repository.QrCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ExhibitService {
    private final ExhibitRepository exhibitRepository;
    private final ExhibitTranslationRepository translationRepository;
    private final AudioService audioService;
    private final QrCodeRepository qrCodes;

    public ExhibitService(ExhibitRepository exhibitRepository, ExhibitTranslationRepository translationRepository,
                          AudioService audioService, QrCodeRepository qrCodes) {
        this.exhibitRepository = exhibitRepository;
        this.translationRepository = translationRepository;
        this.audioService = audioService;
        this.qrCodes = qrCodes;
    }

    @Transactional(readOnly = true)
    public List<ExhibitInfoResponse> listInfo() {
        return exhibitRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .map(this::info).filter(item -> !item.languages().isEmpty()).toList();
    }

    @Transactional(readOnly = true)
    public ExhibitInfoResponse getInfo(Long id) { return info(findActive(id)); }

    public ExhibitResponse getExhibitByIdAndLang(Long id, String languageCode) {
        Exhibit exhibit = findActive(id);
        ExhibitTranslation translation = translationRepository.findByExhibitIdAndLanguageCode(
                        id, Languages.normalize(languageCode))
                .orElseThrow(() -> new ResourceNotFoundException("Không có bản dịch cho ngôn ngữ: " + languageCode));

        String audioUrl = audioService.getOrGenerateAudio(translation);
        return new ExhibitResponse(
                exhibit.getId(),
                exhibit.getImageUrl(),
                translation.getLanguageCode(),
                translation.getTitle(),
                translation.getContent(),
                audioUrl
        );
    }

    private Exhibit findActive(Long id) {
        return exhibitRepository.findById(id).filter(Exhibit::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Hiện vật không tồn tại hoặc đã ngừng trưng bày"));
    }

    private ExhibitInfoResponse info(Exhibit exhibit) {
        var languages = translationRepository.findAllByExhibitIdOrderByLanguageCodeAsc(exhibit.getId()).stream()
                .map(t -> new ExhibitInfoResponse.Translation(t.getLanguageCode(), Languages.name(t.getLanguageCode()),
                        t.getTitle(), t.getContent())).toList();
        String code = qrCodes.findFirstByExhibitIdAndIsActiveTrueOrderByIdAsc(exhibit.getId())
                .map(qr -> qr.getCode()).orElse(null);
        return new ExhibitInfoResponse(exhibit.getId(), exhibit.getImageUrl(), languages, code);
    }
}
