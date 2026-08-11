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
TOKEN_FILE="${TOKEN_FILE:-.dezhban-tokens}"

TOKEN_RESPONSE="$(
    curl --fail-with-body --silent --show-error \
        --user "${CLIENT_ID}:${CLIENT_SECRET}" \
        --request POST "${AUTHORIZATION_SERVER}/oauth2/token" \
        --header "Content-Type: application/x-www-form-urlencoded" \
        --data-urlencode "grant_type=authorization_code" \
        --data-urlencode "code=${AUTHORIZATION_CODE}" \
        --data-urlencode "redirect_uri=${REDIRECT_URI}"
)"

umask 077
TEMPORARY_TOKEN_FILE="$(mktemp "${TOKEN_FILE}.XXXXXX")"
trap 'rm -f "${TEMPORARY_TOKEN_FILE}"' EXIT

printf '%s' "${TOKEN_RESPONSE}" |
    python3 -c '
import json
import shlex
import sys

response = json.load(sys.stdin)
for environment_name, response_name in (
    ("ACCESS_TOKEN", "access_token"),
    ("REFRESH_TOKEN", "refresh_token"),
):
    value = response.get(response_name)
    if not value:
        raise SystemExit(f"Token response does not contain {response_name}")
    print(f"{environment_name}={shlex.quote(value)}")
' > "${TEMPORARY_TOKEN_FILE}"

mv "${TEMPORARY_TOKEN_FILE}" "${TOKEN_FILE}"
trap - EXIT

printf 'Access and refresh tokens stored in %s with owner-only permissions.\n' "${TOKEN_FILE}"
printf 'Load them into the current shell with: source %q\n' "${TOKEN_FILE}"
printf '\nCall UserInfo after loading the file:\n'
printf '%s\n' \
    "curl --fail-with-body --header 'Authorization: Bearer \${ACCESS_TOKEN}' '${AUTHORIZATION_SERVER}/userinfo'"
printf '\nRefresh the tokens with:\n'
printf '%s\n' \
    "curl --fail-with-body --user '${CLIENT_ID}:${CLIENT_SECRET}' --request POST '${AUTHORIZATION_SERVER}/oauth2/token' --data-urlencode 'grant_type=refresh_token' --data-urlencode 'refresh_token=\${REFRESH_TOKEN}'"
