package com.khesam.dezhban.controller;

import com.khesam.dezhban.controller.dto.PageResponse;
import com.khesam.dezhban.controller.dto.UserDtos;
import com.khesam.dezhban.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    PageResponse<UserDtos.Response> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userManagementService.list(page, size);
    }

    @PostMapping
    ResponseEntity<UserDtos.Response> create(
            @Valid @RequestBody UserDtos.CreateRequest request
    ) {
        UserDtos.Response response = userManagementService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{subject}")
                .buildAndExpand(response.subject())
                .toUri();
        return ResponseEntity.created(location)
                .eTag(userManagementService.etag(response))
                .body(response);
    }

    @GetMapping("/{subject}")
    ResponseEntity<UserDtos.Response> get(@PathVariable String subject) {
        UserDtos.Response response = userManagementService.get(subject);
        return ResponseEntity.ok()
                .eTag(userManagementService.etag(response))
                .body(response);
    }

    @PutMapping("/{subject}")
    ResponseEntity<UserDtos.Response> replace(
            @PathVariable String subject,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UserDtos.ReplaceRequest request
    ) {
        UserDtos.Response response = userManagementService.replace(subject, ifMatch, request);
        return ResponseEntity.ok()
                .eTag(userManagementService.etag(response))
                .body(response);
    }

    @PatchMapping(
            value = "/{subject}",
            consumes = "application/merge-patch+json"
    )
    ResponseEntity<UserDtos.Response> patch(
            @PathVariable String subject,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode patch
    ) {
        UserDtos.Response response = userManagementService.patch(subject, ifMatch, patch);
        return ResponseEntity.ok()
                .eTag(userManagementService.etag(response))
                .body(response);
    }

    @PutMapping("/{subject}/password")
    ResponseEntity<Void> updatePassword(
            @PathVariable String subject,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UserDtos.PasswordRequest request
    ) {
        userManagementService.updatePassword(subject, ifMatch, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{subject}")
    ResponseEntity<Void> delete(
            @PathVariable String subject,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        userManagementService.delete(subject, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
