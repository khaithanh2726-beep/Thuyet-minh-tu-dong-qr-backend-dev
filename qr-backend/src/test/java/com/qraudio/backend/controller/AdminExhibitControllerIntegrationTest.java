package com.qraudio.backend.controller;

import com.qraudio.backend.dto.CreateTranslationRequest;
import com.qraudio.backend.entity.AudioAsset;
import com.qraudio.backend.entity.Exhibit;
import com.qraudio.backend.entity.ExhibitTranslation;
import com.qraudio.backend.entity.QrCode;
import com.qraudio.backend.entity.User;
import com.qraudio.backend.exception.ApiException;
import com.qraudio.backend.repository.AudioAssetRepository;
import com.qraudio.backend.repository.ExhibitRepository;
import com.qraudio.backend.repository.ExhibitTranslationRepository;
import com.qraudio.backend.repository.QrCodeRepository;
import com.qraudio.backend.repository.UserRepository;
import com.qraudio.backend.security.JwtTokenUtil;
import com.qraudio.backend.service.AdminExhibitService;
import com.qraudio.backend.service.AudioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminExhibitControllerIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ExhibitRepository exhibits;
    @Autowired private ExhibitTranslationRepository translations;
    @Autowired private AudioAssetRepository audioAssets;
    @Autowired private QrCodeRepository qrCodes;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtTokenUtil jwt;
    @Autowired private AdminExhibitService service;
    @MockitoBean private AudioService audio;
    private Exhibit exhibit;
    private ExhibitTranslation vietnamese;
    private String bearer;

    @BeforeEach
    void setUp() {
        // The test profile points exclusively to the in-memory H2 database.
        audioAssets.deleteAll();
        qrCodes.deleteAll();
        translations.deleteAll();
        exhibits.deleteAll();
        users.deleteAll();
        bearer = saveUserToken("admin", "ADMIN");
        exhibit = saveExhibit(true);
        vietnamese = saveTranslation(exhibit.getId(), "vi-vn", "Nội dung tiếng Việt");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "PUT", "DELETE"})
    void adminOperations_RequireLogin(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), mutationPath(method))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "PUT", "DELETE"})
    void nonAdmin_CannotManageExhibitsOrTranslations(String method) throws Exception {
        String viewer = saveUserToken("viewer", "USER");
        mvc.perform(request(HttpMethod.valueOf(method), mutationPath(method))
                        .header("Authorization", viewer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAndReadExhibit_DefaultsToActive_WithoutGeneratingAudio() throws Exception {
        mvc.perform(post("/api/admin/exhibits").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"imageUrl\":\"museum.png\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.imageUrl").value("museum.png"))
                .andExpect(jsonPath("$.data.active").value(true));
        assertEquals(2, exhibits.count());
        mvc.perform(get("/api/admin/exhibits").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get(adminPath()).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(exhibit.getId()));
        mvc.perform(get(adminPath() + "/translations").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].content").value("Nội dung tiếng Việt"));
        verifyNoInteractions(audio);
    }

    @Test
    void createInactiveExhibit_AndUpdateImage() throws Exception {
        mvc.perform(post("/api/admin/exhibits").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.active").value(false));
        mvc.perform(patch(adminPath()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"imageUrl\":\"new.png\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.imageUrl").value("new.png"));
        assertEquals("new.png", exhibits.findById(exhibit.getId()).orElseThrow().getImageUrl());
        mvc.perform(patch(adminPath()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"imageUrl\":\"\"}"))
                .andExpect(status().isOk());
        assertNull(exhibits.findById(exhibit.getId()).orElseThrow().getImageUrl());
    }

    @Test
    void invalidExhibitUpdates_Return400() throws Exception {
        mvc.perform(patch(adminPath()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/exhibits").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageUrl\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(adminPath()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":{}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownExhibit_Returns404ForReadsAndMutations() throws Exception {
        String missing = "/api/admin/exhibits/999999";
        mvc.perform(get(missing).header("Authorization", bearer)).andExpect(status().isNotFound());
        mvc.perform(get(missing + "/translations").header("Authorization", bearer))
                .andExpect(status().isNotFound());
        mvc.perform(patch(missing).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(missing).header("Authorization", bearer)).andExpect(status().isNotFound());
        mvc.perform(post(missing + "/translations").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(translationJson("en-us", "English", "Content")))
                .andExpect(status().isNotFound());
    }

    @Test
    void translationLanguage_IsNormalized_AndDuplicateReturns409() throws Exception {
        mvc.perform(post(adminPath() + "/translations").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(translationJson("EN-US", "English title", "English content")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.languageCode").value("en-us"));
        mvc.perform(get(adminPath() + "/translations/EN-US").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").value("English content"));
        mvc.perform(post(adminPath() + "/translations").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(translationJson("en-us", "Duplicate", "Duplicate")))
                .andExpect(status().isConflict());
        assertEquals(2, translations.findAllByExhibitIdOrderByLanguageCodeAsc(exhibit.getId()).size());
        verifyNoInteractions(audio);
    }

    @Test
    void sameLanguage_CanBelongToDifferentExhibits() throws Exception {
        Exhibit other = saveExhibit(true);
        mvc.perform(post("/api/admin/exhibits/" + other.getId() + "/translations")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(translationJson("vi-vn", "Tên khác", "Nội dung khác")))
                .andExpect(status().isCreated());
        assertTrue(translations.existsByExhibitIdAndLanguageCode(exhibit.getId(), "vi-vn"));
        assertTrue(translations.existsByExhibitIdAndLanguageCode(other.getId(), "vi-vn"));
    }

    @Test
    void malformedOrIncompleteTranslation_Returns400() throws Exception {
        for (String invalid : new String[]{translationJson("vi", "Title", "Content"),
                translationJson("en-us", " ", "Content"), translationJson("en-us", "Title", " "),
                translationJson("en-us", "a".repeat(256), "Content"), "{}"}) {
            mvc.perform(post(adminPath() + "/translations").header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put(adminPath() + "/translations/vi-vn").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Title only\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(adminPath() + "/translations/invalid").header("Authorization", bearer))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/exhibits/not-a-number").header("Authorization", bearer))
                .andExpect(status().isBadRequest());
    }

    @Test
    void translationContent_EnforcesMysqlUtf8ByteLimit() throws Exception {
        mvc.perform(post(adminPath() + "/translations").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(translationJson("en-us", "Title", "ấ".repeat(22000))))
                .andExpect(status().isBadRequest());
        assertFalse(translations.existsByExhibitIdAndLanguageCode(exhibit.getId(), "en-us"));
    }

    @Test
    void longNarration_IsNotTruncated() throws Exception {
        String narration = "Nội dung thuyết minh. ".repeat(100);
        mvc.perform(post(adminPath() + "/translations").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(translationJson("en-us", "Title", narration)))
                .andExpect(status().isCreated());
        assertEquals(narration, translations.findByExhibitIdAndLanguageCode(
                exhibit.getId(), "en-us").orElseThrow().getContent());
    }

    @Test
    void translationFromAnotherExhibit_CannotBeReadModifiedOrDeleted() throws Exception {
        Exhibit other = saveExhibit(true);
        String path = "/api/admin/exhibits/" + other.getId() + "/translations/vi-vn";
        mvc.perform(get(path).header("Authorization", bearer)).andExpect(status().isNotFound());
        mvc.perform(put(path).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Title\",\"content\":\"Content\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(path).header("Authorization", bearer)).andExpect(status().isNotFound());
        assertTrue(translations.existsById(vietnamese.getId()));
    }

    @Test
    void updatingContent_InvalidatesOnlyItsAudioCache() throws Exception {
        AudioAsset old = saveAudio(vietnamese);
        ExhibitTranslation english = saveTranslation(exhibit.getId(), "en-us", "English content");
        AudioAsset other = saveAudio(english);
        mvc.perform(put(adminPath() + "/translations/vi-vn").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tên mới\",\"content\":\"Nội dung mới\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").value("Nội dung mới"));
        assertFalse(audioAssets.existsById(old.getId()));
        assertTrue(audioAssets.existsById(other.getId()));
        assertEquals("Nội dung mới", translations.findById(vietnamese.getId()).orElseThrow().getContent());
        verifyNoInteractions(audio);
    }

    @Test
    void changingOnlyTitle_PreservesAudioCache() throws Exception {
        AudioAsset existing = saveAudio(vietnamese);
        mvc.perform(put(adminPath() + "/translations/vi-vn").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tên mới\",\"content\":\"Nội dung tiếng Việt\"}"))
                .andExpect(status().isOk());
        assertTrue(audioAssets.existsById(existing.getId()));
    }

    @Test
    void deletingTranslation_RemovesAudioMetadataButPreservesOtherLanguage() throws Exception {
        AudioAsset old = saveAudio(vietnamese);
        ExhibitTranslation english = saveTranslation(exhibit.getId(), "en-us", "English content");
        AudioAsset other = saveAudio(english);
        mvc.perform(delete(adminPath() + "/translations/vi-vn").header("Authorization", bearer))
                .andExpect(status().isNoContent());
        assertFalse(translations.existsById(vietnamese.getId()));
        assertFalse(audioAssets.existsById(old.getId()));
        assertTrue(translations.existsById(english.getId()));
        assertTrue(audioAssets.existsById(other.getId()));
        assertTrue(exhibits.existsById(exhibit.getId()));
        mvc.perform(get("/api/exhibits/" + exhibit.getId()).param("lang", "vi-vn"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(audio);
    }

    @Test
    void deactivatingExhibit_PreservesData_AndBlocksPublicAndQrReads() throws Exception {
        QrCode qr = new QrCode();
        qr.setExhibitId(exhibit.getId());
        qr.setCode("test-exhibit-qr");
        qr.setActive(true);
        qr.setCreatedAt(LocalDateTime.now());
        qrCodes.saveAndFlush(qr);
        mvc.perform(delete(adminPath()).header("Authorization", bearer)).andExpect(status().isNoContent());
        mvc.perform(delete(adminPath()).header("Authorization", bearer)).andExpect(status().isNoContent());
        mvc.perform(get(adminPath()).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false));
        assertTrue(translations.existsById(vietnamese.getId()));
        assertTrue(qrCodes.existsById(qr.getId()));
        mvc.perform(get("/api/exhibits/" + exhibit.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/qrcodes/resolve/test-exhibit-qr")).andExpect(status().isNotFound());
        verifyNoInteractions(audio);
    }

    @Test
    void reactivatingExhibit_RestoresPublicRead() throws Exception {
        mvc.perform(delete(adminPath()).header("Authorization", bearer)).andExpect(status().isNoContent());
        mvc.perform(patch(adminPath()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(true));
        when(audio.getOrGenerateAudio(any())).thenReturn("/api/audio/play/test.mp3");
        mvc.perform(get("/api/exhibits/" + exhibit.getId()).param("lang", "VI-VN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.languageCode").value("vi-vn"));
    }

    @Test
    void publicReads_ReturnExactLanguage_WithoutSilentFallback() throws Exception {
        saveTranslation(exhibit.getId(), "en-us", "English content");
        when(audio.getOrGenerateAudio(any())).thenReturn("/api/audio/play/test.mp3");
        mvc.perform(get("/api/exhibits/" + exhibit.getId()).param("lang", "vi-vn"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").value("Nội dung tiếng Việt"));
        mvc.perform(get("/api/exhibits/" + exhibit.getId()).param("lang", "en-us"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").value("English content"));
        mvc.perform(get("/api/exhibits/" + exhibit.getId()).param("lang", "fr-fr"))
                .andExpect(status().isNotFound());
        verify(audio, times(2)).getOrGenerateAudio(any());
    }

    @Test
    void concurrentDuplicateTranslations_OnlyOneIsCreated() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> createConcurrentTranslation(start));
            var second = executor.submit(() -> createConcurrentTranslation(start));
            start.countDown();
            var statuses = java.util.List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertTrue(statuses.contains(201));
            assertTrue(statuses.contains(409));
        }
        assertEquals(1, translations.findAllByExhibitIdOrderByLanguageCodeAsc(exhibit.getId())
                .stream().filter(t -> "fr-fr".equals(t.getLanguageCode())).count());
    }

    @Test
    void swagger_DeclaresBearerForAdminExhibitAndTranslationOperations() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/admin/exhibits'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/admin/exhibits/{id}/translations/{languageCode}'].put.security[0].bearerAuth")
                        .isArray());
    }

    private int createConcurrentTranslation(CountDownLatch start) throws Exception {
        assertTrue(start.await(5, TimeUnit.SECONDS));
        try {
            service.createTranslation(exhibit.getId(), new CreateTranslationRequest("fr-fr", "Titre", "Contenu"));
            return 201;
        } catch (ApiException exception) {
            return exception.getStatus().value();
        }
    }

    private String adminPath() { return "/api/admin/exhibits/" + exhibit.getId(); }

    private String mutationPath(String method) {
        return switch (method) {
            case "GET" -> "/api/admin/exhibits";
            case "POST" -> adminPath() + "/translations";
            case "PATCH" -> adminPath();
            default -> adminPath() + "/translations/vi-vn";
        };
    }

    private String saveUserToken(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setPasswordHash(encoder.encode("TestPass123!"));
        user.setActive(true);
        users.saveAndFlush(user);
        return "Bearer " + jwt.generateToken(user.getUsername(), user.getId(), user.getTokenVersion());
    }

    private Exhibit saveExhibit(boolean active) {
        Exhibit value = new Exhibit();
        value.setImageUrl("test.png");
        value.setActive(active);
        return exhibits.saveAndFlush(value);
    }

    private ExhibitTranslation saveTranslation(Long exhibitId, String language, String content) {
        ExhibitTranslation value = new ExhibitTranslation();
        value.setExhibitId(exhibitId);
        value.setLanguageCode(language);
        value.setTitle("Title " + language);
        value.setContent(content);
        return translations.saveAndFlush(value);
    }

    private AudioAsset saveAudio(ExhibitTranslation translation) {
        AudioAsset asset = new AudioAsset();
        asset.setTranslationId(translation.getId());
        asset.setProvider("VoiceRSS");
        asset.setVoiceCode(translation.getLanguageCode());
        asset.setContentHash("test-hash");
        asset.setFormat("MP3");
        asset.setStorageKey("test.mp3");
        return audioAssets.saveAndFlush(asset);
    }

    private String translationJson(String language, String title, String content) {
        return "{\"languageCode\":\"" + language + "\",\"title\":\"" + title
                + "\",\"content\":\"" + content + "\"}";
    }
}
