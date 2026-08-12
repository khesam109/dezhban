# Authorization code flow with PKCE

The seeded client requires PKCE using `S256`. A verifier must be a high-entropy
RFC 7636 unreserved string between 43 and 128 characters. Never send or persist
the verifier at the authorization endpoint; only its SHA-256 challenge is sent.

## Actors, roles, and scopes

Dezhban is an MSSP IdP. Client actor types and their default business scopes:

| Client type | Role | Typical capabilities |
| --- | --- | --- |
| `AP` | `AP` | `sign_request:create`, `sign_request:read` |
| `RO` | `RO` | `kyc:perform`, `certificate_order:create`, `certificate_order:read` |
| `MOBILE_FIRST_PARTY` | `MOBILE_FIRST_PARTY` | register, issue certificate, complete orders, read/sign requests |
| `MOBILE_THIRD_PARTY` | `MOBILE_THIRD_PARTY` | `sign_request:read` only |
| `ADMIN_PANEL` | `ADMIN_PANEL` | `system:admin` |

End users receive `END_USER` (and `ADMIN` when flagged). Access tokens include:

- `client_type`, `client_roles`
- `actor_type` (`END_USER` or the client actor for client credentials)
- `roles` / `user_roles`
- `permissions` — authorized business scopes intersected with client allow-list and role permissions

Manage catalogs and assignments through `/api/v1/admin/authorization/**`.

## Run the local example

Start the server, then generate a verifier, challenge, state, and authorization
URL:

```bash
bash src/main/resources/http/pkce-authorize.sh
source .dezhban-pkce
printf '%s\n' "$AUTHORIZATION_URL"
```

Open that URL and sign in with `user` / `salam`. Confirm that the returned
`state` equals `$AUTHORIZATION_STATE`, copy the authorization code, and exchange
it:

```bash
source .dezhban-pkce
AUTHORIZATION_CODE="<returned-code>" \
  bash src/main/resources/http/authorization-code-flow.sh
source .dezhban-tokens
```

The example client is confidential, so the token request uses both
`client_secret_basic` and PKCE. A native or browser public client uses PKCE
without a client secret.

## Bash with OpenSSL

```bash
base64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

CODE_VERIFIER="$(openssl rand 32 | base64url)"
CODE_CHALLENGE="$(
  printf '%s' "$CODE_VERIFIER" |
    openssl dgst -sha256 -binary |
    base64url
)"
```

## Python

```python
import base64
import hashlib
import secrets

def base64url(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")

verifier = base64url(secrets.token_bytes(32))
challenge = base64url(hashlib.sha256(verifier.encode("ascii")).digest())
```

## Java 21

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

var random = new SecureRandom();
var bytes = new byte[32];
random.nextBytes(bytes);

var encoder = Base64.getUrlEncoder().withoutPadding();
var verifier = encoder.encodeToString(bytes);
var challenge = encoder.encodeToString(
        MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII))
);
```

## JavaScript (browser)

```javascript
const base64url = bytes =>
  btoa(String.fromCharCode(...bytes))
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replaceAll("=", "");

const random = crypto.getRandomValues(new Uint8Array(32));
const verifier = base64url(random);
const digest = await crypto.subtle.digest(
  "SHA-256",
  new TextEncoder().encode(verifier),
);
const challenge = base64url(new Uint8Array(digest));
```

## Go

```go
package main

import (
    "crypto/rand"
    "crypto/sha256"
    "encoding/base64"
)

func pkce() (string, string, error) {
    random := make([]byte, 32)
    if _, err := rand.Read(random); err != nil {
        return "", "", err
    }
    verifier := base64.RawURLEncoding.EncodeToString(random)
    digest := sha256.Sum256([]byte(verifier))
    challenge := base64.RawURLEncoding.EncodeToString(digest[:])
    return verifier, challenge, nil
}
```

## C# / .NET

```csharp
using System.Security.Cryptography;
using System.Text;

static string Base64Url(byte[] value) =>
    Convert.ToBase64String(value)
        .TrimEnd('=')
        .Replace('+', '-')
        .Replace('/', '_');

string verifier = Base64Url(RandomNumberGenerator.GetBytes(32));
string challenge = Base64Url(
    SHA256.HashData(Encoding.ASCII.GetBytes(verifier))
);
```

## Protocol parameters

Authorization request:

```text
code_challenge=<challenge>
code_challenge_method=S256
```

Token request:

```text
code_verifier=<original-verifier>
```

Do not use the `plain` challenge method. Keep each verifier private and use it
for one authorization attempt only.
