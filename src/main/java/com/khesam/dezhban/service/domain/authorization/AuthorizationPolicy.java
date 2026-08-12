package com.khesam.dezhban.service.domain.authorization;

import com.khesam.dezhban.common.ClientType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class AuthorizationPolicy {

    public static final String END_USER = "END_USER";
    public static final String ADMIN = "ADMIN";
    public static final Set<String> PROTOCOL_SCOPES =
            Set.of("openid", "profile", "offline_access");
    public static final Set<String> CLIENT_ACTOR_ROLES =
            Set.of("AP", "RO", "MOBILE_FIRST_PARTY", "MOBILE_THIRD_PARTY", "ADMIN_PANEL");

    private static final Map<ClientType, Set<String>> TYPE_SCOPES = new EnumMap<>(ClientType.class);

    static {
        TYPE_SCOPES.put(ClientType.AP, Set.of("sign_request:create", "sign_request:read"));
        TYPE_SCOPES.put(ClientType.RO, Set.of(
                "kyc:perform",
                "certificate_order:create",
                "certificate_order:read"
        ));
        TYPE_SCOPES.put(ClientType.MOBILE_FIRST_PARTY, Set.of(
                "identity:register",
                "certificate:issue",
                "certificate_order:complete",
                "sign_request:read",
                "sign_request:sign"
        ));
        TYPE_SCOPES.put(ClientType.MOBILE_THIRD_PARTY, Set.of("sign_request:read"));
        TYPE_SCOPES.put(ClientType.ADMIN_PANEL, Set.of("system:admin"));
    }

    private AuthorizationPolicy() {
    }

    public static String actorRole(ClientType type) {
        return type.name();
    }

    public static Set<String> businessScopes(ClientType type) {
        return TYPE_SCOPES.get(type);
    }
}
