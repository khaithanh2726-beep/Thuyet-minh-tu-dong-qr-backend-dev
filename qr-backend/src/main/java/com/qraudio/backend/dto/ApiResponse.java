package com.qraudio.backend.dto;

import java.time.LocalDateTime;

public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse(int status, String message, T data) {
        this.status = status;                   // Mã trạng thái
        this.message = message;                 // Thông báo xuất ra
        this.data = data;                       // Dữ liệu xuất ra
        this.timestamp = LocalDateTime.now();   // Thời điểm phản hồi
    }

    // Hàm gọi khi API thành công với dữ liệu xuất ra
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Thành công", data);
    }
    
    // Hàm gọi khi API thành công với thông báo xuất ra
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    // Hàm gọi khi API thất bại với thông báo lỗi xuất ra
    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public LocalDateTime getTimestamp() { return timestamp; }
}