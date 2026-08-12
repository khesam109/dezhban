package com.khesam.dezhban.service;

import com.khesam.dezhban.controller.dto.PageResponse;
import com.khesam.dezhban.controller.dto.UserDtos;
import com.khesam.dezhban.controller.error.ApiException;
import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.entity.UserPasswordCredentialEntity;
import com.khesam.dezhban.dataaccess.local.entity.UserProfileEntity;
import com.khesam.dezhban.dataaccess.local.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class UserManagementService {

    private final EndUserRepository endUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserPasswordCredentialRepository credentialRepository;
    private final OAuth2AuthorizationRepository authorizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public UserManagementService(
            EndUserRepository endUserRepository,
            UserProfileRepository userProfileRepository,
            UserPasswordCredentialRepository credentialRepository,
            OAuth2AuthorizationRepository authorizationRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager
    ) {
        this.endUserRepository = endUserRepository;
        this.userProfileRepository = userProfileRepository;
        this.credentialRepository = credentialRepository;
        this.authorizationRepository = authorizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDtos.Response> list(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.from(endUserRepository
                .findAll(PageRequest.of(Math.max(page, 0), safeSize, Sort.by("username")))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public UserDtos.Response get(String subject) {
        return toResponse(findBySubject(subject));
    }

    @Transactional
    public UserDtos.Response create(UserDtos.CreateRequest request) {
        String username = normalizeUsername(request.username());
        if (endUserRepository.existsByUsername(username)) {
            throw conflict("Username already exists");
        }
        validatePassword(request.password());

        EndUserEntity user = new EndUserEntity();
        user.setSubject(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEnabled(request.enabled());
        user.setAdmin(request.admin());
        user.setNotBefore(request.notBefore());
        endUserRepository.saveAndFlush(user);

        UserProfileEntity profile = new UserProfileEntity();
        profile.setEndUser(user);
        applyProfile(profile, request.profile());
        userProfileRepository.save(profile);

        UserPasswordCredentialEntity credential = new UserPasswordCredentialEntity();
        credential.setEndUser(user);
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        credentialRepository.save(credential);
        entityManager.flush();
        return toResponse(user);
    }

    @Transactional
    public UserDtos.Response replace(
            String subject,
            String ifMatch,
            UserDtos.ReplaceRequest request
    ) {
        EndUserEntity user = findBySubject(subject);
        requireMatchingVersion(ifMatch, user.getVersion(), "user");
        String username = normalizeUsername(request.username());
        if (!username.equals(user.getUsername()) && endUserRepository.existsByUsername(username)) {
            throw conflict("Username already exists");
        }
        user.setUsername(username);
        user.setEnabled(request.enabled());
        user.setAdmin(request.admin());
        user.setNotBefore(request.notBefore());
        UserProfileEntity profile = userProfileRepository.findById(user.getId())
                .orElseThrow(() -> notFound("User profile not found"));
        applyProfile(profile, request.profile());
        entityManager.flush();
        return toResponse(user);
    }

    @Transactional
    public UserDtos.Response patch(String subject, String ifMatch, JsonNode patch) {
        if (!patch.isObject()) {
            throw invalid("JSON merge patch must be an object");
        }
        EndUserEntity user = findBySubject(subject);
        requireMatchingVersion(ifMatch, user.getVersion(), "user");
        for (var field : patch.properties()) {
            switch (field.getKey()) {
                case "username" -> {
                    String username = normalizeUsername(requiredText(field.getValue(), "username"));
                    if (!username.equals(user.getUsername())
                            && endUserRepository.existsByUsername(username)) {
                        throw conflict("Username already exists");
                    }
                    user.setUsername(username);
                }
                case "enabled" -> user.setEnabled(requiredBoolean(field.getValue(), "enabled"));
                case "admin" -> user.setAdmin(requiredBoolean(field.getValue(), "admin"));
                case "notBefore" -> user.setNotBefore(
                        field.getValue().isNull()
                                ? null
                                : Instant.parse(requiredText(field.getValue(), "notBefore"))
                );
                default -> throw invalid("Unsupported user patch field: " + field.getKey());
            }
        }
        entityManager.flush();
        return toResponse(user);
    }

    @Transactional
    public void updatePassword(
            String subject,
            String ifMatch,
            UserDtos.PasswordRequest request
    ) {
        EndUserEntity user = findBySubject(subject);
        requireMatchingVersion(ifMatch, user.getVersion(), "user");
        validatePassword(request.password());
        UserPasswordCredentialEntity credential = credentialRepository.findById(user.getId())
                .orElseGet(() -> {
                    UserPasswordCredentialEntity value = new UserPasswordCredentialEntity();
                    value.setEndUser(user);
                    return value;
                });
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        credential.setExpiresAt(request.expiresAt());
        credentialRepository.save(credential);
        entityManager.lock(user, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    @Transactional
    public void delete(String subject, String ifMatch) {
        EndUserEntity user = findBySubject(subject);
        requireMatchingVersion(ifMatch, user.getVersion(), "user");
        authorizationRepository.deleteAllByPrincipalName(user.getUsername());
        endUserRepository.delete(user);
    }

    private EndUserEntity findBySubject(String subject) {
        return endUserRepository.findBySubject(subject)
                .orElseThrow(() -> notFound("User not found"));
    }

    private UserDtos.Response toResponse(EndUserEntity user) {
        UserProfileEntity profile = userProfileRepository.findById(user.getId()).orElse(null);
        return new UserDtos.Response(
                user.getSubject(),
                user.getUsername(),
                user.isEnabled(),
                user.isAdmin(),
                user.isLocked(),
                user.getLockUntil(),
                user.getNotBefore(),
                user.getFailedLoginAttempts(),
                user.getFailedLoginAt(),
                user.getCreatedAt(),
                user.getModifiedAt(),
                user.getVersion(),
                profile == null ? null : toProfileResponse(profile)
        );
    }

    private UserDtos.ProfileResponse toProfileResponse(UserProfileEntity profile) {
        return new UserDtos.ProfileResponse(
                profile.getFirstName(),
                profile.getLastName(),
                profile.getGender(),
                profile.getNationalCode(),
                profile.getIdNumber(),
                profile.getBirthDate(),
                profile.getFatherName(),
                profile.getLatinFirstName(),
                profile.getLatinLastName(),
                profile.getLatinFatherName(),
                profile.getNationality(),
                profile.getPostalCode(),
                profile.getProvince(),
                profile.getCity(),
                profile.getAddress(),
                profile.getMobileNumber(),
                profile.getMobileNumberVerified(),
                profile.getPhoneNumber(),
                profile.getEmail(),
                profile.getEmailVerified()
        );
    }

    private void applyProfile(UserProfileEntity profile, UserDtos.ProfileRequest request) {
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setGender(request.gender());
        profile.setNationalCode(request.nationalCode());
        profile.setIdNumber(request.idNumber());
        profile.setBirthDate(request.birthDate());
        profile.setFatherName(request.fatherName());
        profile.setLatinFirstName(request.latinFirstName());
        profile.setLatinLastName(request.latinLastName());
        profile.setLatinFatherName(request.latinFatherName());
        profile.setNationality(request.nationality());
        profile.setPostalCode(request.postalCode());
        profile.setProvince(request.province());
        profile.setCity(request.city());
        profile.setAddress(request.address());
        profile.setMobileNumber(request.mobileNumber());
        profile.setPhoneNumber(request.phoneNumber());
        profile.setEmail(request.email() == null ? null : request.email().toLowerCase());
    }

    private String normalizeUsername(String username) {
        return username.trim();
    }

    private void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalid("Password must not exceed BCrypt's 72-byte input limit");
        }
    }

    private void requireMatchingVersion(String ifMatch, long version, String type) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ApiException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "PRECONDITION_REQUIRED",
                    "If-Match is required"
            );
        }
        if (!etag(type, version).equals(ifMatch)) {
            throw new ApiException(
                    HttpStatus.PRECONDITION_FAILED,
                    "PRECONDITION_FAILED",
                    "Resource version does not match"
            );
        }
    }

    public String etag(UserDtos.Response response) {
        return etag("user", response.version());
    }

    private String etag(String type, long version) {
        return '"' + type + "-" + version + '"';
    }

    private String requiredText(JsonNode value, String field) {
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw invalid(field + " must be a non-empty string");
        }
        return value.asString();
    }

    private boolean requiredBoolean(JsonNode value, String field) {
        if (value == null || !value.isBoolean()) {
            throw invalid(field + " must be a boolean");
        }
        return value.asBoolean();
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_RESOURCE", message);
    }
}
