# Hermes Dispatch

**An open-source, mobile-first Android app for your [Hermes](https://github.com/NousResearch/hermes-agent) agents.**
Fire off tasks by voice or text, watch your self-hosted agent work in real time, control its scheduled jobs, and get progress on your lock screen — all from your phone.

> Status: **early development.** This repo currently contains the Phase 1 vertical slice (pairing → live task & schedule lists, backed by your Hermes bridge) plus the full app architecture for later phases. See [the roadmap](#roadmap).

## Features

- 📋 **Tasks** — history of what your agent has done, with live status.
- ⏰ **Scheduled** — recurring (cron) jobs your agent classifies, with pause / resume / run-now / delete.
- 🗣️ **Voice or text** task creation *(Phase 3)*.
- 🔴 **Live execution** — stream the agent's tool-use as it happens *(Phase 2)*.
- 🔔 **Lock-screen progress** with the app closed, via **UnifiedPush/ntfy** — no Google services required *(Phase 4)*.
- 👥 **Profiles** — switch between multiple Hermes profiles.

## Architecture

```
Android app (Kotlin/Compose)  ──REST+SSE──►  Hermes bridge  ──►  Hermes Agent (your server)
        ▲ UnifiedPush (ntfy)  ◄──push──────  (hermes-webui or sidecar)        └► MCP tools, cron, skills
```

The app talks to a small **self-hosted bridge** — [hermes-dispatch-bridge](https://github.com/adebnar/hermes-dispatch-bridge) — that fronts your Hermes agent (via [hermes-webui](https://github.com/nesquena/hermes-webui)), holds runs server-side, classifies cron tasks, and pushes progress. The app authenticates to it with a single bridge token. See [`docs/API-CONTRACT.md`](docs/API-CONTRACT.md).

---

## Setup

Setup is split into **🧑 what a human does** (credentials, decisions, networking — things an agent shouldn't do for you) and **🤖 what you can ask your Hermes agent to build** (boilerplate the agent can scaffold). Copy the 🤖 prompts straight into your Hermes chat.

### 0. Prerequisites — 🧑 human
- A running **Hermes Agent** on an always-on machine, with a model provider and the MCPs you want (Gmail, Sheets, web search…).
- A way to reach it from your phone: **[Tailscale](https://tailscale.com)** (easiest), or a reverse proxy with HTTPS.
- To build from source: Android Studio (Ladybug+) / JDK 17.

### 1. The bridge
- **🧑 human:** run [hermes-dispatch-bridge](https://github.com/adebnar/hermes-dispatch-bridge) next to Hermes (it fronts hermes-webui), set `HERMES_WEBUI_*` + a strong `BRIDGE_TOKEN`, expose it over HTTPS, and note the URL + token. See that repo's README.
- **🤖 ask Hermes:** (see the bridge repo's setup prompts — it can scaffold the compose, generate the token, and wire ntfy.)

### 2. Push (UnifiedPush via ntfy) — the dead-simple default
- **🧑 human:** install the **[ntfy](https://ntfy.sh) Android app** (the UnifiedPush distributor); pick a hard-to-guess topic; use public `ntfy.sh` or self-host. Paste the topic into Hermes Dispatch's pairing screen.
- **🤖 ask Hermes:**
  > "Add an ntfy service to my docker-compose with a persistent volume and a random topic; print the topic and publish URL."
  > "Write a small `push_shim.py` that subscribes to hermes-webui's chat stream for active runs and POSTs concise progress lines ('Using MCP: gmail', 'Complete') to my ntfy topic; make the topic and base URL env-configurable."

> **Advanced (optional):** the `play` build flavor adds *bring-your-own Firebase*, configured at runtime in **Admin → Notifications** (no `google-services.json`, no rebuild). The default `oss` flavor stays Google-library-free. Most people never need this.

### 3. Install the app — 🧑 human
Build the `oss` flavor (`./gradlew assembleOssDebug`) or install a release. First run: enter your **bridge URL + token** → pick your profile → (optionally) paste your ntfy topic.

---

## Build & test

```bash
./gradlew testOssDebugUnitTest   # JVM unit tests (e.g. the SSE parser)
./gradlew assembleOssDebug       # default, Google-free APK
./gradlew assemblePlayDebug      # adds runtime-configurable FCM
```

## Roadmap

| Phase | Scope | State |
|---|---|---|
| 1 | Pairing + read-only Tasks/Scheduled + cron control | ✅ |
| 2 | Live chat + SSE streaming (split chat/actions) + pinned artifacts | ✅ |
| 3 | Voice capture (on-device STT) + MCP/tools surfacing | ✅ |
| 4 | Background push (UnifiedPush/ntfy) + lock-screen "live update" + deep-links | ✅ |
| 5 | Settings + profile switcher, suggested tasks, pull-to-refresh, error/retry | ✅ |

Deferred (already in the contract, additive later): categories grouping, artifact previews, file workspace, mid-run approvals UI, model picker, registering the push endpoint with the bridge, E2EE webpush.

## Contributing & security
See [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## License
[Apache-2.0](LICENSE).
