package com.qraudio.backend.service;

import com.qraudio.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import java.util.*;

public final class Languages {
    private Languages() {}
    private static final Set<String> SUPPORTED = Set.of(
            "ar-eg","ar-sa","bg-bg","ca-es","zh-cn","zh-hk","zh-tw","hr-hr","cs-cz","da-dk",
            "nl-be","nl-nl","en-au","en-ca","en-gb","en-in","en-ie","en-us","fi-fi","fr-ca",
            "fr-fr","fr-ch","de-at","de-de","de-ch","el-gr","he-il","hi-in","hu-hu","id-id",
            "it-it","ja-jp","ko-kr","ms-my","nb-no","pl-pl","pt-br","pt-pt","ro-ro","ru-ru",
            "sk-sk","sl-si","es-mx","es-es","sv-se","ta-in","th-th","tr-tr","vi-vn");
    public record Language(String code, String name) {}
    public static String name(String code) {
        if ("vi-vn".equals(code)) return "Tiếng Việt";
        if ("en-us".equals(code)) return "English";
        return Locale.forLanguageTag(code).getDisplayName(Locale.forLanguageTag("vi"));
    }
    public static List<Language> list() {
        return SUPPORTED.stream().sorted().map(c -> new Language(c, name(c))).toList();
    }
    public static String normalize(String code) {
        if (code == null || !code.matches("[a-zA-Z]{2}-[a-zA-Z]{2}"))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã ngôn ngữ phải có dạng vi-vn hoặc en-us");
        return code.toLowerCase(Locale.ROOT);
    }
    public static String requireSupported(String code) {
        String normalized = normalize(code);
        if (!SUPPORTED.contains(normalized)) throw new ApiException(HttpStatus.BAD_REQUEST,
                "Ngôn ngữ chưa được VoiceRSS hỗ trợ; xem GET /api/languages");
        return normalized;
    }
}
