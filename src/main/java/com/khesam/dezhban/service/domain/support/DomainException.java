package com.khesam.dezhban.service.domain.support;

public class DomainException extends RuntimeException {

    public enum Kind {
        NOT_FOUND,
        CONFLICT,
        INVALID
    }

    private final Kind kind;
    private final String code;

    public DomainException(Kind kind, String code, String message) {
        super(message);
        this.kind = kind;
        this.code = code;
    }

    public static DomainException notFound(String message) {
        return new DomainException(Kind.NOT_FOUND, "NOT_FOUND", message);
    }

    public static DomainException conflict(String message) {
        return new DomainException(Kind.CONFLICT, "CONFLICT", message);
    }

    public static DomainException invalid(String message) {
        return new DomainException(Kind.INVALID, "INVALID_RESOURCE", message);
    }

    public Kind getKind() {
        return kind;
    }

    public String getCode() {
        return code;
    }
}
