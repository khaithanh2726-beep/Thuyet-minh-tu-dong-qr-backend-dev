package com.qraudio.backend.service;

import com.qraudio.backend.dto.*;
import com.qraudio.backend.entity.QrCode;
import com.qraudio.backend.exception.*;
import com.qraudio.backend.repository.*;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.util.*;

@Service
public class AdminQrService {
    private final QrCodeRepository codes;
    private final ExhibitRepository exhibits;
    private final PublicUrlService urls;
    private final QrCodeService publicQr;
    public AdminQrService(QrCodeRepository codes, ExhibitRepository exhibits, PublicUrlService urls, QrCodeService publicQr) {
        this.codes = codes; this.exhibits = exhibits; this.urls = urls; this.publicQr = publicQr;
    }
    @Transactional(readOnly = true)
    public List<QrResponse> list() { return codes.findAll(Sort.by("id")).stream().map(this::response).toList(); }
    @Transactional(readOnly = true)
    public QrResponse get(Long id) { return response(find(id)); }
    @Transactional
    public QrResponse create(CreateQrRequest request) {
        requireExhibit(request.exhibitId());
        String code = request.code() == null ? UUID.randomUUID().toString().toUpperCase(Locale.ROOT) : request.code().toUpperCase(Locale.ROOT);
        if (codes.findByCode(code).isPresent()) throw new ApiException(HttpStatus.CONFLICT, "Mã QR đã tồn tại");
        QrCode qr = new QrCode(); qr.setExhibitId(request.exhibitId()); qr.setCode(code);
        qr.setActive(request.active() == null || request.active());
        return response(codes.saveAndFlush(qr));
    }
    @Transactional
    public QrResponse update(Long id, UpdateQrRequest request) {
        if (request.exhibitId() == null && request.active() == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chưa có thông tin cần cập nhật");
        QrCode qr = find(id);
        if (request.exhibitId() != null) { requireExhibit(request.exhibitId()); qr.setExhibitId(request.exhibitId()); }
        if (request.active() != null) qr.setActive(request.active());
        return response(codes.saveAndFlush(qr));
    }
    @Transactional
    public void deactivate(Long id) { QrCode qr = find(id); qr.setActive(false); codes.saveAndFlush(qr); }
    public byte[] adminImage(Long id) { return image(find(id).getCode()); }
    public byte[] publicImage(String code) {
        QrCode qr = publicQr.findActive(code); requireActiveExhibit(qr.getExhibitId()); return image(qr.getCode());
    }
    private void requireExhibit(Long id) {
        if (!exhibits.existsById(id)) throw new ResourceNotFoundException("Không tìm thấy hiện vật");
    }
    private void requireActiveExhibit(Long id) {
        if (!exhibits.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hiện vật")).isActive())
            throw new ResourceNotFoundException("Hiện vật hiện không được trưng bày");
    }
    private QrCode find(Long id) { return codes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã QR")); }
    private QrResponse response(QrCode qr) {
        return new QrResponse(qr.getId(), qr.getExhibitId(), qr.getCode(), qr.isActive(), qr.getCreatedAt(),
                urls.qrMetadata(qr.getCode()), "/api/admin/qrcodes/" + qr.getId() + "/image");
    }
    private byte[] image(String code) {
        try {
            var matrix = new MultiFormatWriter().encode(urls.qrMetadata(code), BarcodeFormat.QR_CODE, 480, 480,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 4));
            var output = new ByteArrayOutputStream(); MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được ảnh QR");
        }
    }
}
