package com.qraudio.backend.service;

import com.qraudio.backend.entity.*;
import com.qraudio.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataService {
    private final ExhibitRepository exhibits;
    private final ExhibitTranslationRepository translations;
    private final QrCodeRepository codes;
    public DemoDataService(ExhibitRepository exhibits, ExhibitTranslationRepository translations, QrCodeRepository codes) {
        this.exhibits = exhibits; this.translations = translations; this.codes = codes;
    }
    @Transactional
    public void seed() {
        add("DEMO-OIL-LAMP", "lamp", "Đèn dầu", "Oil lamp",
                "Đèn dầu có bầu chứa nhiên liệu và bấc dẫn dầu lên ngọn lửa. Hình minh họa thể hiện phần thân, tay cầm và miệng đèn.",
                "An oil lamp has a fuel reservoir and a wick that carries oil to the flame. The illustration shows its body, handle and opening.");
        add("DEMO-BRONZE-DRUM", "drum", "Trống đồng", "Bronze drum",
                "Hình minh họa trống đồng có các vòng hoa văn bao quanh ngôi sao ở giữa mặt trống. Các họa tiết lặp lại tạo nên bố cục cân đối.",
                "The bronze drum illustration has concentric patterns surrounding a star at the centre. Repeated motifs give its surface a balanced composition.");
        add("DEMO-CERAMIC-VASE", "vase", "Bình gốm", "Ceramic vase",
                "Bình gốm trong hình có miệng rộng, cổ hẹp, thân phình và chân đế. Đường cong cùng hoa văn làm nổi bật hình dáng của bình.",
                "The ceramic vase illustration has a wide rim, narrow neck, rounded body and foot. Curved lines and decoration emphasize its shape.");
    }
    private void add(String code, String image, String viTitle, String enTitle, String vi, String en) {
        // The QR is the seed marker: never overwrite an existing or disabled demonstration item.
        if (codes.findByCode(code).isPresent()) return;
        Exhibit exhibit = new Exhibit(); exhibit.setActive(true); exhibit.setImageUrl("/images/" + image + ".svg");
        exhibits.saveAndFlush(exhibit);
        translation(exhibit.getId(), "vi-vn", viTitle, vi);
        translation(exhibit.getId(), "en-us", enTitle, en);
        QrCode qr = new QrCode(); qr.setCode(code); qr.setExhibitId(exhibit.getId()); qr.setActive(true);
        codes.saveAndFlush(qr);
    }
    private void translation(Long id, String code, String title, String content) {
        ExhibitTranslation t = new ExhibitTranslation(); t.setExhibitId(id); t.setLanguageCode(code);
        t.setTitle(title); t.setContent(content); translations.save(t);
    }
}
