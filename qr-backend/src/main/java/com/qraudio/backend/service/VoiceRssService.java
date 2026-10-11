package com.qraudio.backend.service;

import com.qraudio.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class VoiceRssService {
    private final String apiKey;
    private final String apiUrl;
    private final RestTemplate client;

    @org.springframework.beans.factory.annotation.Autowired
    public VoiceRssService(@Value("${voicerss.api-key:}") String apiKey,
                           @Value("${voicerss.url}") String apiUrl) {
        this(apiKey, apiUrl, createClient());
    }
    VoiceRssService(String apiKey, String apiUrl, RestTemplate client) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.client = client;
    }
    private static RestTemplate createClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(45000);
        return new RestTemplate(factory);
    }
    public byte[] generateAudio(String text, String languageCode) {
        languageCode = Languages.requireSupported(languageCode);
        if (apiKey.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Chưa cấu hình khóa VoiceRSS. Vui lòng liên hệ quản trị viên.");
        var form = new LinkedMultiValueMap<String, String>();
        form.add("key", apiKey);
        form.add("hl", languageCode);
        form.add("src", text);
        form.add("c", "MP3");
        form.add("f", "44khz_16bit_stereo");
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            byte[] bytes = client.postForObject(apiUrl, new HttpEntity<>(form, headers), byte[].class);
            if (!isMp3(bytes)) throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "VoiceRSS chưa tạo được âm thanh. Kiểm tra khóa API, hạn mức và ngôn ngữ.");
            return bytes;
        } catch (RestClientException exception) {
            // Do not expose provider responses, request parameters or credentials.
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Không kết nối được dịch vụ âm thanh. Vui lòng thử lại sau.");
        }
    }
    public static boolean isMp3(byte[] data) {
        return data != null && data.length >= 4 &&
                ((data[0] == 'I' && data[1] == 'D' && data[2] == '3') ||
                 ((data[0] & 255) == 255 && (data[1] & 224) == 224));
    }
}
