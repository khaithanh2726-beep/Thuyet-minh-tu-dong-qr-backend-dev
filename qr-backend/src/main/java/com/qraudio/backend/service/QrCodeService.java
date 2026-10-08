package com.qraudio.backend.service;

import com.qraudio.backend.dto.ExhibitResponse;
import com.qraudio.backend.entity.QrCode;
import com.qraudio.backend.exception.ResourceNotFoundException;
import com.qraudio.backend.repository.QrCodeRepository;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {
    private final QrCodeRepository qrCodeRepository;
    private final ExhibitService exhibitService; // Tái sử dụng service hiện vật

    public QrCodeService(QrCodeRepository qrCodeRepository, ExhibitService exhibitService) {
        this.qrCodeRepository = qrCodeRepository;
        this.exhibitService = exhibitService;
    }

    // Hàm nhận chuỗi mã QR -> kiểm tra -> trả về thông tin hiện vật
    public ExhibitResponse resolveQrCode(String code, String lang) {
        // Tìm chuỗi mã QR
        QrCode qrCode = qrCodeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Mã QR không hợp lệ hoặc không tồn tại."));

        // Kiểm tra mã QR
        if (!qrCode.isActive()) {
            throw new ResourceNotFoundException("Mã QR này hiện đang bị vô hiệu hóa.");
        }

        // Lấy Id hiện vật và bản dịch
        return exhibitService.getExhibitByIdAndLang(qrCode.getExhibitId(), lang);
    }
}