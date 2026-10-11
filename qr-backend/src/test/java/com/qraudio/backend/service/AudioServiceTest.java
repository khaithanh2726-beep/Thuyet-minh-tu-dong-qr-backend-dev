package com.qraudio.backend.service;

import com.qraudio.backend.entity.*;
import com.qraudio.backend.exception.*;
import com.qraudio.backend.repository.AudioAssetRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AudioServiceTest {
    @TempDir Path directory;
    AudioAssetRepository assets;
    VoiceRssService voice;
    AudioService service;
    ExhibitTranslation translation;
    @BeforeEach void setup() {
        assets = mock(AudioAssetRepository.class); voice = mock(VoiceRssService.class);
        when(assets.findByTranslationIdAndContentHashAndFormat(anyLong(), anyString(), eq("MP3"))).thenReturn(Optional.empty());
        when(voice.generateAudio(anyString(), anyString())).thenReturn(new byte[]{'I','D','3',4,0});
        service = new AudioService(assets, voice, directory.toString());
        translation = new ExhibitTranslation(); translation.setId(7L); translation.setLanguageCode("vi-vn"); translation.setContent("Xin chào");
    }
    @Test void repeatedRequestsReuseFileAndConcurrentRequestsCallProviderOnce() throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            var jobs = new ArrayList<Callable<String>>(); for (int i=0;i<8;i++) jobs.add(() -> service.getOrGenerateAudio(translation));
            var results = executor.invokeAll(jobs); String first = results.getFirst().get();
            for (var result : results) assertEquals(first, result.get());
            assertTrue(service.getAudio(first.substring(first.lastIndexOf('/')+1)).exists());
        }
        verify(voice, times(1)).generateAudio("Xin chào", "vi-vn");
    }
    @Test void changedTextCreatesNewFile() {
        String first = service.getOrGenerateAudio(translation); translation.setContent("Nội dung mới");
        assertNotEquals(first, service.getOrGenerateAudio(translation)); verify(voice,times(2)).generateAudio(anyString(),eq("vi-vn"));
    }
    @Test void missingOrCorruptCachedFileIsRegenerated() throws Exception {
        String url = service.getOrGenerateAudio(translation), filename = url.substring(url.lastIndexOf('/')+1);
        Files.writeString(directory.resolve(filename), "ERROR");
        assertEquals(url, service.getOrGenerateAudio(translation));
        Files.delete(directory.resolve(filename)); assertEquals(url, service.getOrGenerateAudio(translation));
        verify(voice,times(3)).generateAudio(anyString(),anyString());
    }
    @Test void badProviderResponseDoesNotCreateCacheMetadata() {
        when(voice.generateAudio(anyString(),anyString())).thenReturn("ERROR".getBytes());
        assertThrows(ApiException.class, () -> service.getOrGenerateAudio(translation)); verify(assets,never()).saveAndFlush(any());
    }
    @Test void traversalAndMissingFilesAreRejected() {
        assertThrows(ResourceNotFoundException.class, () -> service.getAudio("../secret.properties"));
        assertThrows(ResourceNotFoundException.class, () -> service.getAudio("audio_7_" + "a".repeat(64) + ".mp3"));
    }
    @Test void directPlaybackAndJsonResolutionShareCacheAndRegenerateForChangedContent() throws Exception {
        String first = service.getOrGenerateAudio(translation);
        var resource = service.getOrGenerateResource(translation);
        assertEquals(first.substring(first.lastIndexOf('/') + 1), resource.getFilename());
        assertArrayEquals(new byte[]{'I','D','3',4,0}, resource.getContentAsByteArray());
        verify(voice, times(1)).generateAudio("Xin chào", "vi-vn");
        translation.setContent("Nội dung mới");
        var updated = service.getOrGenerateResource(translation);
        assertNotEquals(resource.getFilename(), updated.getFilename());
        assertEquals("/api/audio/play/" + updated.getFilename(), service.getOrGenerateAudio(translation));
        verify(voice, times(2)).generateAudio(anyString(), eq("vi-vn"));
    }
}
