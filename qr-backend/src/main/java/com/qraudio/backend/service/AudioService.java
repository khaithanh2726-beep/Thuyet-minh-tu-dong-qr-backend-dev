package com.qraudio.backend.service;

import com.qraudio.backend.entity.AudioAsset;
import com.qraudio.backend.entity.ExhibitTranslation;
import com.qraudio.backend.repository.AudioAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.io.File;
import java.io.FileOutputStream;

@Service
public class AudioService {
    private final AudioAssetRepository audioRepository;
    private final VoiceRssService voiceRssService;
    private final String AUDIO_DIR = "audio_cache/";        // Thư mục lưu file MP3

    public AudioService(AudioAssetRepository audioRepository, VoiceRssService voiceRssService) {
        this.audioRepository = audioRepository;
        this.voiceRssService = voiceRssService;
        new File(AUDIO_DIR).mkdirs();                       // Tạo thư mục nếu chưa có
    }

    public String getOrGenerateAudio(ExhibitTranslation translation) {
        try {
            // Tạo mã file bản dịch
            String contentHash = DigestUtils.md5DigestAsHex(translation.getContent().getBytes());

            // Kiểm tra file bản dịch - true: trả link, false: tạo mới
            AudioAsset existingAudio = audioRepository
                    .findByTranslationIdAndContentHashAndFormat(translation.getId(), contentHash, "MP3")
                    .orElse(null);
            if (existingAudio != null) {
                return "/api/audio/play/" + existingAudio.getStorageKey();
            }
            byte[] audioBytes = voiceRssService.generateAudio(translation.getContent(), translation.getLanguageCode());

            // Lưu file MP3
            String fileName = "audio_exhibit_" + translation.getId() + "_" + contentHash + ".mp3";
            try (FileOutputStream fos = new FileOutputStream(AUDIO_DIR + fileName)) {
                fos.write(audioBytes);
            }

            // Lịch sử Database
            AudioAsset newAudio = new AudioAsset();
            newAudio.setTranslationId(translation.getId());
            newAudio.setProvider("VoiceRSS");
            newAudio.setVoiceCode(translation.getLanguageCode());
            newAudio.setContentHash(contentHash);
            newAudio.setFormat("MP3");
            newAudio.setStorageKey(fileName);
            audioRepository.save(newAudio);
            return "/api/audio/play/" + fileName;
        } catch (Exception e) {
            return null;
        }
    }
}