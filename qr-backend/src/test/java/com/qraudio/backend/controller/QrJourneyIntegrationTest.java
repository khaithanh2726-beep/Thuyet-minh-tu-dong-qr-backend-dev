package com.qraudio.backend.controller;

import com.qraudio.backend.entity.*;
import com.qraudio.backend.repository.*;
import com.qraudio.backend.service.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
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
                        .value("binary"));
    }
}
