# Security Policy

## Reporting a vulnerability
Please report security issues privately via GitHub's **"Report a vulnerability"**
(Security → Advisories) on this repository, or by email to the maintainer. Do not
open public issues for vulnerabilities. We aim to acknowledge within 72 hours.

## Security posture
Hermes Dispatch connects a phone to a **self-hosted** Hermes agent, so it handles
sensitive credentials and an authenticated channel to systems that can read mail,
files, and more. Practices in this codebase:

- **Credentials at rest** are stored in `EncryptedSharedPreferences` (AES-256,
  Android Keystore-backed). Bridge URL, session cookie, and push endpoint never
  hit plaintext storage. See `data/prefs/SecureSettings.kt`.
- **Cleartext is permitted by design** for self-hosted bridges on a private
  network (Tailscale is WireGuard-encrypted; LAN is trusted) — otherwise the app
  couldn't reach a bridge without a TLS reverse proxy. Confidentiality/integrity
  come from the private network. For public-internet exposure, front the bridge
  with HTTPS (Tailscale Serve / reverse proxy) and use an `https://` URL (system
  trust anchors). See `network_security_config.xml`.
- **No secrets in the repo.** `google-services.json`, keystores, and `*.env` are
  git-ignored. The `play` flavor configures Firebase **at runtime**, so no
  service file is committed.
- **Backups disabled** (`allowBackup=false`) and stores excluded from cloud /
  device-transfer (`data_extraction_rules.xml`).
- **Least privilege** — only the permissions the feature set needs.
- **Dependency hygiene** — pinned versions via the Gradle version catalog; CI
  runs lint on every PR.

## Scope
The app trusts the bridge the user pairs with. Securing the bridge and the
Hermes agent (auth, TLS, network exposure) is the operator's responsibility; see
`docs/` for hardened setup guidance.
