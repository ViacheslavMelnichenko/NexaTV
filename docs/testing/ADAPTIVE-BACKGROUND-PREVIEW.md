# NexaTV Preview — adaptive backgrounds

This APK is a separate app (`com.nexatv.launcher.preview`, Android TV 14 or later).
It has its own settings and favorites. Open **NexaTV Preview** from your TV's apps menu.
It does not register as HOME and does not include the accessibility override service.
Your current launcher remains responsible for the HOME button.

## Try it

1. Install `NexaTV-Preview.apk` using your usual sideload method.
2. Open NexaTV Preview. The adaptive background starts enabled, with soft blur,
   25% dimming and motion off.
3. Pin apps from **All apps** using the long-press menu to populate your new favorites row.
4. Open **Settings → Background → Customize** (the Background row's Customize button).
5. Select **Manual**, choose Dawn / Day / Sunset / Night and Clear / Cloudy / Rain / Snow,
   then select **View on Home**. The weather widget continues to show real conditions.
6. Compare blur Off / Soft / Strong and dimming. Enable Gentle motion to try rain or snow.
7. Switch back to **Automatic** to follow Wrocław's conditions, or **Theme** for the original background.

The 16 scenes are bundled; no image downloads are needed. The landscape is illustrative.
Weather uses the existing fixed Wrocław location. Successful requests refresh every 30 minutes
while Home is visible, failures retry after 5 minutes. Last successful weather is retained for
up to 3 hours in this process; stale observations are also rejected. Without usable weather,
cloudy imagery and fallback 06:30 / 18:30 sunrise/sunset in Europe/Warsaw are used.
The clock and calendar continue using the device's timezone.

## Build / download

The **Preview APK** GitHub Actions workflow runs on pushes to `codex/**` branches. Its artifact
`NexaTV-Preview-<commit>` contains the APK, this guide and SHA256SUMS. It expires after 30 days.
The workflow builds, runs JVM tests, Android Lint and detekt, then checks the ABI and 30 MiB size limit.
The existing PR CI still verifies the ordinary debug build. Branch builds use no release signing key.

Published previews are also available under GitHub Releases as prereleases with annotated
`vX.Y.Z-preview.N` tags. Each tag points to the exact successful branch build commit; the published
APK is copied from that run, with its checksum and this guide. These assets are independent of the
30-day Actions artifact retention. Preview tags do not invoke the production signing job.

Local build with JDK 17 and Android SDK 34:

```sh
./gradlew :app:assemblePreview :app:lintPreview :app:testPreviewUnitTest detekt
```

Output: `app/build/outputs/apk/preview/app-preview.apk`.
The CI debug signing key is cached. If that cache is lost or a locally signed preview was installed,
Android may reject an update due to a different certificate. In that case remove **only NexaTV Preview**
and install again; its separate preview settings/favorites will reset.

## On-device acceptance (not yet measured)

- Compare original Theme, adaptive still imagery, transition and optional rain/snow on the TCL.
- D-pad focus and app launching must remain responsive during transitions and image loading.
- Verify all 16 scenes, both blur levels, all dimming options and manual/automatic switching.
- Leave Home for another app and turn the display off: no animation or weather polling should continue.
- Return after a long sleep/offline period: no permanently stale weather or daytime scene at night.
- Repeat Settings/Home and app/Home navigation 100 times; check no sustained memory growth.
- Record cold start, p50/p95 frame timing and idle/transition PSS against the same build in Theme mode.
- Check EN/UK labels at normal and 1.3× font scale. All background options must be reachable by D-pad.

Implementation bounds are not measured performance guarantees: one current image plus one incoming
image during a 4-second crossfade; decode off-main at at most 1920 px wide; blurred images processed
once at 480 px wide; 36 decorative particles at 20 updates/sec only while Home is resumed.
System-disabled animations also disable crossfades and particles. No video, WebView or live blur.
