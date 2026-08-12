package com.khesam.dezhban.controller;

import com.khesam.dezhban.controller.dto.ClientDtos;
import com.khesam.dezhban.controller.dto.PageResponse;
import com.khesam.dezhban.service.application.ClientManagementApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/clients")
public class OAuthClientManagementController {

    private final ClientManagementApplicationService clientManagementService;

    public OAuthClientManagementController(ClientManagementApplicationService clientManagementService) {
        this.clientManagementService = clientManagementService;
    }

    @GetMapping
    PageResponse<ClientDtos.Response> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return clientManagementService.list(page, size);
    }

    @PostMapping
    ResponseEntity<ClientDtos.CreatedResponse> create(
            @Valid @RequestBody ClientDtos.CreateRequest request
    ) {
        ClientDtos.CreatedResponse response = clientManagementService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{clientId}")
                .buildAndExpand(response.client().clientId())
                .toUri();
        return ResponseEntity.created(location)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .eTag(clientManagementService.etag(response.client()))
                .body(response);
    }

    @GetMapping("/{clientId}")
    ResponseEntity<ClientDtos.Response> get(@PathVariable String clientId) {
        ClientDtos.Response response = clientManagementService.get(clientId);
        return ResponseEntity.ok()
                .eTag(clientManagementService.etag(response))
                .body(response);
    }

    @PutMapping("/{clientId}")
    ResponseEntity<ClientDtos.Response> replace(
            @PathVariable String clientId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ClientDtos.ReplaceRequest request
    ) {
        ClientDtos.Response response =
                clientManagementService.replace(clientId, ifMatch, request);
        return ResponseEntity.ok()
                .eTag(clientManagementService.etag(response))
                .body(response);
    }

    @PatchMapping(
            value = "/{clientId}",
            consumes = "application/merge-patch+json"
    )
    ResponseEntity<ClientDtos.Response> patch(
            @PathVariable String clientId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody JsonNode patch
    ) {
        ClientDtos.Response response =
                clientManagementService.patch(clientId, ifMatch, patch);
        return ResponseEntity.ok()
                .eTag(clientManagementService.etag(response))
                .body(response);
    }

    @PostMapping("/{clientId}/secret-rotations")
    ResponseEntity<ClientDtos.SecretResponse> rotateSecret(
            @PathVariable String clientId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ClientDtos.SecretRotationRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(clientManagementService.rotateSecret(clientId, ifMatch, request));
    }

    @DeleteMapping("/{clientId}")
    ResponseEntity<Void> delete(
            @PathVariable String clientId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        clientManagementService.delete(clientId, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
