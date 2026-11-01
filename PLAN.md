# PLAN — Clookie
Spec: SPEC.md (acceptance lists per stage). Design rules: design/DESIGN.md.

- [x] 1 Foundation + design: toolchain, signing, release.ps1, design system, logo, showcase (sample data)
- [x] 2 Alarm engine (Room v1, setAlarmClock, receivers, ringing FGS + lock-screen activity, ring state machine), editor sheets, classic mission via `WakeMission` registry, permission screen + Độ tin cậy, updater, 6 synthesized sounds, v0.2.0
- [x] 3 Cookie Code (k = 4, 40 mm) + QR/barcode (ZXing), pure-Kotlin detector + synthetic set, code studio, v0.3.0
- [x] 4 Math, Shake, Steps, Typing, Memory, Crumbs, chains, sleeper profiles, volume lock, return-to-ringing, awake check, v0.4.0
- [x] 5 Studio + saved looks, 4 clock styles (word clock), timer, stopwatch, wake log, bedtime, widget, JSON backup, v0.5.0
- [x] 6 Review, fixes (ringing screen, Cookie Code scanning, restore), font-scale 1.3 pass, v1.0.0 (targetSdk stays 36)

Key decisions: Fraunces display + Be Vietnam Pro UI; logo "bitten clock"; targetSdk 36 (37 in Stage 6);
engine decisions in SPEC §3 "Alarm engine"; Room schema v1 frozen, migrations only from now on (Stage 5 needed none).
Sounds: `python -E -P scripts\sounds.py` -> res/raw. Missions: add an object + one MissionRegistry line.

Stage 5: model/WordClock + model/Extras (wake stats, stopwatch, timer, bedtime); design/Look (studio overrides,
adjustAccent); data/Backup (schema 1); alarm/Bedtime (reminder + AlarmSideEffects); widget/ClookieWidget.
Decisions in SPEC §3 "Customization and extras (Stage 5)". Screenshots: design/screens/s5_*.
Stage 6: decisions in SPEC §5 "Stage 6 decisions". Screenshots at 1.3: `gradlew :app:recordRoborazziDebug "-PfontScale=1.3"`.
Done. Next step: owner device tests (locked + in-use alarm, Cookie Code scan, widget, timer killed, backup restore).
