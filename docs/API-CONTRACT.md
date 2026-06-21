# Hermes bridge API contract (as consumed by Hermes Dispatch)

This is the HTTP + SSE surface the app depends on. It mirrors the
[hermes-webui](https://github.com/nesquena/hermes-webui) routes (`api/routes.py`,
`api/streaming.py`). **It is not a formally stable upstream API** — the app's
networking layer is deliberately tolerant (unknown fields ignored, unknown SSE
events mapped to `Unknown`). A future `hermes-dispatch-bridge` will pin a
versioned `/v1` shape.

## REST

| Capability | Endpoint | Notes |
|---|---|---|
| Login | `POST /api/auth/login` `{password}` | Sets an HttpOnly session cookie (stored by the client cookie jar). |
| Auth status | `GET /api/auth/status` | `{authenticated}` |
| List tasks | `GET /api/sessions` | → `SessionDto[]` |
| Start a run | `POST /api/chat/start` `{session_id?, message, model?, workspace?}` | → `{stream_id}` |
| Reply mid-run | `POST /api/chat/steer` | Phase 2 |
| Cancel | `POST /api/chat/cancel` `{stream_id}` | Phase 2 |
| Schedules | `GET /api/crons` | → `CronDto[]` |
| Schedule control | `POST /api/crons/{pause,resume,run,delete}` `{id}` | |
| MCP servers | `GET /api/mcp/servers` | → `McpServerDto[]` |
| Models | `GET /api/models` | Phase 5 |

Profile is sent as the `X-Hermes-Profile` request header (the bridge may also
honor a `hermes_profile` cookie).

## SSE — `GET /api/chat/stream?stream_id=…`

`Content-Type: text/event-stream`. Frames use `event:` + `data:` (JSON). The
client maps them to `StreamEvent` (see `data/remote/sse/`). Observed event
vocabulary (from `streaming.py`):

| Event name(s) | → StreamEvent | Drives |
|---|---|---|
| `token`, `text`, `delta`, `message` | `Token(text)` | chat pane |
| `tool`, `tool_use`, `tool_call` | `Tool(name, preview)` | actions pane |
| `status`, `worker_started`, `assistant_started` | `Status(text)` | status line |
| `reasoning`, `thinking` | `Reasoning(text)` | (collapsed) |
| `approval` | `Approval(command, description)` | approval prompt |
| `clarify` | `Clarify(question)` | inline question |
| `completed`, `done`, `finished` | `Completed` | finalize task |
| `interrupted`, `cancelled` | `Interrupted` | finalize task |
| `error` (+ subtypes: `rate_limit`, `quota_exhausted`, `auth_mismatch`, `model_not_found`, …) | `Error(message, type)` | error UI |
| *anything else* | `Unknown(event, data)` | ignored safely |

### Phase 0 action item
Run hermes-webui locally and capture **real** payloads for each event
(`curl -N` the stream) to confirm exact field names, then tighten the DTOs and
add fixture-based parser tests.
