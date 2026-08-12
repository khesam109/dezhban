package com.khesam.dezhban.controller.dto;

import com.khesam.dezhban.common.Gender;
import com.khesam.dezhban.common.Nationality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record ProfileRequest(
            @NotBlank @Size(max = 255) String firstName,
            @NotBlank @Size(max = 255) String lastName,
            @NotNull Gender gender,
            @NotBlank @Pattern(regexp = "\\d{10}") String nationalCode,
            @Size(max = 10) String idNumber,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String birthDate,
            @Size(max = 255) String fatherName,
            @Size(max = 255) String latinFirstName,
            @Size(max = 255) String latinLastName,
            @Size(max = 255) String latinFatherName,
            @NotNull Nationality nationality,
            @Size(max = 10) String postalCode,
            @Size(max = 50) String province,
            @Size(max = 50) String city,
            @Size(max = 255) String address,
            @NotBlank @Size(max = 14) String mobileNumber,
            @Size(max = 14) String phoneNumber,
            @Email @Size(max = 100) String email
    ) {
    }

    public record CreateRequest(
            @NotBlank @Size(min = 3, max = 255) String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            boolean enabled,
            boolean admin,
            Instant notBefore,
            @NotNull @Valid ProfileRequest profile
    ) {
    }

    public record ReplaceRequest(
            @NotBlank @Size(min = 3, max = 255) String username,
            boolean enabled,
            boolean admin,
            Instant notBefore,
            @NotNull @Valid ProfileRequest profile
    ) {
    }

    public record PasswordRequest(
            @NotBlank @Size(min = 8, max = 72) String password,
            Instant expiresAt
    ) {
    }

    public record ProfileResponse(
            String firstName,
            String lastName,
            Gender gender,
            String nationalCode,
            String idNumber,
            String birthDate,
            String fatherName,
            String latinFirstName,
            String latinLastName,
            String latinFatherName,
            Nationality nationality,
            String postalCode,
            String province,
            String city,
            String address,
            String mobileNumber,
            Boolean mobileNumberVerified,
            String phoneNumber,
            String email,
            Boolean emailVerified
    ) {
    }

    public record Response(
            String subject,
            String username,
            boolean enabled,
            boolean admin,
            boolean locked,
            Instant lockUntil,
            Instant notBefore,
            int failedLoginAttempts,
            Instant failedLoginAt,
            Instant createdAt,
            Instant modifiedAt,
            long version,
            ProfileResponse profile
    ) {
    }
}
