# Clookie — Specification

Clookie (clock + cookie, rhymes with "cookie") is an Android alarm clock. It is distributed only as signed release APKs on GitHub Releases of https://github.com/harieedawwn/clookie and shared with friends in Vietnam. Visual quality is the top priority: it must look distinctive and beautiful enough that people pick it over their stock clock app.

## 1. Product brief (complete)

### General
- Android only, Kotlin + Jetpack Compose. Distributed only as signed release APKs on GitHub Releases of https://github.com/harieedawwn/clookie (no Play Store).
- UI in Vietnamese (default) and English, switchable in-app. No onboarding slides, no intro or "about this feature" text, no explanatory paragraphs: short labels, numbers and icons only.
- First launch opens one permission screen in the design system's style: a list of permission rows (icon, short name, status, one-tap grant), a "Cấp tất cả" button that asks them one after another, and "Xong". Required group: notifications, exact alarms, full-screen intent, ignore battery optimization, OEM autostart where the brand has it. Optional group: camera, physical activity, install updates, overlay for return-to-ringing. Required rows must be granted or explicitly skipped; the screen returns automatically from each system settings page and refreshes statuses. Later, any missing permission is asked again inline when a feature needs it, and the screen stays reachable from Độ tin cậy.
- 24-hour time by default; week starts Monday (T2 T3 T4 T5 T6 T7 CN).

### Alarms (Stage 2)
- Fields: time, repeat days, label, sound, volume, volume ramp-up, vibration pattern, snooze length and limit, wake missions (chain of up to 3), sleeper profile, skip next occurrence, enabled.
- Quick naps (+10 / +20 / +30 / +60 min). Home features the next alarm with a live countdown.
- Reliability: AlarmManager.setAlarmClock; reschedule after boot, time change, timezone change and app update; ringing through a foreground service with alarm audio attributes (rings in silent mode); full-screen ringing screen over the lock screen that turns the screen on; ring state survives process death and reboot until dismissed; fallback to a bundled sound when the chosen one is unavailable; missed-alarm notification.
- "Độ tin cậy" panel in Settings: status and one-tap fix for notifications, exact alarms, full-screen intent, battery optimization, and OEM autostart/background settings (Xiaomi/Redmi/POCO, OPPO/realme, vivo, Samsung are common in Vietnam; every OEM intent has a fallback). A small dot on Home when something critical is off.
- Sounds: 6 original alarm sounds synthesized by a script we write (no third-party audio), plus system alarm tones and the user's own audio file.
- In-app updater: checks the latest GitHub release at most once a day and from a button in Settings, shows a small inline banner, downloads the APK and opens the system installer.

