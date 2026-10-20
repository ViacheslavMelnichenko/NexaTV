# ADR-0010: Home weather widget & the first outbound network call

## Status

**Accepted** — 2026-10-20. **Amends NFR-021** (which previously forbade any `INTERNET` permission) and
narrows the "local only (no INTERNET)" aside in [ADR-0008](./ADR-0008-persistence-proto-datastore.md)
(ADR-0008's persistence decision is unchanged — storage stays local; only the blanket no-network stance
is amended). Opens the M5 "Cinematic Home" widget slot alongside the offline calendar widget.

## Context

The owner wants the centre of Home to carry two equal widgets: an offline **month calendar** (today
highlighted) and a **local weather** card for Wrocław — current temperature plus the day's min/max.

Weather is inherently live data, so it cannot be satisfied offline. That collides head-on with a
standing, documented constraint:

- **FACT:** `AndroidManifest.xml` recorded **NFR-021** — *"no INTERNET / ACCESS_NETWORK_STATE
  permissions"* — a deliberate offline-first / privacy posture.
- **FACT:** ADR-0008 reinforced it ("local only (no INTERNET), `allowBackup=false`").
- **FACT:** there is no HTTP client in the project (no OkHttp/Retrofit); `org.json` and
  `HttpURLConnection` ship with the Android SDK.
- **CONSTRAINT (Hard Rule 4):** no secrets in Git — any weather provider that needs an API key would
  require committing or provisioning a secret.

So the real question is not "how do we fetch weather" but **"do we open the first hole in the
offline-first stance, and if so how narrowly"** — an owner-level decision, taken explicitly
(2026-10-20) in favour of a single, minimal, keyless call.

## Decision

1. **Permit exactly one outbound capability: `INTERNET`.** Add `android.permission.INTERNET` only — **not**
   `ACCESS_NETWORK_STATE`, **not** `QUERY_ALL_PACKAGES`, **no** background/boot network. The manifest's
   NFR-021 comment is amended to state the single permitted use (weather) and that no analytics/telemetry
   or any other network traffic is allowed.
2. **Provider: Open-Meteo.** Free, **no API key, no account, no auth** — so no secret ever touches Git
   (Hard Rule 4). Non-commercial use is free; data is CC-BY-4.0. One endpoint:
   `https://api.open-meteo.com/v1/forecast?latitude=<lat>&longitude=<lon>&current=temperature_2m`
   `&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1`.
3. **No new dependency.** `WeatherRepository.fetch()` is a `suspend` function doing one
   `HttpURLConnection` GET on `Dispatchers.IO`, parsed with `org.json` — keeps the APK small (NFR-002) and
   avoids pulling OkHttp/Retrofit for a single request. The pure JSON→domain mapping is `internal` and
   unit-tested with sample payloads, no device/network needed.
4. **Fixed location, not device GPS.** Latitude/longitude are a compile-time constant for **Wrocław**
   (≈ 51.1079, 17.0385). No location permission, no GPS, no device coordinates leave the TV — only a
   coarse, hardcoded city. A user-chosen location is a future settings field (ADR-0009 seam), not v1.
5. **Off the critical path, fail-soft.** The widget fetches lazily after first composition
   (`LaunchedEffect`), never on the cold-start render path (NFR-001). Any failure (offline, timeout, bad
   payload) degrades to an "unavailable" state — Home never blocks, never crashes, and shows the grid and
   calendar regardless. No persistence/caching in v1 (a later optional refinement).

## Alternatives

| Decision point | Options (recommended **bold**) | Verdict |
|---|---|---|
| Open the network at all | **Add `INTERNET` for weather only** · keep NFR-021 absolute (no weather) · weather opt-in in Settings, default off | **Chosen:** owner explicitly wants live weather; scoped to one keyless call. Settings-gated opt-in was offered and is a clean future refinement, but v1 keeps it simple; absolute-offline would drop a requested feature |
| Provider | **Open-Meteo (no key)** · OpenWeatherMap / WeatherAPI (API key) · bundled static data | **Chosen:** keyless means no secret in Git (Hard Rule 4). Keyed services force a committed/provisioned secret; static data can't be live |
| HTTP stack | **`HttpURLConnection` + `org.json`** · OkHttp + Retrofit + Moshi | **Chosen:** one GET doesn't justify ~1–2 MB of libraries (NFR-002); the SDK primitives suffice and stay testable via the pure parser |
| Location source | **Fixed Wrocław constant** · device GPS (`ACCESS_*_LOCATION`) · IP geolocation | **Chosen:** no location permission, no device coordinates exfiltrated; a coarse city is all the widget needs. User-selectable city deferred to settings |
| Fetch timing | **Lazy `LaunchedEffect`, fail-soft** · block Home until loaded · poll on a timer | **Chosen:** keeps NFR-001 cold start untouched and Home resilient offline; polling/blocking would hurt startup and offline UX |

## Consequences

**Positive**
- The requested weather widget works; the calendar widget stays fully offline.
- No secret, no new dependency, no location permission; APK stays lean (NFR-002).
- Failures are invisible to the rest of Home — offline still shows apps + calendar.

**Negative / costs (honest)**
- **The offline-first absolute is now a scoped allowance.** NFR-021 changes from "no INTERNET, ever" to
  "INTERNET for weather only, no telemetry, no other traffic." That line must be guarded in review — any
  future network use needs its own decision, not a free ride on this permission.
- **Privacy trade-off:** when the widget loads, Open-Meteo sees the TV's public IP and the (fixed,
  non-personal) Wrocław coordinates. Mitigated by using a hardcoded city rather than device GPS and by
  sending no identifiers, but it is a third party the device now contacts.
- **No caching in v1:** every Home session that shows the widget makes one request; a brief cache is a
  later refinement if request volume ever matters.
- **Single provider dependency:** if Open-Meteo is unreachable the card shows "unavailable"; acceptable
  because it degrades rather than breaks.

## Rationale

The weather feature cannot be offline, so honouring the request means consciously opening the first hole
in NFR-021. The decision keeps that hole as small as it can be: one permission (`INTERNET`), one keyless
provider (no secret), one SDK-native GET (no dependency), a fixed city (no location permission or GPS
exfiltration), fetched off the startup path and failing soft so the rest of Home — including the offline
calendar — is never affected. Everything else about the offline-first posture (local storage, no backup,
no telemetry, no other network) stands. Widening beyond weather is explicitly out of scope and would
require a new ADR.

## Follow-ups

- **Permission + repository** (`feat`, this milestone): add `INTERNET`, amend the NFR-021 manifest
  comment; `WeatherRepository` (Open-Meteo, `HttpURLConnection` + `org.json`) with a unit-tested pure
  parser.
- **Widgets** (`feat`, this milestone): centre `CalendarWidget` (offline, today highlighted) +
  `WeatherWidget` (lazy fetch, fail-soft) as two equal-width cards above the favorites row.
- **Later (optional):** user-selectable location (ADR-0009 settings field), a short response cache, and
  the Settings opt-in toggle if the offline-by-default posture is wanted back.
