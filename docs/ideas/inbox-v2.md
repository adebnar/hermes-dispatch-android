# Inbox v2 — result-first, gesture-triaged, selective alerts + bug reports

## Problem Statement
How might we turn the Inbox from a raw cron-file dump into a clean stream of task
*results* you can triage by gesture, alert on selectively, and attach to a safe,
redacted bug report?

## Recommended Direction
Show the agent's OUTPUT (not the cron header/prompt) as pretty, titled cards.
Add app-local per-item state (pin / archive / delete / read) keyed on the stable
item id — disk files are never touched. Alerts stay opt-in per job (the bell),
plus one global "alert on failed runs" with repeat-collapsing. Add an opt-in
Settings toggle that captures this app's own logs, redacts secrets (by known
value + pattern), shows you the result, and shares it as a file.

## Key Assumptions to Validate
- [ ] Result = text after final `---` (else `## Error` block) holds across hermes versions — verify against live samples; keep "Details" fallback.
- [ ] Output's first `# heading` makes a good title — sample real deliverables.
- [ ] `logcat -d --pid=<self>` returns useful logs on the target device (Android 13+).
- [ ] Regex + known-value redaction catches the secrets that actually appear in logs.

## MVP Scope
IN:
- Bridge: `/v1/inbox` & `/inbox/item` return `output` (result-only) + keep `content` (raw) for Details; global "alert on failures" flag in state + watcher dedupe.
- App: result-only cards with output-derived title + unread dot; swipe→Archive (Undo), long-press→Pin/Delete/Mark-read; pins float to top; Archive filter to restore; per-item state in Room (id, pinned, archived, deleted, read).
- Settings: "Alert on failed runs" switch; "Bug reporting" switch → capture self logcat → redact → preview → share-as-file (FileProvider).
OUT (later): bridge-side logs, one-shot task results in the same stream, rule-based alerts, true on-disk delete.

## Not Doing (and Why)
- On-disk delete — destroys hermes run history; app-only is reversible and safe.
- Bridge log collection — scoped to app logs; smaller attack surface, less to build.
- Auto-sending reports anywhere — share sheet only; user stays in control.
- Rule/keyword alerts — bell + failures covers 90%; rules add UI and explaining cost.
- Pin = alert coupling — pin is organization; the bell is notification. Keep them orthogonal.

## Resolved Decisions
- Strip a leading echoed title in the output if it duplicates the job name: **yes**.
- Archive view: **filter chip** inside Inbox (not a separate tab).
- Failure-dedupe: **per job until it next succeeds** (a failing cron alerts once, not every run).

## Open Questions
- (Resolved above.)