### Wake missions (Stages 3-4)
Every mission has difficulty settings and a "Thử" preview in the alarm editor.
- Classic: slide to stop, snooze button.
- Cookie Code: a code hand-drawn on notebook paper, scanned with the camera (details below).
- QR / barcode: register any existing code (shampoo bottle, toothpaste, a printed QR) and scan it to stop. Fully offline, works without Google Play services.
- Math: arithmetic, 3 levels, problem count.
- Shake: shake count, sensitivity.
- Steps: walk N steps (step sensor; accelerometer fallback).
- Typing: retype a phrase exactly (the user's own or a built-in Vietnamese set).
- Memory: repeat a lit-tile sequence on a grid.
- Crumbs: Clookie's mini-game; crumbs pop up briefly at random spots, tap N before they vanish, misses cost progress.
- While the user interacts with a mission, volume drops to about 30%, back to full after 20 s idle.

### Cookie Code (Stage 3)
- A square marker of 5x5 cells: the outer ring filled solid (frame), the inner 3x3 holds the pattern. The app generates a random pattern with 3-6 filled cells, not rotationally symmetric, differing by at least 2 cells under every rotation from the user's other codes. Users may keep several codes (bathroom, kitchen); an alarm requires one specific code or any of them.
- One code cell = k x k small squares of the notebook grid; k chosen from the photo in ref/ so the marker is about 3-4 cm wide and filling it with a ballpoint pen takes under 3 minutes (decision in §3). Blue ballpoint ink (the most common pen in Vietnam) must work on the blue printed grid; black is recommended.
- Drawing guide: an animation on a grid that looks like the user's paper shows which cells to fill and animates a pen filling them in order (frame first, then pattern). Play/pause, step back/forward, a step counter like "2/3"; no paragraphs. Then "Quét thử" scans it once before saving. "Lưu ảnh" exports a PNG for users with a printer.
- Detection on the camera luminance frame, off the main thread, at modest resolution: find the dark square frame, correct perspective, decide each cell by dark-pixel fill ratio so thin printed grid lines and pen texture don't count, match in any rotation, accept only after 3 consecutive matching frames. Torch button and auto-torch in the dark. No per-frame allocations.
- In scan mode the ringing screen opens the camera at once, over the lock screen.

### Sleeper profiles (Stage 4)
One-tap presets in the alarm editor, everything editable afterwards.
- Ngủ nông: classic, 60 s volume ramp, snooze 9 min x 5.
- Ngủ vừa: math (medium, 3 problems) or Crumbs, 20 s ramp, snooze x 2.
- Ngủ say: Cookie Code then math, full volume at once, volume lock, snooze only after finishing a mission, awake check on.

### Heavy-sleeper safeguards (Stage 4)
- Volume lock: alarm volume cannot be lowered while ringing.
- Leaving the ringing screen brings it back and the sound continues (strongest method current Android allows; OS limits in §4).
- Awake check: N minutes after dismissal (default 4) a notification asks "Còn thức không?"; no tap within 90 s and the alarm rings again with the same mission.
- Never trap the user: every scan mission has "Mất mã?" leading to a deliberately long backup (e.g. 15 hard math problems); if the camera is unavailable the mission falls back to math.

### Customization and extras (Stage 5)
- Customization studio: pick a preset, tweak tokens (accent, clock font, corner style, hard shadow on/off, density, motion level), live preview, save own presets.
- Home clock styles: big digital, analog "cookie" (chips as hour marks), Vietnamese word clock ("bảy giờ kém mười lăm"), minimal.
- Timer (presets, rings through the alarm engine) and stopwatch (laps, survives backgrounding).
- Wake log: dismiss time, time to dismiss, snoozes, on-time streak, flat 14-day bar chart.
- Bedtime reminder from a sleep-goal setting.
- Home-screen widget (time + next alarm) in the active preset's colors.
- Backup/restore of alarms and settings to a JSON file.
- Reduced-motion setting; honor the system animation scale.

### Visual direction
- Concept: a clock that is a cookie. Warm, tactile, flat and confident, like modern bakery packaging meets Swiss editorial typography: matte solid colors, generous space, oversized numerals, crisp geometry, and one playful motif used with restraint: the bite (a circular notch cut from a shape) and chips (small irregular rounded dots).
- Forbidden: gradients of any kind, neon, glow, bloom, blur, glassmorphism, soft blurred elevation shadows, the stock Material 3 look (baseline purple, stock FAB, stock switches, list rows with a trailing switch, stock time picker), emoji as icons, default Material icons on main screens.
- Allowed depth: solid color layers, 1.5-2 dp strokes, optional hard offset shadow (solid, zero blur).
- Signature layout ideas: Home led by the next alarm in huge numerals with a countdown; alarms as tall "slabs" with oversized time and weekday dots, no stock rows; a bite-motif toggle; the Cookie Dial time picker: drag around a large ring, a haptic tick per minute and a stronger one every 5, tap the hour or minute digits to type on a keypad; non-stock bottom navigation; ringing screen in full-bleed solid color with giant time; dismissal plays a bite animation.
- Presets (at least 7): Bơ sữa (cream, cocoa ink, jam red), Socola (dark chocolate, cream, caramel), Matcha, Khoai môn (taro), Giấy ô li (paper white, ink blue, faint printed-grid background lines), Đêm (true black for OLED), Màu hệ thống (Material You on Android 12+, flattened to solid tones).
- Logo: creative and memorable, flat, readable at 48 px and in monochrome. Seeds to beat: a cookie with a bite forming the "C" and chips as clock hands at 10:10; a cookie clock face where the bite is negative space; a wordmark whose "oo" are a cookie and a clock.
- Motion: springs, 150-350 ms, shared transitions between slab and editor, nothing animating off-screen.
- Performance: full display refresh rate on weak phones; the clock recomposes at most once per second; per-frame drawing happens in the draw phase.

## 2. Toolchain and versions (checked 2026-10-31 against Google Maven, Maven Central, services.gradle.org, AGP release notes)
| Item | Version |
|---|---|
| JDK | Temurin 17.0.20.1 |
| Gradle (wrapper) | 9.8.2 |
| Android Gradle Plugin | 9.5.0 (built-in Kotlin; min Gradle 9.6.0, max API 37) |
| Kotlin / Compose compiler plugin | 2.4.21 |
| KSP | 2.3.13 |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 (build-tools 36.0.0) |
| Room / DataStore | 2.8.5 / 1.3.0 |
| core-ktx / activity-compose / appcompat / core-splashscreen / lifecycle | 1.19.1 / 1.13.0 / 1.7.1 / 1.2.0 / 2.10.0 |
| Screenshot tests | Roborazzi 1.77.0 + Robolectric 4.17 (JVM, works on Windows); sandbox SDK 34 because SDK 35+ sandboxes need Java 21 |
| SDK tools | cmdline-tools 23.0 (`android sdk install`, sdkmanager is deprecated) |
| Git / GitHub CLI | 2.56 / 2.103.3 |

targetSdk stays 36 until Stage 2 reviews Android 17 behavior changes for alarms and full-screen intents.

## 3. Decisions
- Display numerals: Fraunces (SOFT 100, opsz 144, wght 800), compared with Bricolage Grotesque and Unbounded (design/type-display-candidates.png). UI: Be Vietnam Pro. Both subset to Latin + Vietnamese by scripts/fonts.py; FontCoverageTest checks every glyph.
- Cookie Code size: the ref/ paper has 2 mm dotted squares grouped 4x4 by bold lines into 8 mm squares. **k = 4**: one code cell = one bold 8 mm square, marker 5x5 = 40 mm. The bold printed lines are the drawing guide ("fill whole big squares"). Ink area <= 22 cells x 64 mm² = 1408 mm²; ballpoint scribble fill at 12-20 mm²/s is about 70-120 s (< 3 min).
- Logo: concept A "bitten clock" (design/logo/). Mark geometry is shared by the launcher icon XML and `MarkGeometry` in code.
- Language switching uses AppCompat per-app locales (AppCompatActivity), so it works on Android 8-12 too.

### Alarm engine (Stage 2; checked 2026-10-31 against developer.android.com: alarms, FGS types and background-start pages, behavior changes 14-17, notification permission)
- **Exact alarms**: `USE_EXACT_ALARM` (API 33+, granted at install, not user-revocable; recommended for alarm-clock apps; the Play-only policy does not apply to our sideloaded APK) plus `SCHEDULE_EXACT_ALARM` with `maxSdkVersion=32` (API 31-32). Scheduling is `setAlarmClock` only (Doze-exempt, shows the system's next alarm). `canScheduleExactAlarms()` is still checked in Độ tin cậy; if ever false the scheduler falls back to `setAndAllowWhileIdle` rather than dropping the alarm.
- **Full-screen intent**: `USE_FULL_SCREEN_INTENT` declared. On 14+ it is limited to calling/alarm apps and the user can revoke it, so Độ tin cậy checks `NotificationManager.canUseFullScreenIntent()` and fixes via `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`. No change in 15-17.
- **Ringing service**: foreground type `systemExempted` on 34+ (allowed for apps holding an exact-alarm permission, no timeout, may start from BOOT_COMPLETED on 15+ unlike `mediaPlayback`); `mediaPlayback` on 29-33 and as a fallback if the system refuses `systemExempted`. It is started from the `setAlarmClock` broadcast (exact alarms are exempt from FGS background-start limits). If a start is ever refused, the full-screen notification is posted and the ringing screen starts the service once visible.
- **POST_NOTIFICATIONS** (13+): required; the FGS notification is not exempt and the full-screen intent rides on it. Asked first on the permission screen.
- **Background activity starts**: never started directly from the background; the ringing screen opens through the full-screen intent (the platform's sanctioned path for alarm clocks). Optional `SYSTEM_ALERT_WINDOW` is kept for Stage 4 return-to-ringing. 14+ PendingIntent BAL opt-ins are not needed because we do not send activity PendingIntents ourselves.
- **REQUEST_INSTALL_PACKAGES**: declared; the updater checks `PackageManager.canRequestPackageInstalls()` and otherwise opens `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`, then hands the APK to the system installer via FileProvider + `ACTION_VIEW`.
- **targetSdk stays 36.** Android 17 (target 37) requires while-in-use capability for background audio unless the app holds an exact-alarm permission and plays `USAGE_ALARM`, which we do; raising it is safe but deferred to Stage 6 so 17's other target-37 changes are reviewed in one pass. Predictive back (target 36) is fine: Compose `BackHandler` still intercepts.
- **Direct boot**: Room DB, DataStore and ring state live in device-protected storage; receivers, the service and the ringing activity are `directBootAware`, and `LOCKED_BOOT_COMPLETED` reschedules, so an alarm rings after a reboot even before the first unlock.
- **Late and missed**: every registered instant is stored (`pendingTriggerAt`). After boot, a clock/zone change or an app update, an alarm up to 15 min late rings at once; later ones post a missed-alarm notification. An unattended ring stops after 30 min and is reported missed.
- **Volume**: the alarm stream is set to the alarm's volume while ringing and restored afterwards; the ramp and the mission ducking (30% for 20 s after each interaction) scale the player volume.
- **Schema**: Room v1 is frozen as of Stage 2 (v0.1.0 never opened the database). From now on every change bumps the version with a Migration; no destructive fallback.

### Cookie Code and QR (Stage 3; checked 2026-11-01 against Google Maven and Maven Central)
- **Detector** (`model/CookieDetector`, pure Kotlin on the Y plane, no OpenCV): 5x5 box blur; paper white = local max over
  ~12-16 px; ink = darker than 0.7 of it; 3x3 opening drops printed lines; per blob: outer contour, corners, one-sided
  line fit per side, homography; paper and ink levels as planes (outer ring / frame cells); cell = share of central
  samples darker than the midpoint (frame >= 0.5 each, pattern cell >= 0.4); match in 4 rotations, never mirrored.
  Accepted after 3 consecutive matching frames (`ScanGate`). Pattern codes are >= 2 cells apart, so one misread cell
  can only give "no code", never another code.
- **Synthetic set** (`SyntheticCamera`, generated at test time from ref/notebook-paper.jpg, never committed): ink on the
  photo's real 8 mm squares (hatching 70-95 %, overflow, outlines), pinhole camera (f 500 px, tilt 0-45°, any roll,
  80-320 px), blur, gradient, vignette, lamp shadows (penumbra 0.04-0.2 rad x f), dim + noise, torch hot spot.
  200 positive scenes (blue/black) + 100 negatives (paper, scribbles, handwriting, other patterns, other codes), 3 frames
  each. Result: 96.5 % detected within 3 frames (94.7 % per frame), 0 false matches, ~3.5-5.6 ms per 640x480 frame on
  the dev PC (JVM), 0 bytes allocated per frame. Not modeled: hard sunlight shadows.
- **Camera**: CameraX 1.6.2, preview via `CameraXViewfinder`, analysis 640x480 YUV with STRATEGY_KEEP_ONLY_LATEST (late
  frames are dropped) on one background thread; no frames for 6 s while visible = camera error -> math.
- **QR / barcode**: ZXing core 3.5.4 (pure Java, offline, no Play services, JVM-testable; ML Kit's bundled model is
  larger and native). Formats: QR, EAN-13/8, UPC-A (stored as EAN-13)/UPC-E, Code 128. Every other frame is read turned
  90° so barcodes work in both phone orientations; a code must be read twice within 4 frames.
- **Fallbacks**: "Mất mã?" = 15 level-3 problems; no camera permission, camera error or no saved code = 5 problems.

### Missions and safeguards (Stage 4)
- **Tunables**: every count, size, speed and threshold lives in `model/MissionTuning`. Games draw on a Canvas in the
  draw phase from a frame-time clock (`withFrameMillis`); screenshots use `LocalMissionDemo` (fixed mid-game frame).
- **Shake**: linear acceleration (gravity by 500 ms low-pass) peaks over 2.6 / 2.0 / 1.5 g (sensitivity low/mid/high),
  re-armed below half; a lone peak never counts (bursts only, 600 ms). **Steps**: TYPE_STEP_DETECTOR with
  ACTIVITY_RECOGNITION, else |a| peaks (+0.12 g after -0.05 g), 0.28-1.6 s apart, each within 35 % of the last,
  counted after 5 in a row, reset above +1.3 g. JVM traces: bed handling (flip, drop, bumps) = 0 shakes, 0 steps.
- **Typing**: NFC + trim, exact (case-sensitive); "Sai chữ" only when base letters diverge, so Telex/VNI words in
  progress are not flagged. Own phrase is stored URL-encoded in the mission codec (`Typing{...}~text`), no migration.
- **Unfair-input guards**: Memory shows a new sequence after a wrong tap; Crumbs: a vanished crumb or a tap on empty
  plate costs 1; Math keeps the problem, clears the answer. No sensor = 5 level-3 problems.
- **Chain**: pure `ChainProgress` (step, solved, choosing). Snooze after mission: snooze only once a mission is solved;
  a solved chain then offers slide-to-stop or snooze. Progress is kept per ring (alarm id + trigger) in the process.
- **Volume lock**: the service restores STREAM_ALARM every 100 ms if lowered; the ringing screen eats volume-down/mute.
- **Return to ringing**: the screen reports visibility; every 2 s while hidden the service relaunches it (overlay
  permission + screen on) or re-posts a non-silent full-screen alert every 10 s (ID 2).
- **Awake check**: pure `AwakeState` in device storage; `setExactAndAllowWhileIdle` for ask (N min) and deadline
  (90 s); unanswered = `RingingService.recheck` rings the same alarm; snooze or a new ring cancels; re-registered on
  boot; over 30 min overdue (phone off) it is dropped.

### Customization and extras (Stage 5)
- **Look** (`design/Look`): a built-in preset plus overrides (accent, clock font Fraunces / Be Vietnam Pro, corners,
  hard shadow, density). A custom accent is pushed away from the background's lightness until accent >= 3:1 on
  background and surface and its text >= 4.5:1 (`adjustAccent`); the studio shows picked -> adjusted. Saved presets
  and the look live in DataStore as short text codes. Motion (full / reduced) is one global setting, also in the studio.
- **Clock styles**: Số lớn keeps the next-alarm hero; Bánh quy (cookie face, chips as hour marks, hands drawn in the
  draw phase), Bằng chữ (`model/WordClock`, 24-hour words, "phút" after single-digit minutes, :45-:59 "kém" against the
  next hour) and Tối giản show the current time with the next alarm below.
- **Timer**: one `AlarmKind.Timer` row with `oneShotAt` = end, so it rings through the engine (slide to stop, no
  snooze) even when the app is killed; pause deletes the row, resume adds a new one; timers are hidden from Home and
  not logged. **Stopwatch**: `elapsedRealtime` start + accumulated + lap totals in DataStore (survives backgrounding and
  process death; after a reboot the running part restarts from the stored total).
- **Wake log**: regular alarms only; time to dismiss counts from the ring's start. On time = stopped without snooze
  within 5 min; streak = consecutive days with alarms whose first ring was on time (days without alarms are skipped).
- **Bedtime**: one exact reminder at next alarm minus sleep goal (6-9 h), re-planned on every alarm or setting change.
- **Widget**: RemoteViews with TextClock (time, date ticked by the launcher, `updatePeriodMillis` 0), next alarm,
  preset colors, corner style and hard shadow; repainted only when alarms or the look change.
- **Backup**: JSON, `schema` 1 (`BackupCodec`): regular alarms, Cookie/QR codes (ids kept for missions), look, saved
  presets, clock style, motion, bedtime, default sound, language. Newer schemas are refused; restore replaces all.

## 4. OS limits to respect (verify on device in Stage 4)
- No app can block Home, Recents or power-off. "Bring back the ringing screen" = foreground service keeps ringing + re-posted full-screen intent; with the optional overlay permission the service relaunches the activity from the background. Without it, Android 10+ background-activity-launch rules only allow the notification.
- Android 14+: USE_FULL_SCREEN_INTENT can be revoked by the user; check `canUseFullScreenIntent()` and fix from Độ tin cậy.
- Volume lock = observe STREAM_ALARM changes while ringing and restore; some OEM ROMs mute alarms via their own zen modes.
- OEM autostart/battery pages differ per ROM version; every deep link needs a fallback to app details settings.

## 5. Acceptance per stage
### Stage 2 — alarm engine, list, editor, classic, Độ tin cậy, updater, mission contract
- [x] Room stores all alarm fields; Home/editor read and write it; sample data removed.
- [x] setAlarmClock scheduling; reschedule on BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, MY_PACKAGE_REPLACED (unit tests for next-trigger math incl. DST, skip-next, one-time).
- [x] Foreground ringing service, alarm audio attributes, rings in silent mode, volume ramp, vibration patterns, bundled-sound fallback.
- [x] Full-screen ringing activity over lock screen, turns screen on; ring state survives process death and reboot; missed-alarm notification.
- [x] Classic mission (slide + snooze) via a `WakeMission` plug-in contract (difficulty settings, "Thử" preview, volume ducking to 30% while interacting, back after 20 s idle).
- [x] Quick naps; editor sheets for label, sound (6 synthesized + system tones + own file), volume/ramp, vibration, snooze, missions.
- [x] First-launch permission screen + Độ tin cậy panel with OEM intents and fallbacks; Home dot when critical items are off.
- [x] Updater: daily + manual check of latest GitHub release, inline banner, download, system installer.
- [x] Sound synthesis script committed; release v0.2.0.
### Stage 3 — Cookie Code + QR/barcode
- [x] Code generator rules (3-6 cells, asymmetric, >= 2 cells apart under rotation) with unit tests; multiple named codes.
- [x] Drawing guide animation (frame then pattern, play/pause, step, "2/3"), "Quét thử", "Lưu ảnh" PNG.
- [x] Detector on luminance frames off main thread, perspective correction, fill-ratio cells, any rotation, 3 consecutive matches, no per-frame allocations; torch + auto-torch; JVM tests on synthetic samples (blue and black ink). Photographed samples: only the blank ref/ page so far; photos of real drawn codes come from the Stage 3 device test.
- [x] QR/barcode register + scan, offline, no Play services; "Mất mã?" backup; camera-unavailable fallback to math.
### Stage 4 — other missions, profiles, safeguards
- [x] Math, Shake, Steps, Typing, Memory, Crumbs with difficulty + "Thử"; chains of up to 3; snooze after mission.
- [x] Sleeper profiles as one-tap presets; volume lock; return-to-ringing; awake check (4 min, 90 s); release v0.4.0.
- [ ] OS limits in §4 confirmed on device (owner's Stage 4 device test).
### Stage 5 — customization and extras
- [x] Studio with token tweaks, live preview, saved presets; 4 home clock styles incl. Vietnamese word clock.
- [x] Timer + stopwatch, wake log with 14-day chart, bedtime reminder, widget in preset colors, JSON backup/restore, reduced-motion setting; release v0.5.0.
- [ ] Widget, timer in the background and bedtime reminder confirmed on device (owner's Stage 5 device test).
### Stage 6 — review and v1.0.0
- [x] Full review against this spec and the forbidden list; fixes; performance pass (no dropped frames on a weak phone, recomposition counts); v1.0.0 release.

### Stage 6 decisions (2026-11-01)
- Ringing notification no longer uses `setSilent` (its "silent" group made Android 13+ block the full-screen intent:
  the alarm rang with no screen); `setOnlyAlertOnce` instead. The ringing screen is started at once when the phone
  is in use. Overlay ("Hiện trên ứng dụng khác") is now **required**, plus a Xiaomi-only "Hiện trên màn hình khoá"
  row (MIUI's own lock-screen/background pop-up toggles; counts once opened).
- Cookie Code made easier: scan at 2x zoom with centre refocus every 3 s (hold 12-15 cm, sharp focus); ScanGate
  accepts 3 matching frames with up to 2 blank frames between; frame cells need 35 % ink, pattern cells 33 %; outline
  check tolerates wobbly edges; new codes use as few cells as possible (3). Synthetic set (all pattern sizes): 97 %
  detected within 3 frames, 0 false matches. Loose 55 %-coverage hatching measured 93.5 % (not a test target).
- Restore runs in one Room transaction; ringing screen recomposes once a minute; labels wrap instead of "…".
