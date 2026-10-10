package com.qraudio.backend.dto;

import jakarta.validation.constraints.Size;

public record UpdateExhibitRequest(@Size(max = 1000) String imageUrl, Boolean active) {
}
