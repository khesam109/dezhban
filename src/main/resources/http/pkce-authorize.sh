#!/usr/bin/env bash
set -euo pipefail

AUTHORIZATION_SERVER="${AUTHORIZATION_SERVER:-http://localhost:8585}"
CLIENT_ID="${CLIENT_ID:-client}"
REDIRECT_URI="${REDIRECT_URI:-http://127.0.0.1:8585/callback.html}"
SCOPE="${SCOPE:-openid}"
PKCE_FILE="${PKCE_FILE:-.dezhban-pkce}"

umask 077
TEMPORARY_PKCE_FILE="$(mktemp "${PKCE_FILE}.XXXXXX")"
trap 'rm -f "${TEMPORARY_PKCE_FILE}"' EXIT

python3 -c '
import base64
import hashlib
import secrets
import shlex
import sys
import urllib.parse

authorization_server, client_id, redirect_uri, scope = sys.argv[1:]

def base64url(value):
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")

verifier = base64url(secrets.token_bytes(32))
challenge = base64url(hashlib.sha256(verifier.encode("ascii")).digest())
state = base64url(secrets.token_bytes(32))
query = urllib.parse.urlencode({
    "response_type": "code",
    "client_id": client_id,
    "redirect_uri": redirect_uri,
    "scope": scope,
    "state": state,
    "code_challenge": challenge,
    "code_challenge_method": "S256",
})
authorization_url = f"{authorization_server}/oauth2/authorize?{query}"

for name, value in (
    ("CODE_VERIFIER", verifier),
    ("CODE_CHALLENGE", challenge),
    ("AUTHORIZATION_STATE", state),
    ("AUTHORIZATION_URL", authorization_url),
):
    print(f"{name}={shlex.quote(value)}")
' "${AUTHORIZATION_SERVER}" "${CLIENT_ID}" "${REDIRECT_URI}" "${SCOPE}" \
    > "${TEMPORARY_PKCE_FILE}"

mv "${TEMPORARY_PKCE_FILE}" "${PKCE_FILE}"
trap - EXIT

printf 'PKCE verifier and authorization URL stored in %s with owner-only permissions.\n' "${PKCE_FILE}"
printf 'Run: source %q\n' "${PKCE_FILE}"
printf 'Then open: %s\n' "$(source "${PKCE_FILE}"; printf '%s' "${AUTHORIZATION_URL}")"
