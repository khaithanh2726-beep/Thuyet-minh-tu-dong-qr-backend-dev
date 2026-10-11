package com.qraudio.backend.service;

import com.qraudio.backend.dto.AdminExhibitResponse;
import com.qraudio.backend.dto.CreateExhibitRequest;
import com.qraudio.backend.dto.CreateTranslationRequest;
import com.qraudio.backend.dto.TranslationResponse;
import com.qraudio.backend.dto.UpdateExhibitRequest;
import com.qraudio.backend.dto.UpdateTranslationRequest;
import com.qraudio.backend.entity.Exhibit;
import com.qraudio.backend.entity.ExhibitTranslation;
import com.qraudio.backend.exception.ApiException;
import com.qraudio.backend.exception.ResourceNotFoundException;
import com.qraudio.backend.repository.AudioAssetRepository;
import com.qraudio.backend.repository.ExhibitRepository;
import com.qraudio.backend.repository.ExhibitTranslationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class AdminExhibitService {
    private final ExhibitRepository exhibits;
    private final ExhibitTranslationRepository translations;
    private final AudioAssetRepository audioAssets;

    public AdminExhibitService(ExhibitRepository exhibits,
                               ExhibitTranslationRepository translations,
                               AudioAssetRepository audioAssets) {
        this.exhibits = exhibits;
        this.translations = translations;
        this.audioAssets = audioAssets;
    }

    @Transactional(readOnly = true)
    public List<AdminExhibitResponse> list() {
        return exhibits.findAll(Sort.by("id")).stream().map(AdminExhibitResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminExhibitResponse get(Long id) {
        return AdminExhibitResponse.from(findExhibit(id));
    }

    @Transactional
    public AdminExhibitResponse create(CreateExhibitRequest request) {
        Exhibit exhibit = new Exhibit();
        exhibit.setImageUrl(normalizeImageUrl(request.imageUrl()));
        exhibit.setActive(request.active() == null || request.active());
        return AdminExhibitResponse.from(exhibits.saveAndFlush(exhibit));
    }

    @Transactional
    public AdminExhibitResponse update(Long id, UpdateExhibitRequest request) {
        if (request.imageUrl() == null && request.active() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chưa có thông tin cần cập nhật");
        }
        Exhibit exhibit = lockExhibit(id);
        if (request.imageUrl() != null) { exhibit.setImageUrl(normalizeImageUrl(request.imageUrl())); }
        if (request.active() != null) { exhibit.setActive(request.active()); }
        return AdminExhibitResponse.from(exhibits.saveAndFlush(exhibit));
    }

    @Transactional
    public void deactivate(Long id) {
        Exhibit exhibit = lockExhibit(id);
        exhibit.setActive(false);
        exhibits.saveAndFlush(exhibit);
    }

    @Transactional(readOnly = true)
    public List<TranslationResponse> listTranslations(Long exhibitId) {
        findExhibit(exhibitId);
        return translations.findAllByExhibitIdOrderByLanguageCodeAsc(exhibitId)
                .stream().map(TranslationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TranslationResponse getTranslation(Long exhibitId, String languageCode) {
        findExhibit(exhibitId);
        return TranslationResponse.from(findTranslation(exhibitId, Languages.normalize(languageCode)));
    }

    @Transactional
    public TranslationResponse createTranslation(Long exhibitId, CreateTranslationRequest request) {
        lockExhibit(exhibitId);
        String language = Languages.requireSupported(request.languageCode());
        validateContent(request.content());
        if (translations.existsByExhibitIdAndLanguageCode(exhibitId, language)) {
            throw new ApiException(HttpStatus.CONFLICT, "Hiện vật đã có bản dịch cho ngôn ngữ này");
        }
        ExhibitTranslation translation = new ExhibitTranslation();
        translation.setExhibitId(exhibitId);
        translation.setLanguageCode(language);
        translation.setTitle(request.title().trim());
        translation.setContent(request.content());
        return TranslationResponse.from(translations.saveAndFlush(translation));
    }

    @Transactional
    public TranslationResponse updateTranslation(Long exhibitId, String languageCode,
                                                  UpdateTranslationRequest request) {
        lockExhibit(exhibitId);
        ExhibitTranslation translation = findTranslation(exhibitId, Languages.normalize(languageCode));
        validateContent(request.content());
        if (!request.content().equals(translation.getContent())) {
            // Local file cleanup belongs to the audio lifecycle.
            audioAssets.deleteByTranslationId(translation.getId());
            audioAssets.flush();
        }
        translation.setTitle(request.title().trim());
        translation.setContent(request.content());
        return TranslationResponse.from(translations.saveAndFlush(translation));
    }

    @Transactional
    public void deleteTranslation(Long exhibitId, String languageCode) {
        lockExhibit(exhibitId);
        ExhibitTranslation translation = findTranslation(exhibitId, Languages.normalize(languageCode));
        audioAssets.deleteByTranslationId(translation.getId());
        // Flush children first to respect MySQL's foreign key.
        audioAssets.flush();
        translations.delete(translation);
        translations.flush();
    }

    private Exhibit findExhibit(Long id) {
        return exhibits.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy hiện vật"));
    }

    private Exhibit lockExhibit(Long id) {
        return exhibits.findByIdForUpdate(id).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy hiện vật"));
    }

    private ExhibitTranslation findTranslation(Long exhibitId, String language) {
        return translations.findByExhibitIdAndLanguageCode(exhibitId, language).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy bản dịch của hiện vật"));
    }

    private String normalizeImageUrl(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim();
    }

    private void validateContent(String content) {
        if (content.getBytes(StandardCharsets.UTF_8).length > 65535) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nội dung vượt quá giới hạn 65535 byte UTF-8");
        }
    }
}
