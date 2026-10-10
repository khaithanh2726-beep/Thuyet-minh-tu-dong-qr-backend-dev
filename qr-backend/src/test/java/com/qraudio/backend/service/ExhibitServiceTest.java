package com.qraudio.backend.service;

import com.qraudio.backend.dto.ExhibitResponse;
import com.qraudio.backend.entity.Exhibit;
import com.qraudio.backend.entity.ExhibitTranslation;
import com.qraudio.backend.exception.ResourceNotFoundException;
import com.qraudio.backend.repository.ExhibitRepository;
import com.qraudio.backend.repository.ExhibitTranslationRepository;
import com.qraudio.backend.service.AudioService;
import com.qraudio.backend.service.ExhibitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExhibitServiceTest {
    @Mock
    private ExhibitRepository exhibitRepository;
    @Mock
    private ExhibitTranslationRepository translationRepository;
    @Mock
    private AudioService audioService;
    @InjectMocks
    private ExhibitService exhibitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getExhibitByIdAndLang_Success() {
        // Giả lập dữ liệu trong DB
        Long exhibitId = 1L;
        String lang = "vi-vn";
        Exhibit exhibit = new Exhibit();
        exhibit.setId(exhibitId);
        exhibit.setActive(true);
        exhibit.setImageUrl("test.png");
        ExhibitTranslation translation = new ExhibitTranslation();
        translation.setLanguageCode(lang);
        translation.setTitle("Bảo vật quốc gia");
        translation.setContent("Nội dung test");

        // Cấu hình Mockito
        when(exhibitRepository.findById(exhibitId)).thenReturn(Optional.of(exhibit));
        when(translationRepository.findByExhibitIdAndLanguageCode(exhibitId, lang)).thenReturn(Optional.of(translation));
        when(audioService.getOrGenerateAudio(translation)).thenReturn("/api/audio/play/mock.mp3");

        // Gọi hàm và kiểm tra
        ExhibitResponse response = exhibitService.getExhibitByIdAndLang(exhibitId, lang);
        assertNotNull(response);
        assertEquals("Bảo vật quốc gia", response.getTitle());
        assertEquals("/api/audio/play/mock.mp3", response.getAudioUrl());
    }

    @Test
    void getExhibitByIdAndLang_NotFound() {
        Long exhibitId = 99L;
        when(exhibitRepository.findById(exhibitId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            exhibitService.getExhibitByIdAndLang(exhibitId, "vi-vn");
        });
    }
}
