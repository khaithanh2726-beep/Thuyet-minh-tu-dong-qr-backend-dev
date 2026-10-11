package com.qraudio.backend.dto;
import jakarta.validation.constraints.Positive;
public record UpdateQrRequest(@Positive Long exhibitId, Boolean active) {}
