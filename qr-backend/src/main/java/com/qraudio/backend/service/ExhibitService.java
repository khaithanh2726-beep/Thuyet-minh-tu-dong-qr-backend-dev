package com.qraudio.backend.service;

import com.qraudio.backend.dto.ExhibitResponse;
import com.qraudio.backend.entity.Exhibit;
import com.qraudio.backend.entity.ExhibitTranslation;
import com.qraudio.backend.exception.ResourceNotFoundException;
import com.qraudio.backend.repository.ExhibitRepository;
import com.qraudio.backend.repository.ExhibitTranslationRepository;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class ExhibitService {
    private final ExhibitRepository exhibitRepository;
    private final ExhibitTranslationRepository translationRepository;
    private final AudioService audioService;

    public ExhibitService(ExhibitRepository exhibitRepository, ExhibitTranslationRepository translationRepository, AudioService audioService) {
        this.exhibitRepository = exhibitRepository;
        this.translationRepository = translationRepository;
        this.audioService = audioService;
    }

    // Hàm gọi API lấy hiện vật và bản dịch
    public ExhibitResponse getExhibitByIdAndLang(Long id, String languageCode) {
        // Tìm hiện vật, nếu không có thì ném lỗi 404
        Exhibit exhibit = exhibitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hiện vật với ID: " + id));

        if (!exhibit.isActive()) {
            throw new ResourceNotFoundException("Hiện vật hiện không được trưng bày");
        }

        // Tìm bản dịch tương ứng, nếu không có thì ném lỗi 404
        ExhibitTranslation translation = translationRepository.findByExhibitIdAndLanguageCode(
                        id, languageCode.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Không có bản dịch cho ngôn ngữ: " + languageCode));

        // Gọi AudioService lấy file bản dịch
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
}
