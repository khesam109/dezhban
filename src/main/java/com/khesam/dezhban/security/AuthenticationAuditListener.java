package com.khesam.dezhban.security;

import com.khesam.dezhban.security.AuthenticationAttemptService.RequestDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuthenticationAuditListener.class);

    private final AuthenticationAttemptService authenticationAttemptService;

    public AuthenticationAuditListener(
            AuthenticationAttemptService authenticationAttemptService
    ) {
        this.authenticationAttemptService = authenticationAttemptService;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        try {
            Authentication authentication = event.getAuthentication();
            if (authentication instanceof OAuth2ClientAuthenticationToken clientAuthentication) {
                authenticationAttemptService.recordClientSuccess(
                        clientAuthentication.getName(),
                        clientAuthentication.getClientAuthenticationMethod().getValue(),
                        requestDetails(authentication)
                );
            } else if (authentication instanceof
                    org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
                authenticationAttemptService.recordUserSuccess(
                        authentication.getName(),
                        requestDetails(authentication)
                );
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Could not persist authentication success audit event", exception);
        }
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        try {
            Authentication authentication = event.getAuthentication();
            if (authentication instanceof OAuth2ClientAuthenticationToken clientAuthentication) {
                if (event instanceof AuthenticationFailureBadCredentialsEvent) {
                    authenticationAttemptService.recordClientBadCredentials(
                            clientAuthentication.getName(),
                            clientAuthentication.getClientAuthenticationMethod().getValue(),
                            requestDetails(authentication)
                    );
                }
                return;
            }

            if (authentication instanceof
                    org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
                if (event instanceof AuthenticationFailureBadCredentialsEvent) {
                    authenticationAttemptService.recordUserBadCredentials(
                            authentication.getName(),
                            requestDetails(authentication)
                    );
                } else {
                    authenticationAttemptService.recordUserRejection(
                            authentication.getName(),
                            event.getException().getClass().getSimpleName(),
                            event instanceof AuthenticationFailureLockedEvent,
                            requestDetails(authentication)
                    );
                }
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Could not persist authentication failure audit event", exception);
        }
    }

    private RequestDetails requestDetails(Authentication authentication) {
        if (authentication.getDetails() instanceof WebAuthenticationDetails details) {
            return new RequestDetails(details.getRemoteAddress(), details.getSessionId());
        }
        return RequestDetails.EMPTY;
    }
}
