package com.qraudio.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class VoiceRssService {

    @Value("${voicerss.api-key}")
    private String apiKey;

    @Value("${voicerss.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Hàm gọi API VoiceRSS tạo file âm thanh từ văn bản
    public byte[] generateAudio(String text, String languageCode) {
        String url = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("key", apiKey)
                .queryParam("hl", languageCode)             // Ngôn ngữ bản dịch
                .queryParam("src", text)                    // Nội dung văn bản
                .queryParam("c", "MP3")                     // Định dạng MP3
                .queryParam("f", "44khz_16bit_stereo")      // Chất lượng âm thanh
                .toUriString();
        return restTemplate.getForObject(url, byte[].class);
    }
}