package com.qraudio.backend.service;

import com.qraudio.backend.entity.AudioAsset;
import com.qraudio.backend.entity.ExhibitTranslation;
import com.qraudio.backend.exception.ApiException;
import com.qraudio.backend.exception.ResourceNotFoundException;
import com.qraudio.backend.repository.AudioAssetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;

@Service
public class AudioService {
    private final AudioAssetRepository assets;
    private final VoiceRssService voice;
    private final Path root;
    private final Object[] locks = new Object[64];

    public AudioService(AudioAssetRepository assets, VoiceRssService voice,
                        @Value("${audio.storage-dir:audio_cache}") String directory) {
        this.assets = assets;
        this.voice = voice;
        root = Path.of(directory).toAbsolutePath().normalize();
        java.util.Arrays.setAll(locks, i -> new Object());
        try { Files.createDirectories(root); }
        catch (IOException exception) { throw storageError(exception); }
    }
    public String getOrGenerateAudio(ExhibitTranslation translation) {
        return url(getOrGenerateFilename(translation));
    }
    public Resource getOrGenerateResource(ExhibitTranslation translation) {
        return getAudio(getOrGenerateFilename(translation));
    }
    private String getOrGenerateFilename(ExhibitTranslation translation) {
        String language = Languages.requireSupported(translation.getLanguageCode());
        String hash = hash(language + "\n" + translation.getContent());
        String filename = "audio_" + translation.getId() + "_" + hash + ".mp3";
        synchronized (locks[Math.floorMod(translation.getId().hashCode(), locks.length)]) {
            AudioAsset asset = assets.findByTranslationIdAndContentHashAndFormat(
                    translation.getId(), hash, "MP3").orElse(null);
            if (asset != null && validFile(safePath(asset.getStorageKey()))) {
                return asset.getStorageKey();
            }
            Path target = safePath(filename);
            if (!validFile(target)) {
                byte[] bytes = voice.generateAudio(translation.getContent(), language);
                if (!VoiceRssService.isMp3(bytes)) throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "Dịch vụ âm thanh trả về dữ liệu không hợp lệ");
                Path temporary = null;
                try {
                    temporary = Files.createTempFile(root, "tts-", ".tmp");
                    Files.write(temporary, bytes);
                    try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                    catch (AtomicMoveNotSupportedException e) {
                        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException exception) { throw storageError(exception); }
                finally {
                    if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
                }
            }
            if (asset == null) asset = new AudioAsset();
            asset.setTranslationId(translation.getId());
            asset.setProvider("VoiceRSS");
            asset.setVoiceCode(language);
            asset.setContentHash(hash);
            asset.setFormat("MP3");
            asset.setStorageKey(filename);
            assets.saveAndFlush(asset);
            return filename;
        }
    }
    public Resource getAudio(String filename) {
        Path path = safePath(filename);
        if (!validFile(path)) throw new ResourceNotFoundException("Không tìm thấy âm thanh hợp lệ");
        return new FileSystemResource(path);
    }
    private Path safePath(String filename) {
        if (filename == null || !filename.matches("audio_(?:exhibit_)?[0-9]+_[a-f0-9]{32,64}\\.mp3"))
            throw new ResourceNotFoundException("Tên file âm thanh không hợp lệ");
        Path path = root.resolve(filename).normalize();
        if (!path.startsWith(root) || Files.isSymbolicLink(path))
            throw new ResourceNotFoundException("Đường dẫn âm thanh không hợp lệ");
        return path;
    }
    private boolean validFile(Path path) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return false;
        try (var input = Files.newInputStream(path)) { return VoiceRssService.isMp3(input.readNBytes(4)); }
        catch (IOException exception) { return false; }
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private static String url(String filename) { return "/api/audio/play/" + filename; }
    private static ApiException storageError(IOException cause) {
        ApiException error = new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Không lưu được âm thanh trên máy chủ");
        error.initCause(cause);
        return error;
    }
}
