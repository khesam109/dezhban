package com.khesam.dezhban.controller;

import com.khesam.dezhban.controller.dto.WebAuthnDtos;
import com.khesam.dezhban.service.application.WebAuthnApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webauthn")
public class WebAuthnController {

    private final WebAuthnApplicationService webAuthnService;

    public WebAuthnController(WebAuthnApplicationService webAuthnService) {
        this.webAuthnService = webAuthnService;
    }

    @PostMapping("/registration/options")
    public ResponseEntity<WebAuthnDtos.RegistrationOptionsResponse> registrationOptions(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody(required = false) WebAuthnDtos.RegistrationOptionsRequest request
    ) {
        String label = request == null ? null : request.label();
        return ResponseEntity.ok(webAuthnService.startRegistration(principal.getUsername(), label));
    }

    @PostMapping("/registration/finish")
    public ResponseEntity<WebAuthnDtos.CredentialResponse> finishRegistration(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody WebAuthnDtos.FinishRegistrationRequest request
    ) {
        return ResponseEntity.ok(webAuthnService.finishRegistration(
                principal.getUsername(),
                request.challengeId(),
                request.credential(),
                request.label()
        ));
    }

    @GetMapping("/credentials")
    public List<WebAuthnDtos.CredentialResponse> listCredentials(@AuthenticationPrincipal UserDetails principal) {
        return webAuthnService.listCredentials(principal.getUsername());
    }

    @DeleteMapping("/credentials/{credentialId}")
    public ResponseEntity<Void> deleteCredential(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String credentialId
    ) {
        webAuthnService.deleteCredential(principal.getUsername(), credentialId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/authentication/options")
    public ResponseEntity<WebAuthnDtos.AuthenticationOptionsResponse> authenticationOptions(
            @Valid @RequestBody WebAuthnDtos.AuthenticationOptionsRequest request,
            HttpServletRequest servletRequest
    ) {
        servletRequest.getSession(true).setAttribute("WEBAUTHN_USERNAME", request.username());
        return ResponseEntity.ok(webAuthnService.startAuthentication(request.username()));
    }

    @PostMapping("/authentication/finish")
    public ResponseEntity<Map<String, Object>> finishAuthentication(
            @Valid @RequestBody WebAuthnDtos.FinishAuthenticationRequest request,
            HttpServletRequest servletRequest
    ) {
        String username = (String) servletRequest.getSession(true).getAttribute("WEBAUTHN_USERNAME");
        Authentication authentication = webAuthnService.finishAuthentication(
                username,
                request.challengeId(),
                request.credential(),
                servletRequest
        );
        servletRequest.getSession(true).removeAttribute("WEBAUTHN_USERNAME");
        return ResponseEntity.ok(Map.of(
                "status", "authenticated",
                "username", authentication.getName()
        ));
    }
}
