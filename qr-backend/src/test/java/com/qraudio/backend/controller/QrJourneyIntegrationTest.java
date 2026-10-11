package com.qraudio.backend.controller;

import com.qraudio.backend.entity.*;
import com.qraudio.backend.repository.*;
import com.qraudio.backend.service.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.FileSystemResource;
import com.qraudio.backend.exception.ApiException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QrJourneyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AudioAssetRepository assets;
    @Autowired QrCodeRepository codes;
    @Autowired ExhibitTranslationRepository translations;
    @Autowired ExhibitRepository exhibits;
    @Autowired DemoDataService demo;
    @Autowired tools.jackson.databind.ObjectMapper mapper;
    @MockitoBean AudioService audio;
    @TempDir Path directory;
    @BeforeEach void setup() { assets.deleteAll(); codes.deleteAll(); translations.deleteAll(); exhibits.deleteAll(); demo.seed(); }
    @Test void seedIsIdempotentAndPreservesEditedData() {
        var qr = codes.findByCode("DEMO-OIL-LAMP").orElseThrow(); qr.setActive(false); codes.saveAndFlush(qr);
        var t = translations.findByExhibitIdAndLanguageCode(qr.getExhibitId(), "vi-vn").orElseThrow();
        t.setTitle("Đã sửa"); translations.saveAndFlush(t); demo.seed();
        assertEquals(3, exhibits.count()); assertEquals(6, translations.count()); assertEquals(3, codes.count());
        assertFalse(codes.findByCode(qr.getCode()).orElseThrow().isActive());
        assertEquals("Đã sửa", translations.findById(t.getId()).orElseThrow().getTitle());
    }
    @Test void metadataShowsBothLanguagesWithoutTts() throws Exception {
        mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exhibit.languages.length()").value(2));
        mvc.perform(get("/api/catalog")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3));
        Long id = codes.findByCode("DEMO-OIL-LAMP").orElseThrow().getExhibitId();
        mvc.perform(get("/api/exhibits/" + id + "/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.languages.length()").value(2));
        verifyNoInteractions(audio);
    }
    @Test void resolveUsesRequestedLanguageAndIncludesTextAndAudio() throws Exception {
        when(audio.getOrGenerateAudio(any())).thenReturn("/api/audio/play/fixture.mp3");
        mvc.perform(get("/api/qrcodes/resolve/DEMO-OIL-LAMP?lang=en-us")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.languageCode").value("en-us"))
                .andExpect(jsonPath("$.data.title").value("Oil lamp"))
                .andExpect(jsonPath("$.data.content").isNotEmpty()).andExpect(jsonPath("$.data.audioUrl").isNotEmpty());
    }
    @Test void qrPngDecodesToMetadataApi() throws Exception {
        byte[] png = mvc.perform(get("/api/qrcodes/DEMO-BRONZE-DRUM/image")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG)).andReturn().getResponse().getContentAsByteArray();
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(png)))));
        assertEquals("http://localhost/api/qrcodes/DEMO-BRONZE-DRUM", new MultiFormatReader().decode(bitmap).getText());
    }
    @Test void adminQrCrudAndDuplicateValidation() throws Exception {
        Long exhibitId = codes.findByCode("DEMO-OIL-LAMP").orElseThrow().getExhibitId();
        String body = "{\"exhibitId\":" + exhibitId + ",\"code\":\"my-custom-qr\"}";
        mvc.perform(post("/api/admin/qrcodes").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.code").value("MY-CUSTOM-QR"))
                .andExpect(jsonPath("$.data.qrUrl").value("http://localhost/api/qrcodes/MY-CUSTOM-QR"))
                .andExpect(jsonPath("$.data.visitorUrl").doesNotExist());
        mvc.perform(post("/api/admin/qrcodes").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        Long id = codes.findByCode("MY-CUSTOM-QR").orElseThrow().getId();
        mvc.perform(patch("/api/admin/qrcodes/" + id).with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false));
        mvc.perform(get("/api/qrcodes/MY-CUSTOM-QR")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/qrcodes/" + id).with(user("admin").roles("ADMIN"))).andExpect(status().isNoContent());
        assertTrue(codes.existsById(id));
    }
    @Test void adminQrEndpointsRequireAdmin() throws Exception {
        mvc.perform(get("/api/admin/qrcodes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/qrcodes").with(user("visitor").roles("USER"))).andExpect(status().isForbidden());
    }
    @Test void inactiveExhibitAndQrAndMissingLanguageAreNotPublic() throws Exception {
        var qr = codes.findByCode("DEMO-OIL-LAMP").orElseThrow();
        var exhibit = exhibits.findById(qr.getExhibitId()).orElseThrow(); exhibit.setActive(false); exhibits.saveAndFlush(exhibit);
        mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP")).andExpect(status().isNotFound());
        mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/image")).andExpect(status().isNotFound());
        mvc.perform(get("/api/catalog")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get("/api/qrcodes/resolve/DEMO-BRONZE-DRUM?lang=fr-fr")).andExpect(status().isNotFound());
        qr.setActive(false); codes.saveAndFlush(qr); mvc.perform(get("/api/qrcodes/resolve/DEMO-OIL-LAMP")).andExpect(status().isNotFound());
        verifyNoInteractions(audio);
    }
    @Test void supportedLanguagesArePublicWithoutGeneratingAudio() throws Exception {
        mvc.perform(get("/api/languages")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'vi-vn')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.code == 'en-us')]").isNotEmpty());
        verifyNoInteractions(audio);
    }

    @Test void adminSuppliedTranslationsAreUsedForQrNarration() throws Exception {
        var result = mvc.perform(post("/api/admin/exhibits").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated()).andReturn().getResponse();
        long id = mapper.readTree(result.getContentAsString()).path("data").path("id").asLong();
        var contents = java.util.Map.of("vi-vn", "Nội dung riêng do quản trị viên nhập.",
                "en-us", "A separate narration entered by the administrator.");
        for (var entry : contents.entrySet()) {
            String body = mapper.writeValueAsString(java.util.Map.of("languageCode", entry.getKey(),
                    "title", "Exhibit " + entry.getKey(), "content", entry.getValue()));
            mvc.perform(post("/api/admin/exhibits/" + id + "/translations").with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/admin/qrcodes").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("exhibitId", id, "code", "NEW-EXHIBIT"))))
                .andExpect(status().isCreated());
        when(audio.getOrGenerateAudio(any())).thenAnswer(call -> {
            ExhibitTranslation translation = call.getArgument(0);
            assertEquals(contents.get(translation.getLanguageCode()), translation.getContent());
            return "/api/audio/play/" + translation.getLanguageCode() + ".mp3";
        });
        for (var entry : contents.entrySet()) {
            mvc.perform(get("/api/qrcodes/resolve/NEW-EXHIBIT").param("lang", entry.getKey()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.languageCode").value(entry.getKey()))
                    .andExpect(jsonPath("$.data.content").value(entry.getValue()))
                    .andExpect(jsonPath("$.data.audioUrl").value("/api/audio/play/" + entry.getKey() + ".mp3"));
        }
        verify(audio, times(2)).getOrGenerateAudio(any());
    }

    @Test void unknownResourceReturns404InsteadOf500() throws Exception {
        mvc.perform(get("/api/unknown-resource").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test void playbackIsDocumentedAsMp3InOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/audio/play/{fileName}'].get.responses['200'].content['audio/mpeg'].schema.format")
                        .value("binary"))
                .andExpect(jsonPath("$.paths['/api/qrcodes/{code}/audio'].get.responses['200'].content['audio/mpeg'].schema.format")
                        .value("binary"));
    }

    @Test void languageQrImagesDecodeToDistinctDirectAudioUrlsWithoutTts() throws Exception {
        long qrId = codes.findByCode("DEMO-OIL-LAMP").orElseThrow().getId();
        for (String lang : java.util.List.of("vi-vn", "en-us")) {
            String expected = "http://localhost/api/qrcodes/DEMO-OIL-LAMP/audio?lang=" + lang;
            byte[] png = mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/image").param("lang", lang.toUpperCase()))
                    .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
                    .andReturn().getResponse().getContentAsByteArray();
            assertEquals(expected, decode(png));
            byte[] adminPng = mvc.perform(get("/api/admin/qrcodes/" + qrId + "/image")
                            .param("lang", lang).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            assertEquals(expected, decode(adminPng));
        }
        verifyNoInteractions(audio);
    }

    @Test void audioAndLanguageQrRejectMissingOrInvalidTranslationsBeforeTts() throws Exception {
        for (String suffix : java.util.List.of("image", "audio")) {
            for (String invalid : java.util.List.of("", "vi", "zz-zz")) {
                mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/" + suffix).param("lang", invalid))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
            }
            mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/" + suffix).param("lang", "fr-fr"))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/audio"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(audio);
    }

    @Test void directAudioIsPublicUsesChosenTranslationAndSupportsHeadAndRange() throws Exception {
        for (String lang : java.util.List.of("vi-vn", "en-us")) {
            var translation = translations.findByExhibitIdAndLanguageCode(
                    codes.findByCode("DEMO-OIL-LAMP").orElseThrow().getExhibitId(), lang).orElseThrow();
            byte[] bytes = new byte[]{'I', 'D', '3', 4, (byte) (lang.equals("vi-vn") ? 1 : 2), 0, 0, 0};
            Path file = Files.write(directory.resolve(lang + ".mp3"), bytes);
            doAnswer(call -> {
                ExhibitTranslation selected = call.getArgument(0);
                assertEquals(translation.getId(), selected.getId());
                assertEquals(lang, selected.getLanguageCode());
                assertEquals(translation.getContent(), selected.getContent());
                return new FileSystemResource(file);
            }).when(audio).getOrGenerateResource(any());
            mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/audio").param("lang", lang.toUpperCase()))
                    .andExpect(status().isOk()).andExpect(content().contentType("audio/mpeg"))
                    .andExpect(header().string("Content-Disposition", "inline"))
                    .andExpect(header().string("Cache-Control", "no-store")).andExpect(content().bytes(bytes));
            mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/audio").param("lang", lang).header("Range", "bytes=0-3"))
                    .andExpect(status().isPartialContent()).andExpect(header().string("Content-Range", "bytes 0-3/8"))
                    .andExpect(content().bytes(java.util.Arrays.copyOf(bytes, 4)));
            mvc.perform(head("/api/qrcodes/DEMO-OIL-LAMP/audio").param("lang", lang))
                    .andExpect(status().isOk()).andExpect(header().string("Content-Length", "8"));
        }
        verify(audio, times(6)).getOrGenerateResource(any());
        verify(audio, never()).getOrGenerateAudio(any());
    }

    @Test void disabledQrOrExhibitStopsLanguageImagesAndPlayback() throws Exception {
        var qr = codes.findByCode("DEMO-OIL-LAMP").orElseThrow();
        qr.setActive(false); codes.saveAndFlush(qr);
        for (String suffix : java.util.List.of("image", "audio")) {
            mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/" + suffix).param("lang", "vi-vn"))
                    .andExpect(status().isNotFound());
        }
        qr.setActive(true); codes.saveAndFlush(qr);
        var exhibit = exhibits.findById(qr.getExhibitId()).orElseThrow();
        exhibit.setActive(false); exhibits.saveAndFlush(exhibit);
        for (String suffix : java.util.List.of("image", "audio")) {
            mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/" + suffix).param("lang", "vi-vn"))
                    .andExpect(status().isNotFound());
        }
        verifyNoInteractions(audio);
    }

    @Test void sameScannedUrlUsesUpdatedNarrationAndRejectsDeletedTranslation() throws Exception {
        var qr = codes.findByCode("DEMO-OIL-LAMP").orElseThrow();
        var translation = translations.findByExhibitIdAndLanguageCode(qr.getExhibitId(), "vi-vn").orElseThrow();
        Path file = Files.write(directory.resolve("updated.mp3"), new byte[]{'I', 'D', '3', 4, 0});
        when(audio.getOrGenerateResource(any())).thenAnswer(call -> {
            ExhibitTranslation selected = call.getArgument(0);
            assertEquals("Nội dung thuyết minh đã cập nhật.", selected.getContent());
            return new FileSystemResource(file);
        });
        translation.setContent("Nội dung thuyết minh đã cập nhật."); translations.saveAndFlush(translation);
        mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/audio").param("lang", "vi-vn"))
                .andExpect(status().isOk());
        translations.delete(translation);
        mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/audio").param("lang", "vi-vn"))
                .andExpect(status().isNotFound());
        verify(audio, times(1)).getOrGenerateResource(any());
    }

    @Test void directAudioReportsProviderOrStorageErrorsInsteadOfReturningBrokenAudio() throws Exception {
        for (var failure : java.util.List.of(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE)) {
            doThrow(new ApiException(failure, "Không tạo được âm thanh")).when(audio).getOrGenerateResource(any());
            mvc.perform(get("/api/qrcodes/DEMO-OIL-LAMP/audio").param("lang", "vi-vn"))
                    .andExpect(status().is(failure.value())).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(failure.value()));
        }
    }

    private String decode(byte[] png) throws Exception {
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(
                ImageIO.read(new ByteArrayInputStream(png)))));
        return new MultiFormatReader().decode(bitmap).getText();
    }
}
