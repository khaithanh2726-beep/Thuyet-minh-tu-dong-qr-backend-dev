package com.qraudio.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;

@Service
public class PublicUrlService {
    private final String base;
    public PublicUrlService(@Value("${app.public-base-url:}") String configured) {
        base = configured.replaceAll("/+$", "");
        if (!base.isBlank()) {
            URI uri = URI.create(base);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
                throw new IllegalArgumentException("PUBLIC_BASE_URL phải là địa chỉ HTTP/HTTPS hợp lệ");
        }
    }
    public String qrMetadata(String code) {
        return origin() + "/api/qrcodes/" + code;
    }
    public String qrAudio(String code, String language) {
        return origin() + "/api/qrcodes/" + code + "/audio?lang=" + Languages.requireSupported(language);
    }
    private String origin() {
        return base.isBlank() ? ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() : base;
    }
}
