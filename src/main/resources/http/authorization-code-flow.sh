#!/usr/bin/env bash
set -euo pipefail

# Development credentials seeded by 007-populate-development-data.sql:
#   user: user / salam
#   client: client / secret
#
# 1. Open this URL in a browser and sign in with the user credentials:
# http://localhost:8585/oauth2/authorize?response_type=code&client_id=client&redirect_uri=http%3A%2F%2F127.0.0.1%3A8585%2Fcallback.html&scope=openid&state=dezhban-example
#
# 2. Copy the code displayed by callback.html, then run:
# AUTHORIZATION_CODE='<code>' bash src/main/resources/http/authorization-code-flow.sh

: "${AUTHORIZATION_CODE:?Set AUTHORIZATION_CODE to the code returned by the authorization endpoint}"

AUTHORIZATION_SERVER="${AUTHORIZATION_SERVER:-http://localhost:8585}"
CLIENT_ID="${CLIENT_ID:-client}"
CLIENT_SECRET="${CLIENT_SECRET:-secret}"
REDIRECT_URI="${REDIRECT_URI:-http://127.0.0.1:8585/callback.html}"

TOKEN_RESPONSE="$(
    curl --fail-with-body --silent --show-error \
        --user "${CLIENT_ID}:${CLIENT_SECRET}" \
        --request POST "${AUTHORIZATION_SERVER}/oauth2/token" \
        --header "Content-Type: application/x-www-form-urlencoded" \
        --data-urlencode "grant_type=authorization_code" \
        --data-urlencode "code=${AUTHORIZATION_CODE}" \
        --data-urlencode "redirect_uri=${REDIRECT_URI}"
)"

printf 'Token response:\n%s\n' "${TOKEN_RESPONSE}"
printf '\nUse access_token from that response to call UserInfo:\n'
printf '%s\n' \
    "curl --fail-with-body --header 'Authorization: Bearer <access_token>' '${AUTHORIZATION_SERVER}/userinfo'"
printf '\nUse refresh_token to obtain a replacement token:\n'
printf '%s\n' \
    "curl --fail-with-body --user '${CLIENT_ID}:${CLIENT_SECRET}' --request POST '${AUTHORIZATION_SERVER}/oauth2/token' --data-urlencode 'grant_type=refresh_token' --data-urlencode 'refresh_token=<refresh_token>'"
