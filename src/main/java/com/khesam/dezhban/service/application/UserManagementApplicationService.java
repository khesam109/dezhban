package com.khesam.dezhban.service.application;

import com.khesam.dezhban.controller.dto.PageResponse;
import com.khesam.dezhban.controller.dto.UserDtos;
import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.entity.UserProfileEntity;
import com.khesam.dezhban.service.domain.authorization.OAuth2AuthorizationDomainService;
import com.khesam.dezhban.service.domain.support.DomainException;
import com.khesam.dezhban.service.domain.user.EndUserDomainService;
import com.khesam.dezhban.service.domain.user.UserPasswordCredentialDomainService;
import com.khesam.dezhban.service.domain.user.UserProfileDomainService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

@Service
public class UserManagementApplicationService {

    private final EndUserDomainService endUserDomainService;
    private final UserProfileDomainService userProfileDomainService;
    private final UserPasswordCredentialDomainService credentialDomainService;
    private final OAuth2AuthorizationDomainService authorizationDomainService;

    public UserManagementApplicationService(
            EndUserDomainService endUserDomainService,
            UserProfileDomainService userProfileDomainService,
            UserPasswordCredentialDomainService credentialDomainService,
            OAuth2AuthorizationDomainService authorizationDomainService
    ) {
        this.endUserDomainService = endUserDomainService;
        this.userProfileDomainService = userProfileDomainService;
        this.credentialDomainService = credentialDomainService;
        this.authorizationDomainService = authorizationDomainService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDtos.Response> list(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.from(endUserDomainService
                .list(PageRequest.of(Math.max(page, 0), safeSize, Sort.by("username")))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public UserDtos.Response get(String subject) {
        return toResponse(endUserDomainService.requireBySubject(subject));
    }

    @Transactional
    public UserDtos.Response create(UserDtos.CreateRequest request) {
        String username = endUserDomainService.normalizeUsername(request.username());
        EndUserEntity user = endUserDomainService.create(
                username,
                request.enabled(),
                request.admin(),
                request.notBefore()
        );
        userProfileDomainService.create(user, toProfileData(request.profile()));
        credentialDomainService.create(user, request.password());
        endUserDomainService.flush();
        return toResponse(user);
    }

    @Transactional
    public UserDtos.Response replace(
            String subject,
            String ifMatch,
            UserDtos.ReplaceRequest request
    ) {
        EndUserEntity user = endUserDomainService.requireBySubject(subject);
        ResourceVersionGuard.requireMatchingVersion(ifMatch, etag(user.getVersion()));
        endUserDomainService.updateIdentity(
                user,
                endUserDomainService.normalizeUsername(request.username()),
                request.enabled(),
                request.admin(),
                request.notBefore()
        );
        UserProfileEntity profile = userProfileDomainService.requireByUserId(user.getId());
        userProfileDomainService.replace(profile, toProfileData(request.profile()));
        endUserDomainService.flush();
        return toResponse(user);
    }

    @Transactional
    public UserDtos.Response patch(String subject, String ifMatch, JsonNode patch) {
        if (!patch.isObject()) {
            throw DomainException.invalid("JSON merge patch must be an object");
        }
        EndUserEntity user = endUserDomainService.requireBySubject(subject);
        ResourceVersionGuard.requireMatchingVersion(ifMatch, etag(user.getVersion()));
        for (var field : patch.properties()) {
            switch (field.getKey()) {
                case "username" -> endUserDomainService.patchUsername(
                        user,
                        endUserDomainService.normalizeUsername(requiredText(field.getValue(), "username"))
                );
                case "enabled" -> endUserDomainService.patchEnabled(
                        user,
                        requiredBoolean(field.getValue(), "enabled")
                );
                case "admin" -> endUserDomainService.patchAdmin(
                        user,
                        requiredBoolean(field.getValue(), "admin")
                );
                case "notBefore" -> endUserDomainService.patchNotBefore(
                        user,
                        field.getValue().isNull()
                                ? null
                                : Instant.parse(requiredText(field.getValue(), "notBefore"))
                );
                default -> throw DomainException.invalid("Unsupported user patch field: " + field.getKey());
            }
        }
        endUserDomainService.flush();
        return toResponse(user);
    }

    @Transactional
    public void updatePassword(
            String subject,
            String ifMatch,
            UserDtos.PasswordRequest request
    ) {
        EndUserEntity user = endUserDomainService.requireBySubject(subject);
        ResourceVersionGuard.requireMatchingVersion(ifMatch, etag(user.getVersion()));
        credentialDomainService.updatePassword(user, request.password(), request.expiresAt());
        endUserDomainService.forceIncrementVersion(user);
    }

    @Transactional
    public void delete(String subject, String ifMatch) {
        EndUserEntity user = endUserDomainService.requireBySubject(subject);
        ResourceVersionGuard.requireMatchingVersion(ifMatch, etag(user.getVersion()));
        authorizationDomainService.revokeByPrincipalName(user.getUsername());
        endUserDomainService.delete(user);
    }

    public String etag(UserDtos.Response response) {
        return etag(response.version());
    }

    private String etag(long version) {
        return ResourceVersionGuard.etag("user", version);
    }

    private UserDtos.Response toResponse(EndUserEntity user) {
        UserProfileEntity profile = userProfileDomainService.findByUserId(user.getId()).orElse(null);
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

    private UserProfileDomainService.ProfileData toProfileData(UserDtos.ProfileRequest request) {
        return new UserProfileDomainService.ProfileData(
                request.firstName(),
                request.lastName(),
                request.gender(),
                request.nationalCode(),
                request.idNumber(),
                request.birthDate(),
                request.fatherName(),
                request.latinFirstName(),
                request.latinLastName(),
                request.latinFatherName(),
                request.nationality(),
                request.postalCode(),
                request.province(),
                request.city(),
                request.address(),
                request.mobileNumber(),
                request.phoneNumber(),
                request.email()
        );
    }

    private String requiredText(JsonNode value, String field) {
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw DomainException.invalid(field + " must be a non-empty string");
        }
        return value.asString();
    }

    private boolean requiredBoolean(JsonNode value, String field) {
        if (value == null || !value.isBoolean()) {
            throw DomainException.invalid(field + " must be a boolean");
        }
        return value.asBoolean();
    }
}
