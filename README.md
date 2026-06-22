# Hermes Dispatch

**An open-source, mobile-first Android app for your [Hermes](https://github.com/NousResearch/hermes-agent) agents.**
Fire off tasks by voice or text, watch your self-hosted agent work in real time, control its scheduled jobs, and get progress on your lock screen — all from your phone.

### 📲 Download
**[⬇️ Latest release APK](https://github.com/adebnar/hermes-dispatch-android/releases/latest)** — grab `hermes-dispatch-oss.apk` from the latest release and install it (you'll need to allow "install from unknown sources").

> Direct link to the newest build: <https://github.com/adebnar/hermes-dispatch-android/releases/latest/download/hermes-dispatch-oss.apk>

The `oss` build is Google-library-free (F-Droid-friendly) and uses [ntfy](https://ntfy.sh)/UnifiedPush for background notifications.

## Features
- 📋 **Tasks** — history of what your agent has done, with live status; open one for the full conversation, **rename** it, or **long-press a sent message to edit & resend**.
- ⏰ **Scheduled** — recurring (cron) jobs your agent classifies, with pause / resume / run-now / delete and **inline editing** of a job's name, prompt, and schedule.
- 🗣️ **Voice or text** task creation (on-device speech-to-text).
- 🔴 **Live execution** — stream the agent's text + tool-use as it happens, with mid-run approvals and clarifications.
- 📥 **Inbox** — cron jobs that "deliver to this desktop" show up as clean **result** cards (just the agent's output, rendered). **Swipe to archive**, **pin/delete** (app-only — the files on disk are never touched), unread dots, and a per-job **bell** plus a global "alert on failures" so only what you care about buzzes. Subscribe a job to alerts straight from the **Scheduled** tab too, and pick a **custom alert sound** in Settings.
- 🔔 **Lock-screen progress** with the app closed, via UnifiedPush/ntfy — no Google services required. Optional **end-to-end encryption** so the relay only sees ciphertext.
- 🐞 **Bug reporting** (opt-in) — capture the app's own logs into a **redacted** diagnostic report (secrets/keys/tokens stripped), review it, and share as a file.
- 👥 **Profiles** — switch between your Hermes profiles (e.g. work/personal); runs, tasks, and the Inbox scope to the selected one.
- 🤖 **Model picker**, **editable connection** (Bridge URL / token), and an optional **server-side transcription** toggle in Settings.
- 🔗 **Rich result cards** — Sheets/Docs/Drive links the agent returns render as tappable cards; tasks are grouped by recency.

---

## How it works

```
Android app ──REST + SSE──► hermes-dispatch-bridge ──REST + WS──► hermes-agent dashboard ──► MCP tools, cron, skills
     ▲ UnifiedPush (ntfy) ◄── push ──┘ (server-held runs, cron classify, fan-out)
```

The app talks to a small **self-hosted bridge** —
[**hermes-dispatch-bridge**](https://github.com/adebnar/hermes-dispatch-bridge) — that runs next to your
Hermes agent. The bridge fronts the hermes-agent dashboard, holds runs server-side
(so they keep going when your phone sleeps), classifies cron tasks, and pushes progress.
The app authenticates to the bridge with a single **bridge token**.

---

## Setup (full walkthrough)

You need three things running, then you install the app. Steps are split into
**🧑 what a human does** and **🤖 what you can ask your Hermes agent to build for you**.

### 1. Hermes agent + dashboard — 🧑
- Have **[Hermes](https://github.com/NousResearch/hermes-agent)** installed and its **dashboard** running
  (the gateway web UI, default **port 9119**). Confirm it loads at `http://127.0.0.1:9119`.
- Configure at least one model provider and the MCPs you want (Gmail, Sheets, web search…).

### 2. The bridge — 🧑 (one-time) + 🤖
Follow [hermes-dispatch-bridge → Setup](https://github.com/adebnar/hermes-dispatch-bridge#setup). In short:
```bash
git clone https://github.com/adebnar/hermes-dispatch-bridge && cd hermes-dispatch-bridge
uv venv --python 3.12 && uv pip install -e .
cp .env.example .env          # then edit .env (see below)
uv run uvicorn app.main:app --host 0.0.0.0 --port 8099
```
Your `.env` needs:
- `HERMES_URL=http://127.0.0.1:9119` — your dashboard.
- `HERMES_TOKEN=` — **leave empty**; for a local dashboard the bridge auto-reads the token.
- `BRIDGE_TOKEN=<a strong random secret>` — **this is what you'll type into the app.**
  Generate one: `python3 -c "import secrets;print(secrets.token_urlsafe(32))"`

- **🤖 ask Hermes:** *"Generate a strong BRIDGE_TOKEN, write a `.env` for hermes-dispatch-bridge with HERMES_URL=http://127.0.0.1:9119, and install a launchd/systemd service so the bridge runs on boot."*

### 3. Find your bridge URL + token — 🧑
The phone needs the bridge's **URL** and **token**.

**Recommended — HTTPS via Tailscale Serve** (valid cert, validated by the phone automatically):
```bash
# in the bridge repo:
./scripts/enable-https.sh        # or: tailscale serve --bg 8099
```
This gives a URL like `https://your-machine.your-tailnet.ts.net` — **use that** as the bridge URL.
(Requires "HTTPS Certificates" enabled for your tailnet in the Tailscale admin console.)

**Plain HTTP alternative** (no Serve) — the bridge listens on port **8099**:
```bash
tailscale ip -4          # encrypted transport → http://100.x.y.z:8099
ipconfig getifaddr en0   # macOS LAN          → http://192.168.x.y:8099
hostname -I              # Linux LAN (first address)
```

> The app accepts plain `http://` because you're on your own private network
> (Tailscale is WireGuard-encrypted). HTTPS via Tailscale Serve is preferred — it
> validates against the system trust store with no extra config.

**Get the bridge token** (the `BRIDGE_TOKEN` you set in step 2):
```bash
grep BRIDGE_TOKEN ~/path/to/hermes-dispatch-bridge/.env
```

### 4. Background notifications (optional but recommended) — 🧑
Install the **[ntfy](https://ntfy.sh) Android app** (it's the UnifiedPush distributor). That's it —
Hermes Dispatch auto-registers your device with the bridge, which then pushes run
progress to your lock screen. To self-host ntfy instead of the public server, see the bridge README.

### 5. Install & pair the app — 🧑
1. Install the [latest release APK](https://github.com/adebnar/hermes-dispatch-android/releases/latest).
2. Open it and on the pairing screen enter:
   - **Bridge URL** — e.g. `http://100.111.188.14:8099`
   - **Bridge token** — your `BRIDGE_TOKEN`
   - **Profile** (optional) — leave blank, or pick one later in Settings.
3. Tap **Connect**. You're in — create a task with the ＋ button or a suggestion.

---

## Build from source
```bash
./gradlew testOssDebugUnitTest    # JVM unit tests
./gradlew assembleOssDebug        # debug APK (oss, Google-free)
./gradlew assembleOssRelease      # release APK (needs keystore.properties — see below)
./gradlew assemblePlayDebug       # play flavor (adds runtime-configurable FCM)
```
For a signed release, create `keystore.properties` in the repo root (git-ignored):
```properties
storeFile=release.keystore
storePassword=…
keyAlias=…
keyPassword=…
```
and a matching keystore (`keytool -genkeypair -keystore release.keystore -alias … -keyalg RSA -keysize 2048 -validity 10000`). Without it, release builds are unsigned.

## Roadmap
| Phase | Scope | State |
|---|---|---|
| 1 | Pairing + read-only Tasks/Scheduled + cron control | ✅ |
| 2 | Live chat + SSE streaming (split chat/actions) + pinned artifacts | ✅ |
| 3 | Voice capture (on-device STT) + MCP/tools surfacing | ✅ |
| 4 | Background push (UnifiedPush/ntfy) + lock-screen "live update" | ✅ |
| 5 | Settings + profile switcher, suggested tasks, pull-to-refresh | ✅ |
| 6 | Model picker, mid-run approvals/clarify, image previews, branded theme | ✅ |
| 7 | In-app editing: rename tasks, edit & resend, edit schedules, edit connection | ✅ |
| 8 | Inbox (local cron deliverables) + alerts, rich result cards, task grouping, server-side STT, E2EE push, persistent push registry | ✅ |
| 9 | Inbox v2: result-only cards, swipe-archive / pin / delete (app-only), unread + failure alerts, opt-in redacted bug reports | ✅ |
| 10 | Cron-side alert toggle (Scheduled tab) + custom alert sound (per-channel) | ✅ |

Deferred (additive later): F-Droid / Play Store listings, making the repos public.

## Contributing & security
See [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and the API contract in [`docs/API-CONTRACT.md`](docs/API-CONTRACT.md).

## License
[Apache-2.0](LICENSE).
