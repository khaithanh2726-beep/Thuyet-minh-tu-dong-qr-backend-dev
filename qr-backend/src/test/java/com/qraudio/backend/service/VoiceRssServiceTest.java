package com.qraudio.backend.service;

import com.qraudio.backend.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class VoiceRssServiceTest {
    @Test void sendsFormByPostAndAcceptsMp3() {
        var client = new RestTemplate(); var server = MockRestServiceServer.bindTo(client).build();
        server.expect(requestTo("https://example.invalid/tts")).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(org.springframework.util.MultiValueMap.fromSingleValue(java.util.Map.of(
                        "key", "fixture", "hl", "vi-vn", "src", "Xin chào", "c", "MP3", "f", "44khz_16bit_stereo"))))
                .andRespond(withSuccess(new byte[]{'I','D','3',4,0}, MediaType.parseMediaType("audio/mpeg")));
        assertEquals(5, new VoiceRssService("fixture", "https://example.invalid/tts", client).generateAudio("Xin chào", "VI-VN").length);
        server.verify();
    }
    @Test void providerErrorTextIsNotSavedAsAudio() {
        var client = new RestTemplate(); var server = MockRestServiceServer.bindTo(client).build();
        server.expect(anything()).andRespond(withSuccess("ERROR: secret provider detail", MediaType.TEXT_PLAIN));
        ApiException error = assertThrows(ApiException.class, () ->
                new VoiceRssService("fixture", "https://example.invalid/tts", client).generateAudio("Hello", "en-us"));
        assertEquals(502, error.getStatus().value()); assertFalse(error.getMessage().contains("secret provider detail"));
    }
    @Test void httpFailureHasSafeMessage() {
        var client = new RestTemplate(); var server = MockRestServiceServer.bindTo(client).build();
        server.expect(anything()).andRespond(withServerError());
        assertEquals(502, assertThrows(ApiException.class, () -> new VoiceRssService("fixture", "https://example.invalid/tts", client)
                .generateAudio("Hello", "en-us")).getStatus().value());
    }
    @Test void missingConfigurationIsClearAndDoesNotCallProvider() {
        assertEquals(503, assertThrows(ApiException.class, () -> new VoiceRssService("", "https://example.invalid/tts")
                .generateAudio("Hello", "en-us")).getStatus().value());
    }
    @Test void unsupportedLanguageFailsBeforeNetworkCall() {
        assertEquals(400, assertThrows(ApiException.class, () -> new VoiceRssService("fixture", "https://example.invalid/tts")
                .generateAudio("Hello", "xx-xx")).getStatus().value());
    }
}
