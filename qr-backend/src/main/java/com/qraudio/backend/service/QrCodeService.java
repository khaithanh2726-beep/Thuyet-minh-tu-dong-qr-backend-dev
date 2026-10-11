package com.qraudio.backend.service;

import com.qraudio.backend.dto.ExhibitResponse;
import com.qraudio.backend.dto.QrInfoResponse;
import com.qraudio.backend.entity.QrCode;
import com.qraudio.backend.exception.ResourceNotFoundException;
import com.qraudio.backend.repository.QrCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QrCodeService {
    private final QrCodeRepository qrCodeRepository;
    private final ExhibitService exhibitService;

    public QrCodeService(QrCodeRepository qrCodeRepository, ExhibitService exhibitService) {
        this.qrCodeRepository = qrCodeRepository;
        this.exhibitService = exhibitService;
    }

    public ExhibitResponse resolveQrCode(String code, String lang) {
        return exhibitService.getExhibitByIdAndLang(findActive(code).getExhibitId(), lang);
    }

    public Resource getAudio(String code, String lang) {
        return exhibitService.getAudio(findActive(code).getExhibitId(), lang);
    }

    public String requireAudioLanguage(String code, String lang) {
        return Languages.requireSupported(exhibitService.requireAudioTranslation(
                findActive(code).getExhibitId(), lang).getLanguageCode());
    }

    @Transactional(readOnly = true)
    public QrInfoResponse getInfo(String code) {
        QrCode qr = findActive(code);
        return new QrInfoResponse(qr.getCode(), exhibitService.getInfo(qr.getExhibitId()));
    }

    public QrCode findActive(String code) {
        QrCode qrCode = qrCodeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Mã QR không hợp lệ hoặc không tồn tại."));

        if (!qrCode.isActive()) {
            throw new ResourceNotFoundException("Mã QR này hiện đang bị vô hiệu hóa.");
        }

        return qrCode;
    }
}
