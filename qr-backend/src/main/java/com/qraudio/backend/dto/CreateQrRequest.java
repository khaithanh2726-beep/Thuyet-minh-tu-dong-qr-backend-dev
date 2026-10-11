package com.qraudio.backend.dto;
import jakarta.validation.constraints.*;
public record CreateQrRequest(@NotNull @Positive Long exhibitId,
        @Pattern(regexp = "[A-Za-z0-9_-]{3,80}") String code, Boolean active) {}
