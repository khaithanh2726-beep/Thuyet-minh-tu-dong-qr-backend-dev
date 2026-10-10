package com.qraudio.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1, max = 60)
        @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
        @Size(min = 8, max = 72) String password,
        @Pattern(regexp = "ADMIN") String role,
        Boolean active) {
}
