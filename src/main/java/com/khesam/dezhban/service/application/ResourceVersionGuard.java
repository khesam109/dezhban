package com.khesam.dezhban.service.application;

import com.khesam.dezhban.controller.error.ApiException;
import org.springframework.http.HttpStatus;

final class ResourceVersionGuard {

    private ResourceVersionGuard() {
    }

    static void requireMatchingVersion(String ifMatch, String expectedEtag) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ApiException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "PRECONDITION_REQUIRED",
                    "If-Match is required"
            );
        }
        if (!expectedEtag.equals(ifMatch)) {
            throw new ApiException(
                    HttpStatus.PRECONDITION_FAILED,
                    "PRECONDITION_FAILED",
                    "Resource version does not match"
            );
        }
    }

    static String etag(String type, long version) {
        return '"' + type + "-" + version + '"';
    }
}
